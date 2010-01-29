/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.NoOp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * Enhances {@link Configuration} classes by generating a CGLIB subclass capable of
 * interacting with the Spring container to respect bean semantics.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see #enhance
 * @see ConfigurationClassPostProcessor
 */
class ConfigurationClassEnhancer {

	private static final Log logger = LogFactory.getLog(ConfigurationClassEnhancer.class);

	private final List<Callback> callbackInstances = new ArrayList<Callback>();

	private final List<Class<? extends Callback>> callbackTypes = new ArrayList<Class<? extends Callback>>();

	private final CallbackFilter callbackFilter;


	/**
	 * Creates a new {@link ConfigurationClassEnhancer} instance.
	 */
	public ConfigurationClassEnhancer(ConfigurableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");

		this.callbackInstances.add(new BeanMethodInterceptor(beanFactory));
		this.callbackInstances.add(NoOp.INSTANCE);

		for (Callback callback : this.callbackInstances) {
			this.callbackTypes.add(callback.getClass());
		}

		// Set up the callback filter to return the index of the BeanMethodInterceptor when
		// handling a @Bean-annotated method; otherwise, return index of the NoOp callback.
		callbackFilter = new CallbackFilter() {
			public int accept(Method candidateMethod) {
				return (AnnotationUtils.findAnnotation(candidateMethod, Bean.class) != null ? 0 : 1);
			}
		};
	}


	/**
	 * Loads the specified class and generates a CGLIB subclass of it equipped with
	 * container-aware callbacks capable of respecting scoping and other bean semantics.
	 * @return fully-qualified name of the enhanced subclass
	 */
	public Class<?> enhance(Class<?> configClass) {
		Class<?> enhancedClass = createClass(newEnhancer(configClass));
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Successfully enhanced %s; enhanced class name is: %s",
					configClass.getName(), enhancedClass.getName()));
		}
		return enhancedClass;
	}

	/**
	 * Creates a new CGLIB {@link Enhancer} instance.
	 */
	private Enhancer newEnhancer(Class<?> superclass) {
		Enhancer enhancer = new Enhancer();
		// Because callbackFilter and callbackTypes are dynamically populated
		// there's no opportunity for caching. This does not appear to be causing
		// any performance problem.
		enhancer.setUseCache(false);
		enhancer.setSuperclass(superclass);
		enhancer.setUseFactory(false);
		enhancer.setCallbackFilter(this.callbackFilter);
		enhancer.setCallbackTypes(this.callbackTypes.toArray(new Class[this.callbackTypes.size()]));
		return enhancer;
	}

	/**
	 * Uses enhancer to generate a subclass of superclass, ensuring that
	 * {@link #callbackInstances} are registered for the new subclass.
	 */
	private Class<?> createClass(Enhancer enhancer) {
		Class<?> subclass = enhancer.createClass();
		// registering callbacks statically (as opposed to threadlocal) is critical for usage in an OSGi env (SPR-5932)
		Enhancer.registerStaticCallbacks(subclass, this.callbackInstances.toArray(new Callback[this.callbackInstances.size()]));
		return subclass;
	}
	
	
	/**
	 * Intercepts calls to {@link FactoryBean#getObject()}, delegating to calling
	 * {@link BeanFactory#getBean(String)} in order to respect caching / scoping.
	 * @see BeanMethodInterceptor#intercept(Object, Method, Object[], MethodProxy)
	 * @see BeanMethodInterceptor#enhanceFactoryBean(Class, String)
	 */
	private static class GetObjectMethodInterceptor implements MethodInterceptor {
		
		private final ConfigurableBeanFactory beanFactory;
		private final String beanName;

		public GetObjectMethodInterceptor(ConfigurableBeanFactory beanFactory, String beanName) {
			this.beanFactory = beanFactory;
			this.beanName = beanName;
		}

		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			return beanFactory.getBean(beanName);
		}
		
	}


	/**
	 * Intercepts the invocation of any {@link Bean}-annotated methods in order to ensure proper
	 * handling of bean semantics such as scoping and AOP proxying.
	 * @see Bean
	 * @see ConfigurationClassEnhancer
	 */
	private static class BeanMethodInterceptor implements MethodInterceptor {

		private final ConfigurableBeanFactory beanFactory;

		public BeanMethodInterceptor(ConfigurableBeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		/**
		 * Enhance a {@link Bean @Bean} method to check the supplied BeanFactory for the
		 * existence of this bean object.
		 */
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			// by default the bean name is the name of the @Bean-annotated method
			String beanName = method.getName();

			// check to see if the user has explicitly set the bean name
			Bean bean = AnnotationUtils.findAnnotation(method, Bean.class);
			if (bean != null && bean.name().length > 0) {
				beanName = bean.name()[0];
			}

			// determine whether this bean is a scoped-proxy
			Scope scope = AnnotationUtils.findAnnotation(method, Scope.class);
			if (scope != null && scope.proxyMode() != ScopedProxyMode.NO) {
				String scopedBeanName = ScopedProxyCreator.getTargetBeanName(beanName);
				if (this.beanFactory.isCurrentlyInCreation(scopedBeanName)) {
					beanName = scopedBeanName;
				}
			}

			// to handle the case of an inter-bean method reference, we must explicitly check the
			// container for already cached instances
			
			// first, check to see if the requested bean is a FactoryBean. If so, create a subclass
			// proxy that intercepts calls to getObject() and returns any cached bean instance.
			// this ensures that the semantics of calling a FactoryBean from within @Bean methods
			// is the same as that of referring to a FactoryBean within XML. See SPR-6602.
			if (factoryContainsBean('&'+beanName) && factoryContainsBean(beanName)) {
				Object factoryBean = this.beanFactory.getBean('&'+beanName);
			    if (factoryBean instanceof ScopedProxyFactoryBean) {
			    	// pass through - scoped proxy factory beans are a special case and should not
			    	// be further proxied
			    } else {
			    	// it is a candidate FactoryBean - go ahead with enhancement
			    	return enhanceFactoryBean(factoryBean.getClass(), beanName);
			    }
			}
			
			// the bean is not a FactoryBean - check to see if it has been cached
			if (factoryContainsBean(beanName)) {
				// we have an already existing cached instance of this bean -> retrieve it
				return this.beanFactory.getBean(beanName);
			}

			// no cached instance of the bean exists - actually create and return the bean
			return proxy.invokeSuper(obj, args);
		}

		/**
		 * Check the beanFactory to see whether the bean named <var>beanName</var> already
		 * exists. Accounts for the fact that the requested bean may be "in creation", i.e.:
		 * we're in the middle of servicing the initial request for this bean. From an enhanced
		 * factory method's perspective, this means that the bean does not actually yet exist,
		 * and that it is now our job to create it for the first time by executing the logic
		 * in the corresponding factory method.
		 * <p>Said another way, this check repurposes
		 * {@link ConfigurableBeanFactory#isCurrentlyInCreation(String)} to determine whether
		 * the container is calling this method or the user is calling this method.
		 * @param beanName name of bean to check for
		 * @return whether <var>beanName</var> already exists in the factory
		 */
		private boolean factoryContainsBean(String beanName) {
			return (this.beanFactory.containsBean(beanName) && !this.beanFactory.isCurrentlyInCreation(beanName));
		}

		/**
		 * Create a subclass proxy that intercepts calls to getObject(), delegating to the current BeanFactory
		 * instead of creating a new instance. These proxies are created only when calling a FactoryBean from
		 * within a Bean method, allowing for proper scoping semantics even when working against the FactoryBean
		 * instance directly. If a FactoryBean instance is fetched through the container via &-dereferencing,
		 * it will not be proxied. This too is aligned with the way XML configuration works.
		 */
		private Object enhanceFactoryBean(Class<?> fbClass, String beanName) throws InstantiationException, IllegalAccessException {
			Enhancer enhancer = new Enhancer();
			enhancer.setUseCache(false);
			enhancer.setSuperclass(fbClass);
			enhancer.setUseFactory(false);
			enhancer.setCallbackFilter(new CallbackFilter() {
				public int accept(Method method) {
					return method.getName().equals("getObject") ? 0 : 1;
				}
			});
			List<Callback> callbackInstances = new ArrayList<Callback>();
			callbackInstances.add(new GetObjectMethodInterceptor(this.beanFactory, beanName));
			callbackInstances.add(NoOp.INSTANCE);
			
			List<Class<? extends Callback>> callbackTypes = new ArrayList<Class<? extends Callback>>();
			for (Callback callback : callbackInstances) {
				callbackTypes.add(callback.getClass());
			}
			
			enhancer.setCallbackTypes(callbackTypes.toArray(new Class[callbackTypes.size()]));
			Class<?> fbSubclass = enhancer.createClass();
			Enhancer.registerCallbacks(fbSubclass, callbackInstances.toArray(new Callback[callbackInstances.size()]));
			return fbSubclass.newInstance();
		}

	}
}
