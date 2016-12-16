/*
 * Copyright 2020-2020 the original author or authors.
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

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.function.Function;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.Pipe;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static net.bytebuddy.matcher.ElementMatchers.isAccessibleTo;
import static net.bytebuddy.matcher.ElementMatchers.not;

@SuppressWarnings("serial")
public class ByteBuddyAopProxy implements AopProxy, Serializable {

	private static final String ADVISED = "advised";

	private static final ClassLoadingStrategy<ClassLoader> STRATEGY = ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V9)
			? ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup())
			: ClassLoadingStrategy.Default.INJECTION;

	/**
	 * Logger available to subclasses; static to optimize serialization.
	 */
	protected static final Log logger = LogFactory.getLog(ByteBuddyAopProxy.class);

	/**
	 * A cache that is used for avoiding repeated proxy creation.
	 */
	private static final TypeCache<Object> cache = new TypeCache.WithInlineExpunction<>(TypeCache.Sort.SOFT);

	/**
	 * Keeps track of the Classes that we have validated for final methods.
	 */
	private static final Map<Class<?>, Boolean> validatedClasses = new WeakHashMap<>();

	/**
	 * The configuration used to configure this proxy.
	 */
	protected final AdvisedSupport advised;

	@Nullable
	protected Object[] constructorArgs;

	@Nullable
	protected Class<?>[] constructorArgTypes;

	/**
	 * Create a new ByteBuddyAopProxy for the given AOP configuration.
	 *
	 * @param config the AOP configuration as AdvisedSupport object
	 * @throws AopConfigException if the config is invalid. We try to throw an informative
	 *                            exception in this case, rather than let a mysterious failure happen later.
	 */
	public ByteBuddyAopProxy(AdvisedSupport config) throws AopConfigException {
		Assert.notNull(config, "AdvisedSupport must not be null");
		if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		this.advised = config;
	}

	/**
	 * Set constructor arguments to use for creating the proxy.
	 *
	 * @param constructorArgs     the constructor argument values
	 * @param constructorArgTypes the constructor argument types
	 */
	public void setConstructorArguments(@Nullable Object[] constructorArgs, @Nullable Class<?>[] constructorArgTypes) {
		if (constructorArgs == null || constructorArgTypes == null) {
			throw new IllegalArgumentException("Both 'constructorArgs' and 'constructorArgTypes' need to be specified");
		}
		if (constructorArgs.length != constructorArgTypes.length) {
			throw new IllegalArgumentException("Number of 'constructorArgs' (" + constructorArgs.length +
					") must match number of 'constructorArgTypes' (" + constructorArgTypes.length + ")");
		}
		this.constructorArgs = constructorArgs;
		this.constructorArgTypes = constructorArgTypes;
	}

	@Override
	public Object getProxy() {
		return getProxy(null);
	}

	@Override
	public Object getProxy(@Nullable ClassLoader classLoader) {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating Byte Buddy proxy: target source is " + this.advised.getTargetSource());
		}

		try {
			Class<?> rootClass = this.advised.getTargetClass();
			Assert.state(rootClass != null, "Target class must be available for creating a Byte Buddy proxy");

			Class<?> proxySuperClass = rootClass;
			if (rootClass.getName().contains(ClassUtils.BYTE_BUDDY_CLASS_INFIX)) {
				proxySuperClass = rootClass.getSuperclass();
				Class<?>[] additionalInterfaces = rootClass.getInterfaces();
				for (Class<?> additionalInterface : additionalInterfaces) {
					if (additionalInterface != ByteBuddyProxy.class) {
						this.advised.addInterface(additionalInterface);
					}
				}
			}

			validateClassIfNecessary(proxySuperClass, classLoader);

			boolean useCache = !(classLoader instanceof SmartClassLoader) || ((SmartClassLoader) classLoader).isClassReloadable(proxySuperClass);

			boolean exposeProxy = this.advised.isExposeProxy();
			boolean isStatic = this.advised.getTargetSource().isStatic();
			boolean isFrozen = this.advised.isFrozen();
			Class<?>[] proxyInterfaces = Arrays.stream(AopProxyUtils.completeProxiedInterfaces(this.advised))
					.filter(iface -> iface != ByteBuddyProxy.class)
					.toArray(Class[]::new);

			Class<?> proxyType = null;

			Object cacheKey = null;

			ClassLoader targetClassLoader;
			if (classLoader == null) {
				targetClassLoader = proxySuperClass.getClassLoader();
				if (targetClassLoader == null) {
					targetClassLoader = getClass().getClassLoader();
				}
			}
			else {
				targetClassLoader = classLoader;
			}

			if (useCache) {
				cacheKey = generateCacheKey(proxySuperClass, proxyInterfaces, exposeProxy, isStatic, isFrozen);
				proxyType = cache.find(targetClassLoader, cacheKey);
			}

			if (proxyType == null) {

				synchronized (cache) {

					if (useCache) {
						proxyType = cache.find(targetClassLoader, cacheKey);
					}

					if (proxyType == null) {
						ByteBuddy byteBuddy = createByteBuddy();
						byteBuddy = byteBuddy.ignore(ElementMatchers.none());
						byteBuddy = byteBuddy.with(new NamingStrategy.SuffixingRandom(ClassUtils.BYTE_BUDDY_CLASS_INFIX));

						DynamicType.Builder<?> builder = byteBuddy.subclass(proxySuperClass);
						builder = builder.ignoreAlso(not(isAccessibleTo(proxySuperClass))).implement(proxyInterfaces);

						builder = configure(builder, rootClass, exposeProxy, isStatic, isFrozen);

						builder = builder.defineField(ADVISED, AdvisedSupport.class, Visibility.PRIVATE);
						builder = builder.implement(ByteBuddyProxy.class)
								.method(ElementMatchers.named("$$_spring_setAdvised").or(ElementMatchers.named("$$_spring_getAdvised")))
								.intercept(FieldAccessor.ofField(ADVISED));

						proxyType = builder.make().load(targetClassLoader, STRATEGY).getLoaded();

						if (useCache) {
							proxyType = cache.insert(targetClassLoader, cacheKey, proxyType);
						}
					}
				}

			}

			Object proxy = createProxyInstance(proxyType, useCache);
			((ByteBuddyProxy) proxy).$$_spring_setAdvised(this.advised);

			return proxy;
		}
		catch (IllegalStateException ex) {
			throw new AopConfigException("Could not generate Byte Buddy subclass of class [" +
					this.advised.getTargetClass() + "]: " +
					"Common causes of this problem include using a final class or a non-visible class",
					ex);
		}
		catch (Exception ex) {
			// TargetSource.getTarget() failed
			throw new AopConfigException("Unexpected AOP exception", ex);
		}
	}

	protected Object createProxyInstance(Class<?> proxyClass, boolean useCache) throws Exception {
		return this.constructorArgs != null ?
				proxyClass.getDeclaredConstructor(this.constructorArgTypes).newInstance(this.constructorArgs) :
				proxyClass.getDeclaredConstructor().newInstance();
	}

	/**
	 * Creates a {@link ByteBuddy} configuration. Subclasses may wish to override this to return a custom
	 * configuration.
	 */
	protected ByteBuddy createByteBuddy() {
		return new ByteBuddy().with(TypeValidation.DISABLED);
	}

	/**
	 * Checks to see whether the supplied {@code Class} has already been validated and
	 * validates it if not.
	 */
	private void validateClassIfNecessary(Class<?> proxySuperClass, @Nullable ClassLoader proxyClassLoader) {
		if (logger.isInfoEnabled()) {
			synchronized (validatedClasses) {
				if (!validatedClasses.containsKey(proxySuperClass)) {
					doValidateClass(proxySuperClass, proxyClassLoader);
					validatedClasses.put(proxySuperClass, Boolean.TRUE);
				}
			}
		}
	}

	/**
	 * Checks for final methods on the given {@code Class}, as well as package-visible
	 * methods across ClassLoaders, and writes warnings to the log for each one found.
	 */
	private void doValidateClass(Class<?> proxySuperClass, @Nullable ClassLoader proxyClassLoader) {
		if (Object.class != proxySuperClass) {
			Method[] methods = proxySuperClass.getDeclaredMethods();
			for (Method method : methods) {
				int mod = method.getModifiers();
				if (!Modifier.isStatic(mod)) {
					if (Modifier.isFinal(mod)) {
						logger.info("Unable to proxy method [" + method + "] because it is final: " +
								"All calls to this method via a proxy will NOT be routed to the target instance.");
					}
					else if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod) && !Modifier.isPrivate(mod) &&
							proxyClassLoader != null && proxySuperClass.getClassLoader() != proxyClassLoader) {
						logger.info("Unable to proxy method [" + method + "] because it is package-visible " +
								"across different ClassLoaders: All calls to this method via a proxy will " +
								"NOT be routed to the target instance.");
					}
				}
			}
			doValidateClass(proxySuperClass.getSuperclass(), proxyClassLoader);
		}
	}

	/**
	 * Process a return value. Wraps a return of {@code this} if necessary to be the
	 * {@code proxy} and also verifies that {@code null} is not returned as a primitive.
	 */
	@Nullable
	private static Object processReturnType(
			Object proxy, @Nullable Object target, Method method, @Nullable Object returnValue) {
		// Massage return value if necessary
		if (returnValue != null && returnValue == target &&
				!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
			// Special case: it returned "this". Note that we can't help
			// if the target sets a reference to itself in another returned object.
			returnValue = proxy;
		}
		Class<?> returnType = method.getReturnType();
		if (returnValue == null && returnType != Void.TYPE && returnType.isPrimitive()) {
			throw new AopInvocationException(
					"Null return value from advice does not match primitive return type for: " + method);
		}
		return returnValue;
	}

	/**
	 * Allows to configure a class in a custom manner. A custom configuration must yield the same proxy for equal
	 * input parameters. Alternatively, {@link ByteBuddyAopProxy#generateCacheKey(Class, Class[], boolean, boolean, boolean)}
	 * can be overridden where any custom configuration must yield a key with equal constraints.
	 *
	 * @param builder     the builder that should be used for creating the proxy.
	 * @param rootClass   the root class that is being proxied.
	 * @param exposeProxy if this proxy is exposed.
	 * @param isStatic    if this proxy is static.
	 * @param isFrozen    if this proxy is frozen.
	 * @return a fully configured builder.
	 */
	protected DynamicType.Builder<?> configure(DynamicType.Builder<?> builder,
			Class<?> rootClass,
			boolean exposeProxy,
			boolean isStatic,
			boolean isFrozen) {

		Class<?> targetClass = this.advised.getTargetClass();

		MethodDelegation.WithCustomProperties invokeConfiguration = MethodDelegation.withDefaultConfiguration()
				.withBinders(Pipe.Binder.install(Function.class));
		MethodDelegation invokeTarget;
		if (exposeProxy) {
			invokeTarget = invokeConfiguration.to(isStatic ?
					StaticUnadvisedExposedInterceptor.class :
					DynamicUnadvisedExposedInterceptor.class);
		}
		else {
			invokeTarget = invokeConfiguration.to(isStatic ?
					StaticUnadvisedInterceptor.class :
					DynamicUnadvisedInterceptor.class);
		}

		MethodDelegation aopProxy = MethodDelegation.to(DynamicAdvisedInterceptor.class);

		Implementation adviceDispatched = MethodCall.invokeSelf().onField(ADVISED).withAllArguments();

		Implementation dispatchTarget = isStatic ?
				MethodDelegation.withDefaultConfiguration().withBinders(Pipe.Binder.install(Function.class)).to(ForwardingInterceptor.class) :
				SuperMethodCall.INSTANCE;

		builder = builder.ignoreAlso((ElementMatcher<MethodDescription>) target -> {
			if (ElementMatchers.isFinalizer().matches(target)) {
				logger.debug("Found finalize() method - using NO_OVERRIDE");
				return true;
			}
			return false;
		});

		builder = builder.method(target -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Method " + target +
						"has return type that is assignable from the target type (may return this) - " +
						"using INVOKE_TARGET");
			}
			return true;
		}).intercept(invokeTarget);

		builder = builder.method(target -> {
			TypeDescription returnType = target.getReturnType().asErasure();
			if (returnType.isPrimitive() || !returnType.isAssignableFrom(targetClass)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Method " + target +
							" has return type that ensures this cannot be returned- using DISPATCH_TARGET");
				}
				return true;
			}
			return false;
		}).intercept(dispatchTarget);

		builder = builder.method(target -> {
			TypeDescription returnType = target.getReturnType().asErasure();
			if (returnType.represents(targetClass)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Method " + target +
							"has return type same as target type (may return this) - using INVOKE_TARGET");
				}
				return true;
			}
			return false;
		}).intercept(invokeTarget);

		// See if the return type of the method is outside the class hierarchy
		// of the target type. If so we know it never needs to have return type
		// massage and can use a dispatcher.
		// If the proxy is being exposed, then must use the interceptor the
		// correct one is already configured. If the target is not static, then
		// cannot use a dispatcher because the target cannot be released.
		builder = builder.method(target -> exposeProxy || !isStatic).intercept(invokeTarget);

		builder = builder.method(target -> {
			Method method = ((MethodDescription.ForLoadedMethod) target.asDefined()).getLoadedMethod();
			if (!this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass).isEmpty() || !isFrozen) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unable to apply any optimisations to advised method: " + target);
				}
				return true;
			}
			else {
				return false;
			}
		}).intercept(aopProxy);

		if (isStatic && isFrozen) {
			Method[] methods = rootClass.getMethods();

			// TODO: small memory optimisation here (can skip creation for methods with no advice)
			for (Method method : methods) {
				List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, rootClass);
				Implementation fixedChainStaticTargetInterceptor = MethodDelegation.to(new FixedChainStaticTargetInterceptor(chain));

				builder = builder.method(target -> {
					if (target.asDefined().represents(method)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Method has advice and optimisations are enabled: " + target);
						}
						return true;
					}
					return false;
				}).intercept(fixedChainStaticTargetInterceptor);
			}
		}

		builder = builder.method(target -> {
			// If exposing the proxy, then AOP_PROXY must be used.
			if (exposeProxy) {
				if (logger.isDebugEnabled()) {
					logger.debug("Must expose proxy on advised method: " + target);
				}
				return true;
			}
			return false;
		}).intercept(aopProxy);

		builder = builder.method(target -> {
			if (ElementMatchers.isHashCode().matches(target)) {
				logger.debug("Found 'hashCode' method: " + target);
				return true;
			}
			return false;
		}).intercept(MethodDelegation.to(HashCodeInterceptor.class));

		builder = builder.method(target -> {
			if (ElementMatchers.isEquals().matches(target)) {
				logger.debug("Found 'equals' method: " + target);
				return true;
			}
			return false;
		}).intercept(MethodDelegation.to(EqualsInterceptor.class));

		builder = builder.method(target -> {
			if (!this.advised.isOpaque() && target.getDeclaringType().isInterface() &&
					target.getDeclaringType().asErasure().isAssignableFrom(Advised.class)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Method is declared on Advised interface: " + target);
				}
				return true;
			}
			return false;
		}).intercept(adviceDispatched);

		return builder;
	}

	/**
	 * Allows the generation of a unique key for any generated proxy. By default, a key takes all parameters of this
	 * method into account and also remembers the class of this instance.
	 *
	 * @param rootClass       the root class that is being proxied.
	 * @param proxyInterfaces any additional proxy interfaces.
	 * @param exposeProxy     if this proxy is exposed.
	 * @param isStatic        if this proxy is static.
	 * @param isFrozen        if this proxy is frozen.
	 * @return a unique key for the proxy with appropriate implementations of {@link Object#hashCode()} and
	 * {@link Object#equals(Object)}.
	 */
	protected Object generateCacheKey(Class<?> rootClass,
			Class<?>[] proxyInterfaces,
			boolean exposeProxy,
			boolean isStatic,
			boolean isFrozen) {
		return new CacheKey(getClass(), rootClass, proxyInterfaces, exposeProxy, isStatic, isFrozen);
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof ByteBuddyAopProxy &&
				AopProxyUtils.equalsInProxy(this.advised, ((ByteBuddyAopProxy) other).advised)));
	}

	@Override
	public int hashCode() {
		return ByteBuddyAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}

	/**
	 * A Spring proxy that is generated with Byte Buddy.
	 */
	public interface ByteBuddyProxy {

		AdvisedSupport $$_spring_getAdvised();

		void $$_spring_setAdvised(AdvisedSupport advised);
	}

	/**
	 * Method interceptor used for static targets with no advice chain. The call
	 * is passed directly back to the target. Used when the proxy needs to be
	 * exposed and it can't be determined that the method won't return
	 * {@code this}.
	 */
	public static class StaticUnadvisedInterceptor {

		@Nullable
		@RuntimeType
		public static Object intercept(@Nullable @FieldValue(ADVISED) AdvisedSupport advised,
				@This Object proxy,
				@Origin Method method,
				@Pipe Function<Object, ?> forward,
				@Nullable @SuperCall(nullIfImpossible = true) Callable<?> superCall) throws Throwable {
			if (advised == null) {
				if (superCall == null) {
					throw new AbstractMethodError();
				}
				else {
					return superCall.call();
				}
			}
			Object target = advised.getTargetSource().getTarget();
			Object returnValue = forward.apply(target);
			return processReturnType(proxy, target, method, returnValue);
		}
	}

	/**
	 * Method interceptor used for static targets with no advice chain, when the
	 * proxy is to be exposed.
	 */
	public static class StaticUnadvisedExposedInterceptor {

		@Nullable
		@RuntimeType
		public static Object intercept(@Nullable @FieldValue(ADVISED) AdvisedSupport advised,
				@This Object proxy,
				@Origin Method method,
				@Pipe Function<Object, ?> forward,
				@Nullable @SuperCall(nullIfImpossible = true) Callable<?> superCall) throws Throwable {
			if (advised == null) {
				if (superCall == null) {
					throw new AbstractMethodError();
				}
				else {
					return superCall.call();
				}
			}
			Object target = advised.getTargetSource().getTarget();
			Object oldProxy = null;
			try {
				oldProxy = AopContext.setCurrentProxy(proxy);
				Object returnValue = forward.apply(target);
				return processReturnType(proxy, target, method, returnValue);
			}
			finally {
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}

	/**
	 * Interceptor used to invoke a dynamic target without creating a method
	 * invocation or evaluating an advice chain. (We know there was no advice
	 * for this method.)
	 */
	public static class DynamicUnadvisedInterceptor {

		@Nullable
		@RuntimeType
		public static Object intercept(@Nullable @FieldValue(ADVISED) AdvisedSupport advised,
				@This Object proxy,
				@Origin Method method,
				@Pipe Function<Object, ?> forward,
				@Nullable @SuperCall(nullIfImpossible = true) Callable<?> superCall) throws Throwable {
			if (advised == null) {
				if (superCall == null) {
					throw new AbstractMethodError();
				}
				else {
					return superCall.call();
				}
			}
			TargetSource targetSource = advised.getTargetSource();
			Object target = targetSource.getTarget();
			try {
				Object returnValue = forward.apply(target);
				return processReturnType(proxy, target, method, returnValue);
			}
			finally {
				targetSource.releaseTarget(target);
			}
		}
	}

	/**
	 * Interceptor for unadvised dynamic targets when the proxy needs exposing.
	 */
	public static class DynamicUnadvisedExposedInterceptor {

		@Nullable
		@RuntimeType
		public static Object intercept(@Nullable @FieldValue(ADVISED) AdvisedSupport advised,
				@This Object proxy,
				@Origin Method method,
				@Pipe Function<Object, ?> forward,
				@Nullable @SuperCall(nullIfImpossible = true) Callable<?> superCall) throws Throwable {
			if (advised == null) {
				if (superCall == null) {
					throw new AbstractMethodError();
				}
				else {
					return superCall.call();
				}
			}
			TargetSource targetSource = advised.getTargetSource();
			Object oldProxy = null;
			Object target = targetSource.getTarget();
			try {
				oldProxy = AopContext.setCurrentProxy(proxy);
				Object returnValue = forward.apply(target);
				return processReturnType(proxy, target, method, returnValue);
			}
			finally {
				AopContext.setCurrentProxy(oldProxy);
				targetSource.releaseTarget(target);
			}
		}
	}

	public static class ForwardingInterceptor {

		@Nullable
		@RuntimeType
		public static Object intercept(@Nullable @FieldValue(ADVISED) AdvisedSupport advised,
				@Pipe Function<Object, ?> forward,
				@Nullable @SuperCall(nullIfImpossible = true) Callable<?> superCall) throws Throwable {
			if (advised == null) {
				if (superCall == null) {
					throw new AbstractMethodError();
				}
				else {
					return superCall.call();
				}
			}
			return forward.apply(advised.getTargetSource().getTarget());
		}
	}

	/**
	 * Dispatcher for the {@code equals} method.
	 * Ensures that the method call is always handled by this class.
	 */
	public static class EqualsInterceptor {

		@Nullable
		@RuntimeType
		public static Object intercept(@Nullable @FieldValue(ADVISED) AdvisedSupport advised,
				@This Object proxy,
				@Nullable @Argument(0) Object other,
				@Nullable @SuperCall(nullIfImpossible = true) Callable<?> superCall) throws Throwable {
			if (advised == null) {
				if (superCall == null) {
					throw new AbstractMethodError();
				}
				else {
					return superCall.call();
				}
			}
			if (proxy == other) {
				return true;
			}
			if (other instanceof ByteBuddyProxy) {
				AdvisedSupport otherAdvised = ((ByteBuddyProxy) other).$$_spring_getAdvised();
				return AopProxyUtils.equalsInProxy(advised, otherAdvised);
			}
			else {
				return false;
			}
		}
	}

	/**
	 * Dispatcher for the {@code hashCode} method.
	 * Ensures that the method call is always handled by this class.
	 */
	public static class HashCodeInterceptor {

		@Nullable
		@RuntimeType
		public static Object intercept(@Nullable @FieldValue(ADVISED) AdvisedSupport advised,
				@Nullable @SuperCall(nullIfImpossible = true) Callable<?> superCall) throws Throwable {
			if (advised == null) {
				if (superCall == null) {
					throw new AbstractMethodError();
				}
				else {
					return superCall.call();
				}
			}
			return ByteBuddyAopProxy.class.hashCode() * 13 + advised.getTargetSource().hashCode();
		}
	}

	/**
	 * Interceptor used specifically for advised methods on a frozen, static proxy.
	 */
	public static class FixedChainStaticTargetInterceptor implements Serializable {

		private final List<Object> adviceChain;

		public FixedChainStaticTargetInterceptor(List<Object> adviceChain) {
			this.adviceChain = adviceChain;
		}

		@Nullable
		@RuntimeType
		public Object intercept(@Nullable @FieldValue(ADVISED) AdvisedSupport advised,
				@This Object proxy,
				@Origin Method method,
				@AllArguments Object[] args,
				@Nullable @SuperCall(nullIfImpossible = true) Callable<?> superCall) throws Throwable {
			if (advised == null) {
				if (superCall == null) {
					throw new AbstractMethodError();
				}
				else {
					return superCall.call();
				}
			}
			Object target = advised.getTargetSource().getTarget();
			MethodInvocation invocation = new ReflectiveMethodInvocation(proxy, target, method, args,
					advised.getTargetClass(), this.adviceChain);
			// If we get here, we need to create a MethodInvocation.
			Object returnValue = invocation.proceed();
			returnValue = processReturnType(proxy, target, method, returnValue);
			return returnValue;
		}
	}

	/**
	 * General purpose AOP callback. Used when the target is dynamic or when the
	 * proxy is not frozen.
	 */
	public static class DynamicAdvisedInterceptor {

		@Nullable
		@RuntimeType
		public static Object intercept(@Nullable @FieldValue(ADVISED) AdvisedSupport advised,
				@This Object proxy,
				@Origin Method method,
				@AllArguments Object[] args,
				@Nullable @SuperCall(nullIfImpossible = true) Callable<?> superCall) throws Throwable {
			if (advised == null) {
				if (superCall == null) {
					throw new AbstractMethodError();
				}
				else {
					return superCall.call();
				}
			}
			Object oldProxy = null;
			boolean setProxyContext = false;
			Class<?> targetClass = null;
			Object target = null;
			try {
				if (advised.exposeProxy) {
					// Make invocation available if necessary.
					oldProxy = AopContext.setCurrentProxy(proxy);
					setProxyContext = true;
				}
				// May be null. Get as late as possible to minimize the time we
				// "own" the target, in case it comes from a pool...
				target = advised.getTargetSource().getTarget();
				if (target != null) {
					targetClass = target.getClass();
				}
				List<Object> chain = advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
				Object returnValue;
				// Check whether we only have one InvokerInterceptor: that is,
				// no real advice, but just reflective invocation of the target.
				if (chain.isEmpty() && Modifier.isPublic(method.getModifiers()) && Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
					// We can skip creating a MethodInvocation: just invoke the target directly.
					// Note that the final invoker must be an InvokerInterceptor, so we know
					// it does nothing but a reflective operation on the target, and no hot
					// swapping or fancy proxying.
					Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
					try {
						returnValue = method.invoke(target, argsToUse);
					}
					catch (InvocationTargetException exception) {
						throw exception.getCause();
					}
				}
				else {
					// We need to create a method invocation...
					try {
						returnValue = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain).proceed();
					}
					catch (Throwable throwable) {
						if (throwable instanceof RuntimeException || throwable instanceof Error) {
							throw throwable;
						}
						for (Class<?> exceptionType : method.getExceptionTypes()) {
							if (exceptionType.isInstance(throwable)) {
								throw throwable;
							}
						}
						throw new UndeclaredThrowableException(throwable);
					}
				}
				returnValue = processReturnType(proxy, target, method, returnValue);
				return returnValue;
			}
			finally {
				if (target != null) {
					advised.getTargetSource().releaseTarget(target);
				}
				if (setProxyContext) {
					// Restore old proxy.
					AopContext.setCurrentProxy(oldProxy);
				}
			}
		}
	}

	private static final class CacheKey {

		private final Class<?> proxyGenerator;

		private final Set<String> types;

		private final boolean exposeProxy;

		private final boolean isStatic;

		private final boolean isFrozen;

		private CacheKey(Class<?> proxyGenerator,
				Class<?> rootClass,
				Class<?>[] proxyInterfaces,
				boolean exposeProxy,
				boolean isStatic,
				boolean isFrozen) {
			this.proxyGenerator = proxyGenerator;
			this.types = new HashSet<>();
			this.types.add(rootClass.getName());
			for (Class<?> proxyInterface : proxyInterfaces) {
				this.types.add(proxyInterface.getName());
			}
			this.exposeProxy = exposeProxy;
			this.isStatic = isStatic;
			this.isFrozen = isFrozen;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (object == null || getClass() != object.getClass()) {
				return false;
			}
			CacheKey key = (CacheKey) object;
			if (this.proxyGenerator != key.proxyGenerator) {
				return false;
			}
			if (this.exposeProxy != key.exposeProxy) {
				return false;
			}
			if (this.isStatic != key.isStatic) {
				return false;
			}
			if (this.isFrozen != key.isFrozen) {
				return false;
			}
			return this.types.equals(key.types);
		}

		@Override
		public int hashCode() {
			int result = this.types.hashCode();
			result = 31 * result + this.proxyGenerator.hashCode();
			result = 31 * result + (this.exposeProxy ? 1 : 0);
			result = 31 * result + (this.isStatic ? 1 : 0);
			result = 31 * result + (this.isFrozen ? 1 : 0);
			return result;
		}
	}
}
