/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.Advisor;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInfo;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Base class for AOP proxy configuration managers.
 *
 * <p>These are not themselves AOP proxies, but subclasses of this class are
 * normally factories from which AOP proxy instances are obtained directly.
 *
 * <p>This class frees subclasses of the housekeeping of Advices
 * and Advisors, but doesn't actually implement proxy creation
 * methods, which are provided by subclasses.
 *
 * <p>This class is serializable; subclasses need not be.
 *
 * <p>This class is used to hold snapshots of proxies.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see org.springframework.aop.framework.AopProxy
 */
public class AdvisedSupport extends ProxyConfig implements Advised {

	/** use serialVersionUID from Spring 2.0 for interoperability. */
	private static final long serialVersionUID = 2651364800145442165L;


	/**
	 * Canonical TargetSource when there's no target, and behavior is
	 * supplied by the advisors.
	 */
	public static final TargetSource EMPTY_TARGET_SOURCE = EmptyTargetSource.INSTANCE;


	/** Package-protected to allow direct access for efficiency. */
	@SuppressWarnings("serial")
	TargetSource targetSource = EMPTY_TARGET_SOURCE;

	/** Whether the Advisors are already filtered for the specific target class. */
	private boolean preFiltered = false;

	/** The AdvisorChainFactory to use. */
	@SuppressWarnings("serial")
	private AdvisorChainFactory advisorChainFactory = DefaultAdvisorChainFactory.INSTANCE;

	/**
	 * Interfaces to be implemented by the proxy. Held in List to keep the order
	 * of registration, to create JDK proxy with specified order of interfaces.
	 */
	@SuppressWarnings("serial")
	private List<Class<?>> interfaces = new ArrayList<>();

	/**
	 * List of Advisors. If an Advice is added, it will be wrapped
	 * in an Advisor before being added to this List.
	 */
	@SuppressWarnings("serial")
	private List<Advisor> advisors = new ArrayList<>();

	/**
	 * List of minimal {@link AdvisorKeyEntry} instances,
	 * to be assigned to the {@link #advisors} field on reduction.
	 * @since 6.0.10
	 * @see #reduceToAdvisorKey
	 */
	@SuppressWarnings("serial")
	private List<Advisor> advisorKey = this.advisors;

	/** Cache with Method as key and advisor chain List as value. */
	private transient @Nullable Map<MethodCacheKey, List<Object>> methodCache;

	/** Cache with shared interceptors which are not method-specific. */
	private transient volatile @Nullable List<Object> cachedInterceptors;

	/**
	 * Optional field for {@link AopProxy} implementations to store metadata in.
	 * Used by {@link JdkDynamicAopProxy}.
	 * @since 6.1.3
	 * @see JdkDynamicAopProxy#JdkDynamicAopProxy(AdvisedSupport)
	 */
	transient volatile @Nullable Object proxyMetadataCache;


	/**
	 * No-arg constructor for use as a JavaBean.
	 */
	public AdvisedSupport() {
	}

	/**
	 * Create an {@code AdvisedSupport} instance with the given parameters.
	 * @param interfaces the proxied interfaces
	 */
	public AdvisedSupport(Class<?>... interfaces) {
		setInterfaces(interfaces);
	}


	/**
	 * Set the given object as target.
	 * <p>Will create a SingletonTargetSource for the object.
	 * @see #setTargetSource
	 * @see org.springframework.aop.target.SingletonTargetSource
	 */
	public void setTarget(Object target) {
		setTargetSource(new SingletonTargetSource(target));
	}

	@Override
	public void setTargetSource(@Nullable TargetSource targetSource) {
		this.targetSource = (targetSource != null ? targetSource : EMPTY_TARGET_SOURCE);
	}

	@Override
	public TargetSource getTargetSource() {
		return this.targetSource;
	}

	/**
	 * Set a target class to be proxied, indicating that the proxy
	 * should be castable to the given class.
	 * <p>Internally, an {@link org.springframework.aop.target.EmptyTargetSource}
	 * for the given target class will be used. The kind of proxy needed
	 * will be determined on actual creation of the proxy.
	 * <p>This is a replacement for setting a "targetSource" or "target",
	 * for the case where we want a proxy based on a target class
	 * (which can be an interface or a concrete class) without having
	 * a fully capable TargetSource available.
	 * @see #setTargetSource
	 * @see #setTarget
	 */
	public void setTargetClass(@Nullable Class<?> targetClass) {
		this.targetSource = EmptyTargetSource.forClass(targetClass);
	}

	@Override
	public @Nullable Class<?> getTargetClass() {
		return this.targetSource.getTargetClass();
	}

	@Override
	public void setPreFiltered(boolean preFiltered) {
		this.preFiltered = preFiltered;
	}

	@Override
	public boolean isPreFiltered() {
		return this.preFiltered;
	}

	/**
	 * Set the advisor chain factory to use.
	 * <p>Default is a {@link DefaultAdvisorChainFactory}.
	 */
	public void setAdvisorChainFactory(AdvisorChainFactory advisorChainFactory) {
		Assert.notNull(advisorChainFactory, "AdvisorChainFactory must not be null");
		this.advisorChainFactory = advisorChainFactory;
	}

	/**
	 * Return the advisor chain factory to use (never {@code null}).
	 */
	public AdvisorChainFactory getAdvisorChainFactory() {
		return this.advisorChainFactory;
	}


	/**
	 * Set the interfaces to be proxied.
	 */
	public void setInterfaces(Class<?>... interfaces) {
		Assert.notNull(interfaces, "Interfaces must not be null");
		this.interfaces.clear();
		for (Class<?> ifc : interfaces) {
			addInterface(ifc);
		}
	}

	/**
	 * Add a new proxied interface.
	 * @param ifc the additional interface to proxy
	 */
	public void addInterface(Class<?> ifc) {
		Assert.notNull(ifc, "Interface must not be null");
		if (!ifc.isInterface()) {
			throw new IllegalArgumentException("[" + ifc.getName() + "] is not an interface");
		}
		if (!this.interfaces.contains(ifc)) {
			this.interfaces.add(ifc);
			adviceChanged();
		}
	}

	/**
	 * Remove a proxied interface.
	 * <p>Does nothing if the given interface isn't proxied.
	 * @param ifc the interface to remove from the proxy
	 * @return {@code true} if the interface was removed; {@code false}
	 * if the interface was not found and hence could not be removed
	 */
	public boolean removeInterface(Class<?> ifc) {
		return this.interfaces.remove(ifc);
	}

	@Override
	public Class<?>[] getProxiedInterfaces() {
		return ClassUtils.toClassArray(this.interfaces);
	}

	@Override
	public boolean isInterfaceProxied(Class<?> ifc) {
		for (Class<?> proxyIntf : this.interfaces) {
			if (ifc.isAssignableFrom(proxyIntf)) {
				return true;
			}
		}
		return false;
	}

	boolean hasUserSuppliedInterfaces() {
		for (Class<?> ifc : this.interfaces) {
			if (!SpringProxy.class.isAssignableFrom(ifc) && !isAdvisorIntroducedInterface(ifc)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAdvisorIntroducedInterface(Class<?> ifc) {
		for (Advisor advisor : this.advisors) {
			if (advisor instanceof IntroductionAdvisor introductionAdvisor) {
				for (Class<?> introducedInterface : introductionAdvisor.getInterfaces()) {
					if (introducedInterface == ifc) {
						return true;
					}
				}
			}
		}
		return false;
	}


	@Override
	public final Advisor[] getAdvisors() {
		return this.advisors.toArray(new Advisor[0]);
	}

	@Override
	public int getAdvisorCount() {
		return this.advisors.size();
	}

	@Override
	public void addAdvisor(Advisor advisor) {
		int pos = this.advisors.size();
		addAdvisor(pos, advisor);
	}

	@Override
	public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {
		if (advisor instanceof IntroductionAdvisor introductionAdvisor) {
			validateIntroductionAdvisor(introductionAdvisor);
		}
		addAdvisorInternal(pos, advisor);
	}

	@Override
	public boolean removeAdvisor(Advisor advisor) {
		int index = indexOf(advisor);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public void removeAdvisor(int index) throws AopConfigException {
		if (isFrozen()) {
			throw new AopConfigException("Cannot remove Advisor: Configuration is frozen.");
		}
		if (index < 0 || index > this.advisors.size() - 1) {
			throw new AopConfigException("Advisor index " + index + " is out of bounds: " +
					"This configuration only has " + this.advisors.size() + " advisors.");
		}

		Advisor advisor = this.advisors.remove(index);
		if (advisor instanceof IntroductionAdvisor introductionAdvisor) {
			// We need to remove introduction interfaces.
			for (Class<?> ifc : introductionAdvisor.getInterfaces()) {
				removeInterface(ifc);
			}
		}

		adviceChanged();
	}

	@Override
	public int indexOf(Advisor advisor) {
		Assert.notNull(advisor, "Advisor must not be null");
		return this.advisors.indexOf(advisor);
	}

	@Override
	public boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException {
		Assert.notNull(a, "Advisor a must not be null");
		Assert.notNull(b, "Advisor b must not be null");
		int index = indexOf(a);
		if (index == -1) {
			return false;
		}
		removeAdvisor(index);
		addAdvisor(index, b);
		return true;
	}

	/**
	 * Add all the given advisors to this proxy configuration.
	 * @param advisors the advisors to register
	 */
	public void addAdvisors(Advisor... advisors) {
		addAdvisors(Arrays.asList(advisors));
	}

	/**
	 * Add all the given advisors to this proxy configuration.
	 * @param advisors the advisors to register
	 */
	public void addAdvisors(Collection<Advisor> advisors) {
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (!CollectionUtils.isEmpty(advisors)) {
			for (Advisor advisor : advisors) {
				if (advisor instanceof IntroductionAdvisor introductionAdvisor) {
					validateIntroductionAdvisor(introductionAdvisor);
				}
				Assert.notNull(advisor, "Advisor must not be null");
				this.advisors.add(advisor);
			}
			adviceChanged();
		}
	}

	private void validateIntroductionAdvisor(IntroductionAdvisor advisor) {
		advisor.validateInterfaces();
		// If the advisor passed validation, we can make the change.
		for (Class<?> ifc : advisor.getInterfaces()) {
			addInterface(ifc);
		}
	}

	private void addAdvisorInternal(int pos, Advisor advisor) throws AopConfigException {
		Assert.notNull(advisor, "Advisor must not be null");
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (pos > this.advisors.size()) {
			throw new IllegalArgumentException(
					"Illegal position " + pos + " in advisor list with size " + this.advisors.size());
		}
		this.advisors.add(pos, advisor);
		adviceChanged();
	}

	/**
	 * Allows uncontrolled access to the {@link List} of {@link Advisor Advisors}.
	 * <p>Use with care, and remember to {@link #adviceChanged() fire advice changed events}
	 * when making any modifications.
	 */
	protected final List<Advisor> getAdvisorsInternal() {
		return this.advisors;
	}

	@Override
	public void addAdvice(Advice advice) throws AopConfigException {
		int pos = this.advisors.size();
		addAdvice(pos, advice);
	}

	/**
	 * Cannot add introductions this way unless the advice implements IntroductionInfo.
	 */
	@Override
	public void addAdvice(int pos, Advice advice) throws AopConfigException {
		Assert.notNull(advice, "Advice must not be null");
		if (advice instanceof IntroductionInfo introductionInfo) {
			// We don't need an IntroductionAdvisor for this kind of introduction:
			// It's fully self-describing.
			addAdvisor(pos, new DefaultIntroductionAdvisor(advice, introductionInfo));
		}
		else if (advice instanceof DynamicIntroductionAdvice) {
			// We need an IntroductionAdvisor for this kind of introduction.
			throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
		}
		else {
			addAdvisor(pos, new DefaultPointcutAdvisor(advice));
		}
	}

	@Override
	public boolean removeAdvice(Advice advice) throws AopConfigException {
		int index = indexOf(advice);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public int indexOf(Advice advice) {
		Assert.notNull(advice, "Advice must not be null");
		for (int i = 0; i < this.advisors.size(); i++) {
			Advisor advisor = this.advisors.get(i);
			if (advisor.getAdvice() == advice) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Is the given advice included in any advisor within this proxy configuration?
	 * @param advice the advice to check inclusion of
	 * @return whether this advice instance is included
	 */
	public boolean adviceIncluded(@Nullable Advice advice) {
		if (advice != null) {
			for (Advisor advisor : this.advisors) {
				if (advisor.getAdvice() == advice) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Count advices of the given class.
	 * @param adviceClass the advice class to check
	 * @return the count of the interceptors of this class or subclasses
	 */
	public int countAdvicesOfType(@Nullable Class<?> adviceClass) {
		int count = 0;
		if (adviceClass != null) {
			for (Advisor advisor : this.advisors) {
				if (adviceClass.isInstance(advisor.getAdvice())) {
					count++;
				}
			}
		}
		return count;
	}


	/**
	 * Determine a list of {@link org.aopalliance.intercept.MethodInterceptor} objects
	 * for the given method, based on this configuration.
	 * @param method the proxied method
	 * @param targetClass the target class
	 * @return a List of MethodInterceptors (may also include InterceptorAndDynamicMethodMatchers)
	 */
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, @Nullable Class<?> targetClass) {
		List<Object> cachedInterceptors;
		if (this.methodCache != null) {
			// Method-specific cache for method-specific pointcuts
			MethodCacheKey cacheKey = new MethodCacheKey(method);
			cachedInterceptors = this.methodCache.get(cacheKey);
			if (cachedInterceptors == null) {
				cachedInterceptors = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
						this, method, targetClass);
				this.methodCache.put(cacheKey, cachedInterceptors);
			}
		}
		else {
			// Shared cache since there are no method-specific advisors (see below).
			cachedInterceptors = this.cachedInterceptors;
			if (cachedInterceptors == null) {
				cachedInterceptors = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
						this, method, targetClass);
				this.cachedInterceptors = cachedInterceptors;
			}
		}
		return cachedInterceptors;
	}

	/**
	 * Invoked when advice has changed.
	 */
	protected void adviceChanged() {
		this.methodCache = null;
		this.cachedInterceptors = null;
		this.proxyMetadataCache = null;

		// Initialize method cache if necessary; otherwise,
		// cachedInterceptors is going to be shared (see above).
		for (Advisor advisor : this.advisors) {
			if (advisor instanceof PointcutAdvisor) {
				this.methodCache = new ConcurrentHashMap<>();
				break;
			}
		}
	}

	/**
	 * Call this method on a new instance created by the no-arg constructor
	 * to create an independent copy of the configuration from the given object.
	 * @param other the AdvisedSupport object to copy configuration from
	 */
	protected void copyConfigurationFrom(AdvisedSupport other) {
		copyConfigurationFrom(other, other.targetSource, new ArrayList<>(other.advisors));
	}

	/**
	 * Copy the AOP configuration from the given {@link AdvisedSupport} object,
	 * but allow substitution of a fresh {@link TargetSource} and a given interceptor chain.
	 * @param other the {@code AdvisedSupport} object to take proxy configuration from
	 * @param targetSource the new TargetSource
	 * @param advisors the Advisors for the chain
	 */
	protected void copyConfigurationFrom(AdvisedSupport other, TargetSource targetSource, List<Advisor> advisors) {
		copyFrom(other);
		this.targetSource = targetSource;
		this.advisorChainFactory = other.advisorChainFactory;
		this.interfaces = new ArrayList<>(other.interfaces);
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor introductionAdvisor) {
				validateIntroductionAdvisor(introductionAdvisor);
			}
			Assert.notNull(advisor, "Advisor must not be null");
			this.advisors.add(advisor);
		}
		adviceChanged();
	}

	/**
	 * Build a configuration-only copy of this {@link AdvisedSupport},
	 * replacing the {@link TargetSource}.
	 */
	AdvisedSupport getConfigurationOnlyCopy() {
		AdvisedSupport copy = new AdvisedSupport();
		copy.copyFrom(this);
		copy.targetSource = EmptyTargetSource.forClass(getTargetClass(), getTargetSource().isStatic());
		copy.preFiltered = this.preFiltered;
		copy.advisorChainFactory = this.advisorChainFactory;
		copy.interfaces = new ArrayList<>(this.interfaces);
		copy.advisors = new ArrayList<>(this.advisors);
		copy.advisorKey = new ArrayList<>(this.advisors.size());
		for (Advisor advisor : this.advisors) {
			copy.advisorKey.add(new AdvisorKeyEntry(advisor));
		}
		copy.methodCache = this.methodCache;
		copy.cachedInterceptors = this.cachedInterceptors;
		copy.proxyMetadataCache = this.proxyMetadataCache;
		return copy;
	}

	void reduceToAdvisorKey() {
		this.advisors = this.advisorKey;
		this.methodCache = null;
		this.cachedInterceptors = null;
		this.proxyMetadataCache = null;
	}

	Object getAdvisorKey() {
		return this.advisorKey;
	}


	@Override
	public String toProxyConfigString() {
		return toString();
	}

	/**
	 * For debugging/diagnostic use.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": ").append(this.interfaces.size()).append(" interfaces ");
		sb.append(ClassUtils.classNamesToString(this.interfaces)).append("; ");
		sb.append(this.advisors.size()).append(" advisors ");
		sb.append(this.advisors).append("; ");
		sb.append("targetSource [").append(this.targetSource).append("]; ");
		sb.append(super.toString());
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize method cache if necessary.
		adviceChanged();
	}


	/**
	 * Simple wrapper class around a Method. Used as the key when
	 * caching methods, for efficient equals and hashCode comparisons.
	 */
	private static final class MethodCacheKey implements Comparable<MethodCacheKey> {

		private final Method method;

		private final int hashCode;

		public MethodCacheKey(Method method) {
			this.method = method;
			this.hashCode = method.hashCode();
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof MethodCacheKey that &&
					(this.method == that.method || this.method.equals(that.method))));
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public String toString() {
			return this.method.toString();
		}

		@Override
		public int compareTo(MethodCacheKey other) {
			int result = this.method.getName().compareTo(other.method.getName());
			if (result == 0) {
				result = this.method.toString().compareTo(other.method.toString());
			}
			return result;
		}
	}


	/**
	 * Stub for an {@link Advisor} instance that is just needed for key purposes,
	 * allowing for efficient equals and hashCode comparisons against the
	 * advice class and the pointcut.
	 * @since 6.0.10
	 * @see #getConfigurationOnlyCopy()
	 * @see #getAdvisorKey()
	 */
	private static final class AdvisorKeyEntry implements Advisor {

		private final Class<?> adviceType;

		private final @Nullable String classFilterKey;

		private final @Nullable String methodMatcherKey;

		public AdvisorKeyEntry(Advisor advisor) {
			this.adviceType = advisor.getAdvice().getClass();
			if (advisor instanceof PointcutAdvisor pointcutAdvisor) {
				Pointcut pointcut = pointcutAdvisor.getPointcut();
				this.classFilterKey = pointcut.getClassFilter().toString();
				this.methodMatcherKey = pointcut.getMethodMatcher().toString();
			}
			else {
				this.classFilterKey = null;
				this.methodMatcherKey = null;
			}
		}

		@Override
		public Advice getAdvice() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof AdvisorKeyEntry that &&
					this.adviceType == that.adviceType &&
					ObjectUtils.nullSafeEquals(this.classFilterKey, that.classFilterKey) &&
					ObjectUtils.nullSafeEquals(this.methodMatcherKey, that.methodMatcherKey)));
		}

		@Override
		public int hashCode() {
			return this.adviceType.hashCode();
		}
	}

}
