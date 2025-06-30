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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.UnknownAdviceTypeException;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} implementation that builds an
 * AOP proxy based on beans in a Spring {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>{@link org.aopalliance.intercept.MethodInterceptor MethodInterceptors} and
 * {@link org.springframework.aop.Advisor Advisors} are identified by a list of bean
 * names in the current bean factory, specified through the "interceptorNames" property.
 * The last entry in the list can be the name of a target bean or a
 * {@link org.springframework.aop.TargetSource}; however, it is normally preferable
 * to use the "targetName"/"target"/"targetSource" properties instead.
 *
 * <p>Global interceptors and advisors can be added at the factory level. The specified
 * ones are expanded in an interceptor list where an "xxx*" entry is included in the
 * list, matching the given prefix with the bean names &mdash; for example, "global*"
 * would match both "globalBean1" and "globalBean2"; whereas, "*" would match all
 * defined interceptors. The matching interceptors get applied according to their
 * returned order value, if they implement the {@link org.springframework.core.Ordered}
 * interface.
 *
 * <p>Creates a JDK proxy when proxy interfaces are given, and a CGLIB proxy for the
 * actual target class if not. Note that the latter will only work if the target class
 * does not have final methods, as a dynamic subclass will be created at runtime.
 *
 * <p>It's possible to cast a proxy obtained from this factory to {@link Advised},
 * or to obtain the ProxyFactoryBean reference and programmatically manipulate it.
 * This won't work for existing prototype references, which are independent. However,
 * it will work for prototypes subsequently obtained from the factory. Changes to
 * interception will work immediately on singletons (including existing references).
 * However, to change interfaces or a target it's necessary to obtain a new instance
 * from the factory. This means that singleton instances obtained from the factory
 * do not have the same object identity. However, they do have the same interceptors
 * and target, and changing any reference will change all objects.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setInterceptorNames
 * @see #setProxyInterfaces
 * @see org.aopalliance.intercept.MethodInterceptor
 * @see org.springframework.aop.Advisor
 * @see Advised
 */
@SuppressWarnings("serial")
public class ProxyFactoryBean extends ProxyCreatorSupport
		implements FactoryBean<Object>, BeanClassLoaderAware, BeanFactoryAware {

	/**
	 * This suffix in a value in an interceptor list indicates to expand globals.
	 */
	public static final String GLOBAL_SUFFIX = "*";


	private static final Log logger = LogFactory.getLog(ProxyFactoryBean.class);

	private String @Nullable [] interceptorNames;

	private @Nullable String targetName;

	private boolean autodetectInterfaces = true;

	private boolean singleton = true;

	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	private boolean freezeProxy = false;

	private transient @Nullable ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private transient boolean classLoaderConfigured = false;

	private transient @Nullable BeanFactory beanFactory;

	/** Whether the advisor chain has already been initialized. */
	private boolean advisorChainInitialized = false;

	/** If this is a singleton, the cached singleton proxy instance. */
	private @Nullable Object singletonInstance;


	/**
	 * Set the names of the interfaces we're proxying. If no interface
	 * is given, a CGLIB for the actual class will be created.
	 * <p>This is essentially equivalent to the "setInterfaces" method,
	 * but mirrors TransactionProxyFactoryBean's "setProxyInterfaces".
	 * @see #setInterfaces
	 * @see AbstractSingletonProxyFactoryBean#setProxyInterfaces
	 */
	public void setProxyInterfaces(Class<?>[] proxyInterfaces) throws ClassNotFoundException {
		setInterfaces(proxyInterfaces);
	}

	/**
	 * Set the list of Advice/Advisor bean names. This must always be set
	 * to use this factory bean in a bean factory.
	 * <p>The referenced beans should be of type Interceptor, Advisor or Advice
	 * The last entry in the list can be the name of any bean in the factory.
	 * If it's neither an Advice nor an Advisor, a new SingletonTargetSource
	 * is added to wrap it. Such a target bean cannot be used if the "target"
	 * or "targetSource" or "targetName" property is set, in which case the
	 * "interceptorNames" array must contain only Advice/Advisor bean names.
	 * <p><b>NOTE: Specifying a target bean as final name in the "interceptorNames"
	 * list is deprecated and will be removed in a future Spring version.</b>
	 * Use the {@link #setTargetName "targetName"} property instead.
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see org.springframework.aop.Advisor
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.target.SingletonTargetSource
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * Set the name of the target bean. This is an alternative to specifying
	 * the target name at the end of the "interceptorNames" array.
	 * <p>You can also specify a target object or a TargetSource object
	 * directly, via the "target"/"targetSource" property, respectively.
	 * @see #setInterceptorNames(String[])
	 * @see #setTarget(Object)
	 * @see #setTargetSource(org.springframework.aop.TargetSource)
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	/**
	 * Set whether to autodetect proxy interfaces if none specified.
	 * <p>Default is "true". Turn this flag off to create a CGLIB
	 * proxy for the full target class if no interfaces specified.
	 * @see #setProxyTargetClass
	 */
	public void setAutodetectInterfaces(boolean autodetectInterfaces) {
		this.autodetectInterfaces = autodetectInterfaces;
	}

	/**
	 * Set the value of the singleton property. Governs whether this factory
	 * should always return the same proxy instance (which implies the same target)
	 * or whether it should return a new prototype instance, which implies that
	 * the target and interceptors may be new instances also, if they are obtained
	 * from prototype bean definitions. This allows for fine control of
	 * independence/uniqueness in the object graph.
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	/**
	 * Specify the AdvisorAdapterRegistry to use.
	 * Default is the global AdvisorAdapterRegistry.
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	/**
	 * Set the ClassLoader to generate the proxy class in.
	 * <p>Default is the bean ClassLoader, i.e. the ClassLoader used by the
	 * containing BeanFactory for loading all bean classes. This can be
	 * overridden here for specific proxies.
	 */
	public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		checkInterceptorNames();
	}


	/**
	 * Return a proxy. Invoked when clients obtain beans from this factory bean.
	 * Create an instance of the AOP proxy to be returned by this factory.
	 * The instance will be cached for a singleton, and create on each call to
	 * {@code getObject()} for a proxy.
	 * @return a fresh AOP proxy reflecting the current state of this factory
	 */
	@Override
	public @Nullable Object getObject() throws BeansException {
		initializeAdvisorChain();
		if (isSingleton()) {
			return getSingletonInstance();
		}
		else {
			if (this.targetName == null) {
				logger.info("Using non-singleton proxies with singleton targets is often undesirable. " +
						"Enable prototype proxies by setting the 'targetName' property.");
			}
			return newPrototypeInstance();
		}
	}

	/**
	 * Return the type of the proxy. Will check the singleton instance if
	 * already created, else fall back to the proxy interface (in case of just
	 * a single one), the target bean type, or the TargetSource's target class.
	 * @see org.springframework.aop.framework.AopProxy#getProxyClass
	 */
	@Override
	public @Nullable Class<?> getObjectType() {
		synchronized (this) {
			if (this.singletonInstance != null) {
				return this.singletonInstance.getClass();
			}
		}
		try {
			// This might be incomplete since it potentially misses introduced interfaces
			// from Advisors that will be lazily retrieved via setInterceptorNames.
			return createAopProxy().getProxyClass(this.proxyClassLoader);
		}
		catch (AopConfigException ex) {
			if (getTargetClass() == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to determine early proxy class: " + ex.getMessage());
				}
				return null;
			}
			else {
				throw ex;
			}
		}
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}


	/**
	 * Return the singleton instance of this class's proxy object,
	 * lazily creating it if it hasn't been created already.
	 * @return the shared singleton proxy
	 */
	private synchronized Object getSingletonInstance() {
		if (this.singletonInstance == null) {
			this.targetSource = freshTargetSource();
			if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
				// Rely on AOP infrastructure to tell us what interfaces to proxy.
				Class<?> targetClass = getTargetClass();
				if (targetClass == null) {
					throw new FactoryBeanNotInitializedException("Cannot determine target class for proxy");
				}
				setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
			// Initialize the shared singleton instance.
			super.setFrozen(this.freezeProxy);
			this.singletonInstance = getProxy(createAopProxy());
		}
		return this.singletonInstance;
	}

	/**
	 * Create a new prototype instance of this class's created proxy object,
	 * backed by an independent AdvisedSupport configuration.
	 * @return a totally independent proxy, whose advice we may manipulate in isolation
	 */
	private synchronized Object newPrototypeInstance() {
		// In the case of a prototype, we need to give the proxy
		// an independent instance of the configuration.
		// In this case, no proxy will have an instance of this object's configuration,
		// but will have an independent copy.
		ProxyCreatorSupport copy = new ProxyCreatorSupport(getAopProxyFactory());

		// The copy needs a fresh advisor chain, and a fresh TargetSource.
		TargetSource targetSource = freshTargetSource();
		copy.copyConfigurationFrom(this, targetSource, freshAdvisorChain());
		if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
			// Rely on AOP infrastructure to tell us what interfaces to proxy.
			Class<?> targetClass = targetSource.getTargetClass();
			if (targetClass != null) {
				copy.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
		}
		copy.setFrozen(this.freezeProxy);

		return getProxy(copy.createAopProxy());
	}

	/**
	 * Return the proxy object to expose.
	 * <p>The default implementation uses a {@code getProxy} call with
	 * the factory's bean class loader. Can be overridden to specify a
	 * custom class loader.
	 * @param aopProxy the prepared AopProxy instance to get the proxy from
	 * @return the proxy object to expose
	 * @see AopProxy#getProxy(ClassLoader)
	 */
	protected Object getProxy(AopProxy aopProxy) {
		return aopProxy.getProxy(this.proxyClassLoader);
	}

	/**
	 * Check the interceptorNames list whether it contains a target name as final element.
	 * If found, remove the final name from the list and set it as targetName.
	 */
	private void checkInterceptorNames() {
		if (!ObjectUtils.isEmpty(this.interceptorNames)) {
			String finalName = this.interceptorNames[this.interceptorNames.length - 1];
			if (this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				// The last name in the chain may be an Advisor/Advice or a target/TargetSource.
				// Unfortunately we don't know; we must look at type of the bean.
				if (!finalName.endsWith(GLOBAL_SUFFIX) && !isNamedBeanAnAdvisorOrAdvice(finalName)) {
					// The target isn't an interceptor.
					this.targetName = finalName;
					if (logger.isDebugEnabled()) {
						logger.debug("Bean with name '" + finalName + "' concluding interceptor chain " +
								"is not an advisor class: treating it as a target or TargetSource");
					}
					this.interceptorNames = Arrays.copyOf(this.interceptorNames, this.interceptorNames.length - 1);
				}
			}
		}
	}

	/**
	 * Look at bean factory metadata to work out whether this bean name,
	 * which concludes the interceptorNames list, is an Advisor or Advice,
	 * or may be a target.
	 * @param beanName bean name to check
	 * @return {@code true} if it's an Advisor or Advice
	 */
	private boolean isNamedBeanAnAdvisorOrAdvice(String beanName) {
		Assert.state(this.beanFactory != null, "No BeanFactory set");
		Class<?> namedBeanClass = this.beanFactory.getType(beanName);
		if (namedBeanClass != null) {
			return (Advisor.class.isAssignableFrom(namedBeanClass) || Advice.class.isAssignableFrom(namedBeanClass));
		}
		// Treat it as a target bean if we can't tell.
		if (logger.isDebugEnabled()) {
			logger.debug("Could not determine type of bean with name '" + beanName +
					"' - assuming it is neither an Advisor nor an Advice");
		}
		return false;
	}

	/**
	 * Create the advisor (interceptor) chain. Advisors that are sourced
	 * from a BeanFactory will be refreshed each time a new prototype instance
	 * is added. Interceptors added programmatically through the factory API
	 * are unaffected by such changes.
	 */
	private synchronized void initializeAdvisorChain() throws AopConfigException, BeansException {
		if (!this.advisorChainInitialized && !ObjectUtils.isEmpty(this.interceptorNames)) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve interceptor names " + Arrays.toString(this.interceptorNames));
			}

			// Globals can't be last unless we specified a targetSource using the property...
			if (this.interceptorNames[this.interceptorNames.length - 1].endsWith(GLOBAL_SUFFIX) &&
					this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				throw new AopConfigException("Target required after globals");
			}

			// Materialize interceptor chain from bean names.
			for (String name : this.interceptorNames) {
				if (name.endsWith(GLOBAL_SUFFIX)) {
					if (!(this.beanFactory instanceof ListableBeanFactory lbf)) {
						throw new AopConfigException(
								"Can only use global advisors or interceptors with a ListableBeanFactory");
					}
					addGlobalAdvisors(lbf, name.substring(0, name.length() - GLOBAL_SUFFIX.length()));
				}

				else {
					// If we get here, we need to add a named interceptor.
					// We must check if it's a singleton or prototype.
					Object advice;
					if (this.singleton || this.beanFactory.isSingleton(name)) {
						// Add the real Advisor/Advice to the chain.
						advice = this.beanFactory.getBean(name);
					}
					else {
						// It's a prototype Advice or Advisor: replace with a prototype.
						// Avoid unnecessary creation of prototype bean just for advisor chain initialization.
						advice = new PrototypePlaceholderAdvisor(name);
					}
					addAdvisorOnChainCreation(advice);
				}
			}

			this.advisorChainInitialized = true;
		}
	}


	/**
	 * Return an independent advisor chain.
	 * We need to do this every time a new prototype instance is returned,
	 * to return distinct instances of prototype Advisors and Advices.
	 */
	private List<Advisor> freshAdvisorChain() {
		Advisor[] advisors = getAdvisors();
		List<Advisor> freshAdvisors = new ArrayList<>(advisors.length);
		for (Advisor advisor : advisors) {
			if (advisor instanceof PrototypePlaceholderAdvisor ppa) {
				if (logger.isDebugEnabled()) {
					logger.debug("Refreshing bean named '" + ppa.getBeanName() + "'");
				}
				// Replace the placeholder with a fresh prototype instance resulting from a getBean lookup
				if (this.beanFactory == null) {
					throw new IllegalStateException("No BeanFactory available anymore (probably due to " +
							"serialization) - cannot resolve prototype advisor '" + ppa.getBeanName() + "'");
				}
				Object bean = this.beanFactory.getBean(ppa.getBeanName());
				Advisor refreshedAdvisor = namedBeanToAdvisor(bean);
				freshAdvisors.add(refreshedAdvisor);
			}
			else {
				// Add the shared instance.
				freshAdvisors.add(advisor);
			}
		}
		return freshAdvisors;
	}

	/**
	 * Add all global interceptors and pointcuts.
	 */
	private void addGlobalAdvisors(ListableBeanFactory beanFactory, String prefix) {
		String[] globalAdvisorNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Advisor.class);
		String[] globalInterceptorNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Interceptor.class);
		if (globalAdvisorNames.length > 0 || globalInterceptorNames.length > 0) {
			List<Object> beans = new ArrayList<>(globalAdvisorNames.length + globalInterceptorNames.length);
			for (String name : globalAdvisorNames) {
				if (name.startsWith(prefix)) {
					beans.add(beanFactory.getBean(name));
				}
			}
			for (String name : globalInterceptorNames) {
				if (name.startsWith(prefix)) {
					beans.add(beanFactory.getBean(name));
				}
			}
			AnnotationAwareOrderComparator.sort(beans);
			for (Object bean : beans) {
				addAdvisorOnChainCreation(bean);
			}
		}
	}

	/**
	 * Invoked when advice chain is created.
	 * <p>Add the given advice, advisor or object to the interceptor list.
	 * Because of these three possibilities, we can't type the signature
	 * more strongly.
	 * @param next advice, advisor or target object
	 */
	private void addAdvisorOnChainCreation(Object next) {
		// We need to convert to an Advisor if necessary so that our source reference
		// matches what we find from superclass interceptors.
		addAdvisor(namedBeanToAdvisor(next));
	}

	/**
	 * Return a TargetSource to use when creating a proxy. If the target was not
	 * specified at the end of the interceptorNames list, the TargetSource will be
	 * this class's TargetSource member. Otherwise, we get the target bean and wrap
	 * it in a TargetSource if necessary.
	 */
	private TargetSource freshTargetSource() {
		if (this.targetName == null) {
			// Not refreshing target: bean name not specified in 'interceptorNames'
			return this.targetSource;
		}
		else {
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve target with name '" + this.targetName + "'");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Refreshing target with name '" + this.targetName + "'");
			}
			Object target = this.beanFactory.getBean(this.targetName);
			return (target instanceof TargetSource targetSource ? targetSource : new SingletonTargetSource(target));
		}
	}

	/**
	 * Convert the following object sourced from calling getBean() on a name in the
	 * interceptorNames array to an Advisor or TargetSource.
	 */
	private Advisor namedBeanToAdvisor(Object next) {
		try {
			return this.advisorAdapterRegistry.wrap(next);
		}
		catch (UnknownAdviceTypeException ex) {
			// We expected this to be an Advisor or Advice,
			// but it wasn't. This is a configuration error.
			throw new AopConfigException("Unknown advisor type " + next.getClass() +
					"; can only include Advisor or Advice type beans in interceptorNames chain " +
					"except for last entry which may also be target instance or TargetSource", ex);
		}
	}

	/**
	 * Blow away and recache singleton on an advice change.
	 */
	@Override
	protected void adviceChanged() {
		super.adviceChanged();
		if (this.singleton) {
			logger.debug("Advice has changed; re-caching singleton instance");
			synchronized (this) {
				this.singletonInstance = null;
			}
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
	}


	/**
	 * Used in the interceptor chain where we need to replace a bean with a prototype
	 * on creating a proxy.
	 */
	private static class PrototypePlaceholderAdvisor implements Advisor, Serializable {

		private final String beanName;

		private final String message;

		public PrototypePlaceholderAdvisor(String beanName) {
			this.beanName = beanName;
			this.message = "Placeholder for prototype Advisor/Advice with bean name '" + beanName + "'";
		}

		public String getBeanName() {
			return this.beanName;
		}

		@Override
		public Advice getAdvice() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public String toString() {
			return this.message;
		}
	}

}
