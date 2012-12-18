/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework.autoproxy;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that wraps each eligible bean with an AOP proxy, delegating to specified interceptors
 * before invoking the bean itself.
 *
 * <p>This class distinguishes between "common" interceptors: shared for all proxies it
 * creates, and "specific" interceptors: unique per bean instance. There need not
 * be any common interceptors. If there are, they are set using the interceptorNames
 * property. As with ProxyFactoryBean, interceptors names in the current factory
 * are used rather than bean references to allow correct handling of prototype
 * advisors and interceptors: for example, to support stateful mixins.
 * Any advice type is supported for "interceptorNames" entries.
 *
 * <p>Such auto-proxying is particularly useful if there's a large number of beans that
 * need to be wrapped with similar proxies, i.e. delegating to the same interceptors.
 * Instead of x repetitive proxy definitions for x target beans, you can register
 * one single such post processor with the bean factory to achieve the same effect.
 *
 * <p>Subclasses can apply any strategy to decide if a bean is to be proxied,
 * e.g. by type, by name, by definition details, etc. They can also return
 * additional interceptors that should just be applied to the specific bean
 * instance. The default concrete implementation is BeanNameAutoProxyCreator,
 * identifying the beans to be proxied via a list of bean names.
 *
 * <p>Any number of {@link TargetSourceCreator} implementations can be used to create
 * a custom target source - for example, to pool prototype objects. Auto-proxying will
 * occur even if there is no advice, as long as a TargetSourceCreator specifies a custom
 * {@link org.springframework.aop.TargetSource}. If there are no TargetSourceCreators set,
 * or if none matches, a {@link org.springframework.aop.target.SingletonTargetSource}
 * will be used by default to wrap the target bean instance.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 13.10.2003
 * @see #setInterceptorNames
 * @see #getAdvicesAndAdvisorsForBean
 * @see BeanNameAutoProxyCreator
 * @see DefaultAdvisorAutoProxyCreator
 */
public abstract class AbstractAutoProxyCreator extends ProxyConfig
		implements SmartInstantiationAwareBeanPostProcessor, BeanClassLoaderAware, BeanFactoryAware,
		Ordered, AopInfrastructureBean {

	/**
	 * Convenience constant for subclasses: Return value for "do not proxy".
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	protected static final Object[] DO_NOT_PROXY = null;

	/**
	 * Convenience constant for subclasses: Return value for
	 * "proxy without additional interceptors, just the common ones".
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Default value is same as non-ordered */
	private int order = Ordered.LOWEST_PRECEDENCE;

	/** Default is global AdvisorAdapterRegistry */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	/**
	 * Indicates whether or not the proxy should be frozen. Overridden from super
	 * to prevent the configuration from becoming frozen too early.
	 */
	private boolean freezeProxy = false;

	/** Default is no common interceptors */
	private String[] interceptorNames = new String[0];

	private boolean applyCommonInterceptorsFirst = true;

	private TargetSourceCreator[] customTargetSourceCreators;

	private ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private boolean classLoaderConfigured = false;

	private BeanFactory beanFactory;

	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<Object, Boolean>(64);

	// using a ConcurrentHashMap as a Set
	private final Map<String, Boolean> targetSourcedBeans = new ConcurrentHashMap<String, Boolean>(16);

	// using a ConcurrentHashMap as a Set
	private final Map<Object, Boolean> earlyProxyReferences = new ConcurrentHashMap<Object, Boolean>(16);

	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<Object, Class<?>>(16);


	/**
	 * Set the ordering which will apply to this class's implementation
	 * of Ordered, used when applying multiple BeanPostProcessors.
	 * <p>Default value is {@code Integer.MAX_VALUE}, meaning that it's non-ordered.
	 * @param order ordering value
	 */
	public final void setOrder(int order) {
	  this.order = order;
	}

	public final int getOrder() {
	  return this.order;
	}

	/**
	 * Set whether or not the proxy should be frozen, preventing advice
	 * from being added to it once it is created.
	 * <p>Overridden from the super class to prevent the proxy configuration
	 * from being frozen before the proxy is created.
	 */
	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	@Override
	public boolean isFrozen() {
		return this.freezeProxy;
	}

	/**
	 * Specify the AdvisorAdapterRegistry to use.
	 * Default is the global AdvisorAdapterRegistry.
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * Set custom TargetSourceCreators to be applied in this order.
	 * If the list is empty, or they all return null, a SingletonTargetSource
	 * will be created for each bean.
	 * <p>Note that TargetSourceCreators will kick in even for target beans
	 * where no advices or advisors have been found. If a TargetSourceCreator
	 * returns a TargetSource for a specific bean, that bean will be proxied
	 * in any case.
	 * <p>TargetSourceCreators can only be invoked if this post processor is used
	 * in a BeanFactory, and its BeanFactoryAware callback is used.
	 * @param targetSourceCreators list of TargetSourceCreator.
	 * Ordering is significant: The TargetSource returned from the first matching
	 * TargetSourceCreator (that is, the first that returns non-null) will be used.
	 */
	public void setCustomTargetSourceCreators(TargetSourceCreator[] targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * Set the common interceptors. These must be bean names in the current factory.
	 * They can be of any advice or advisor type Spring supports.
	 * <p>If this property isn't set, there will be zero common interceptors.
	 * This is perfectly valid, if "specific" interceptors such as matching
	 * Advisors are all we want.
	 */
	public void setInterceptorNames(String[] interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * Set whether the common interceptors should be applied before bean-specific ones.
	 * Default is "true"; else, bean-specific interceptors will get applied first.
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	/**
	 * Set the ClassLoader to generate the proxy class in.
	 * <p>Default is the bean ClassLoader, i.e. the ClassLoader used by the
	 * containing BeanFactory for loading all bean classes. This can be
	 * overridden here for specific proxies.
	 */
	public void setProxyClassLoader(ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the owning BeanFactory.
	 * May be {@code null}, as this object doesn't need to belong to a bean factory.
	 */
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		Object cacheKey = getCacheKey(beanClass, beanName);
		return this.proxyTypes.get(cacheKey);
	}

	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		this.earlyProxyReferences.put(cacheKey, Boolean.TRUE);
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		Object cacheKey = getCacheKey(beanClass, beanName);

		if (beanName == null || !this.targetSourcedBeans.containsKey(beanName)) {
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		// Create proxy here if we have a custom TargetSource.
		// Suppresses unnecessary default instantiation of the target bean:
		// The TargetSource will handle target instances in a custom fashion.
		if (beanName != null) {
			TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
			if (targetSource != null) {
				this.targetSourcedBeans.put(beanName, Boolean.TRUE);
				Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
				Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
				this.proxyTypes.put(cacheKey, proxy.getClass());
				return proxy;
			}
		}

		return null;
	}

	public boolean postProcessAfterInstantiation(Object bean, String beanName) {
		return true;
	}

	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		return pvs;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * Create a proxy with the configured interceptors if the bean is
	 * identified as one to proxy by the subclass.
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (!this.earlyProxyReferences.containsKey(cacheKey)) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}


	/**
	 * Build a cache key for the given bean class and bean name.
	 * @param beanClass the bean class
	 * @param beanName the bean name
	 * @return the cache key for the given class and name
	 */
	protected Object getCacheKey(Class<?> beanClass, String beanName) {
		return beanClass.getName() + "_" + beanName;
	}

	/**
	 * Wrap the given bean if necessary, i.e. if it is eligible for being proxied.
	 * @param bean the raw bean instance
	 * @param beanName the name of the bean
	 * @param cacheKey the cache key for metadata access
	 * @return a proxy wrapping the bean, or the raw bean instance as-is
	 */
	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		if (beanName != null && this.targetSourcedBeans.containsKey(beanName)) {
			return bean;
		}
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// Create proxy if we have advice.
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			Object proxy = createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}

	/**
	 * Return whether the given bean class represents an infrastructure class
	 * that should never be proxied.
	 * <p>The default implementation considers Advices, Advisors and
	 * AopInfrastructureBeans as infrastructure classes.
	 * @param beanClass the class of the bean
	 * @return whether the bean represents an infrastructure class
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.Advisor
	 * @see org.springframework.aop.framework.AopInfrastructureBean
	 * @see #shouldSkip
	 */
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}

	/**
	 * Subclasses should override this method to return {@code true} if the
	 * given bean should not be considered for auto-proxying by this post-processor.
	 * <p>Sometimes we need to be able to avoid this happening if it will lead to
	 * a circular reference. This implementation returns {@code false}.
	 * @param beanClass the class of the bean
	 * @param beanName the name of the bean
	 * @return whether to skip the given bean
	 */
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		return false;
	}

	/**
	 * Create a target source for bean instances. Uses any TargetSourceCreators if set.
	 * Returns {@code null} if no custom TargetSource should be used.
	 * <p>This implementation uses the "customTargetSourceCreators" property.
	 * Subclasses can override this method to use a different mechanism.
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName the name of the bean
	 * @return a TargetSource for this bean
	 * @see #setCustomTargetSourceCreators
	 */
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		// We can't create fancy target sources for directly registered singletons.
		if (this.customTargetSourceCreators != null &&
				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
				if (ts != null) {
					// Found a matching TargetSource.
					if (logger.isDebugEnabled()) {
						logger.debug("TargetSourceCreator [" + tsc +
								" found custom TargetSource for bean with name '" + beanName + "'");
					}
					return ts;
				}
			}
		}

		// No custom TargetSource found.
		return null;
	}

	/**
	 * Create an AOP proxy for the given bean.
	 * @param beanClass the class of the bean
	 * @param beanName the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 * specific to this bean (may be empty, but not null)
	 * @param targetSource the TargetSource for the proxy,
	 * already pre-configured to access the bean
	 * @return the AOP proxy for the bean
	 * @see #buildAdvisors
	 */
	protected Object createProxy(
			Class<?> beanClass, String beanName, Object[] specificInterceptors, TargetSource targetSource) {

		ProxyFactory proxyFactory = new ProxyFactory();
		// Copy our properties (proxyTargetClass etc) inherited from ProxyConfig.
		proxyFactory.copyFrom(this);

		if (!shouldProxyTargetClass(beanClass, beanName)) {
			// Must allow for introductions; can't just set interfaces to
			// the target's interfaces only.
			Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, this.proxyClassLoader);
			for (Class<?> targetInterface : targetInterfaces) {
				proxyFactory.addInterface(targetInterface);
			}
		}

		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		for (Advisor advisor : advisors) {
			proxyFactory.addAdvisor(advisor);
		}

		proxyFactory.setTargetSource(targetSource);
		customizeProxyFactory(proxyFactory);

		proxyFactory.setFrozen(this.freezeProxy);
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}

		return proxyFactory.getProxy(this.proxyClassLoader);
	}

	/**
	 * Determine whether the given bean should be proxied with its target
	 * class rather than its interfaces. Checks the
	 * {@link #setProxyTargetClass "proxyTargetClass" setting} as well as the
	 * {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
	 * of the corresponding bean definition.
	 * @param beanClass the class of the bean
	 * @param beanName the name of the bean
	 * @return whether the given bean should be proxied with its target class
	 * @see AutoProxyUtils#shouldProxyTargetClass
	 */
	protected boolean shouldProxyTargetClass(Class<?> beanClass, String beanName) {
		return (isProxyTargetClass() ||
				(this.beanFactory instanceof ConfigurableListableBeanFactory &&
						AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName)));
	}

	/**
	 * Return whether the Advisors returned by the subclass are pre-filtered
	 * to match the bean's target class already, allowing the ClassFilter check
	 * to be skipped when building advisors chains for AOP invocations.
	 * <p>Default is {@code false}. Subclasses may override this if they
	 * will always return pre-filtered Advisors.
	 * @return whether the Advisors are pre-filtered
	 * @see #getAdvicesAndAdvisorsForBean
	 * @see org.springframework.aop.framework.Advised#setPreFiltered
	 */
	protected boolean advisorsPreFiltered() {
		return false;
	}

	/**
	 * Determine the advisors for the given bean, including the specific interceptors
	 * as well as the common interceptor, all adapted to the Advisor interface.
	 * @param beanName the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 * specific to this bean (may be empty, but not null)
	 * @return the list of Advisors for the given bean
	 */
	protected Advisor[] buildAdvisors(String beanName, Object[] specificInterceptors) {
		// Handle prototypes correctly...
		Advisor[] commonInterceptors = resolveInterceptorNames();

		List<Object> allInterceptors = new ArrayList<Object>();
		if (specificInterceptors != null) {
			allInterceptors.addAll(Arrays.asList(specificInterceptors));
			if (commonInterceptors != null) {
				if (this.applyCommonInterceptorsFirst) {
					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
				}
				else {
					allInterceptors.addAll(Arrays.asList(commonInterceptors));
				}
			}
		}
		if (logger.isDebugEnabled()) {
			int nrOfCommonInterceptors = (commonInterceptors != null ? commonInterceptors.length : 0);
			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
			logger.debug("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
		}

		Advisor[] advisors = new Advisor[allInterceptors.size()];
		for (int i = 0; i < allInterceptors.size(); i++) {
			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
		}
		return advisors;
	}

	/**
	 * Resolves the specified interceptor names to Advisor objects.
	 * @see #setInterceptorNames
	 */
	private Advisor[] resolveInterceptorNames() {
		ConfigurableBeanFactory cbf = (this.beanFactory instanceof ConfigurableBeanFactory) ?
				(ConfigurableBeanFactory) this.beanFactory : null;
		List<Advisor> advisors = new ArrayList<Advisor>();
		for (String beanName : this.interceptorNames) {
			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
				Object next = this.beanFactory.getBean(beanName);
				advisors.add(this.advisorAdapterRegistry.wrap(next));
			}
		}
		return advisors.toArray(new Advisor[advisors.size()]);
	}

	/**
	 * Subclasses may choose to implement this: for example,
	 * to change the interfaces exposed.
	 * <p>The default implementation is empty.
	 * @param proxyFactory ProxyFactory that is already configured with
	 * TargetSource and interfaces and will be used to create the proxy
	 * immediably after this method returns
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}


	/**
	 * Return whether the given bean is to be proxied, what additional
	 * advices (e.g. AOP Alliance interceptors) and advisors to apply.
	 * @param beanClass the class of the bean to advise
	 * @param beanName the name of the bean
	 * @param customTargetSource the TargetSource returned by the
	 * {@link #getCustomTargetSource} method: may be ignored.
	 * Will be {@code null} if no custom target source is in use.
	 * @return an array of additional interceptors for the particular bean;
	 * or an empty array if no additional interceptors but just the common ones;
	 * or {@code null} if no proxy at all, not even with the common interceptors.
	 * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
	 * @throws BeansException in case of errors
	 * @see #DO_NOT_PROXY
	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 */
	protected abstract Object[] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException;

}
