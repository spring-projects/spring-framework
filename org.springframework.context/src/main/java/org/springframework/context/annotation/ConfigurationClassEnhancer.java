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

package org.springframework.context.annotation;

import java.lang.reflect.Method;

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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.SimpleInstantiationStrategy;
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

	private static final Class<?>[] CALLBACK_TYPES = { BeanMethodInterceptor.class,
		DisposableBeanMethodInterceptor.class, NoOp.class };

	private static final CallbackFilter CALLBACK_FILTER = new CallbackFilter() {

		public int accept(Method candidateMethod) {
			// Set up the callback filter to return the index of the BeanMethodInterceptor when
			// handling a @Bean-annotated method; otherwise, return index of the NoOp callback.
			if (BeanAnnotationHelper.isBeanAnnotated(candidateMethod)) {
				return 0;
			}
			if (DisposableBeanMethodInterceptor.isDestroyMethod(candidateMethod)) {
				return 1;
			}
			return 2;
		}
	};

	private static final Callback DISPOSABLE_BEAN_METHOD_INTERCEPTOR = new DisposableBeanMethodInterceptor();


	private final Callback[] callbackInstances;


	/**
	 * Creates a new {@link ConfigurationClassEnhancer} instance.
	 */
	public ConfigurationClassEnhancer(ConfigurableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		// Callback instances must be ordered in the same way as CALLBACK_TYPES and CALLBACK_FILTER
		this.callbackInstances = new Callback[] {
				new BeanMethodInterceptor(beanFactory),
				DISPOSABLE_BEAN_METHOD_INTERCEPTOR,
				NoOp.INSTANCE };
	}

	/**
	 * Loads the specified class and generates a CGLIB subclass of it equipped with
	 * container-aware callbacks capable of respecting scoping and other bean semantics.
	 * @return the enhanced subclass
	 */
	public Class<?> enhance(Class<?> configClass) {
		if (EnhancedConfiguration.class.isAssignableFrom(configClass)) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Ignoring request to enhance %s as it has " +
						"already been enhanced. This usually indicates that more than one " +
						"ConfigurationClassPostProcessor has been registered (e.g. via " +
						"<context:annotation-config>). This is harmless, but you may " +
						"want check your configuration and remove one CCPP if possible",
						configClass.getName()));
			}
			return configClass;
		}
		Class<?> enhancedClass = createClass(newEnhancer(configClass));
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Successfully enhanced %s; enhanced class name is: %s",
					configClass.getName(), enhancedClass.getName()));
		}
		return enhancedClass;
	}

	/**
	 * Marker interface to be implemented by all @Configuration CGLIB subclasses.
	 * Facilitates idempotent behavior for {@link ConfigurationClassEnhancer#enhance(Class)}
	 * through checking to see if candidate classes are already assignable to it, e.g.
	 * have already been enhanced.
	 * <p>Also extends {@link DisposableBean}, as all enhanced
	 * {@code @Configuration} classes must de-register static CGLIB callbacks on
	 * destruction, which is handled by the (private) {@code DisposableBeanMethodInterceptor}.
	 * <p>Note that this interface is intended for framework-internal use only, however
	 * must remain public in order to allow access to subclasses generated from other
	 * packages (i.e. user code).
	 */
	public interface EnhancedConfiguration extends DisposableBean {
	}

	/**
	 * Creates a new CGLIB {@link Enhancer} instance.
	 */
	private Enhancer newEnhancer(Class<?> superclass) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(superclass);
		enhancer.setInterfaces(new Class[] {EnhancedConfiguration.class});
		enhancer.setUseFactory(false);
		enhancer.setCallbackFilter(CALLBACK_FILTER);
		enhancer.setCallbackTypes(CALLBACK_TYPES);
		return enhancer;
	}

	/**
	 * Uses enhancer to generate a subclass of superclass, ensuring that
	 * {@link #callbackInstances} are registered for the new subclass.
	 */
	private Class<?> createClass(Enhancer enhancer) {
		Class<?> subclass = enhancer.createClass();
		// registering callbacks statically (as opposed to threadlocal) is critical for usage in an OSGi env (SPR-5932)
		Enhancer.registerStaticCallbacks(subclass, this.callbackInstances);
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
	 * Intercepts the invocation of any {@link DisposableBean#destroy()} on @Configuration
	 * class instances for the purpose of de-registering CGLIB callbacks. This helps avoid
	 * garbage collection issues See SPR-7901.
	 * @see EnhancedConfiguration
	 */
	private static class DisposableBeanMethodInterceptor implements MethodInterceptor {

		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			Enhancer.registerStaticCallbacks(obj.getClass(), null);
			// does the actual (non-CGLIB) superclass actually implement DisposableBean?
			// if so, call its dispose() method. If not, just exit.
			if (DisposableBean.class.isAssignableFrom(obj.getClass().getSuperclass())) {
				return proxy.invokeSuper(obj, args);
			}
			return null;
		}

		public static boolean isDestroyMethod(Method candidateMethod) {
			return candidateMethod.getName().equals("destroy") &&
				candidateMethod.getParameterTypes().length == 0 &&
				DisposableBean.class.isAssignableFrom(candidateMethod.getDeclaringClass());
		}
	}


	/**
	 * Intercepts the invocation of any {@link Bean}-annotated methods in order to ensure proper
	 * handling of bean semantics such as scoping and AOP proxying.
	 * @see Bean
	 * @see ConfigurationClassEnhancer
	 */
	private static class BeanMethodInterceptor implements MethodInterceptor {

		private static final Class<?>[] CALLBACK_TYPES = {
			GetObjectMethodInterceptor.class, NoOp.class };

		private static final CallbackFilter CALLBACK_FITLER = new CallbackFilter() {
			public int accept(Method method) {
				return method.getName().equals("getObject") ? 0 : 1;
			}
		};


		private final ConfigurableBeanFactory beanFactory;


		public BeanMethodInterceptor(ConfigurableBeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		/**
		 * Enhance a {@link Bean @Bean} method to check the supplied BeanFactory for the
		 * existence of this bean object.
		 *
		 * @throws Throwable as a catch-all for any exception that may be thrown when
		 * invoking the super implementation of the proxied method i.e., the actual
		 * {@code @Bean} method.
		 */
		public Object intercept(Object enhancedConfigInstance, Method beanMethod, Object[] beanMethodArgs,
					MethodProxy cglibMethodProxy) throws Throwable {

			String beanName = BeanAnnotationHelper.determineBeanNameFor(beanMethod);

			// determine whether this bean is a scoped-proxy
			Scope scope = AnnotationUtils.findAnnotation(beanMethod, Scope.class);
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
				}
				else {
					// it is a candidate FactoryBean - go ahead with enhancement
					return enhanceFactoryBean(factoryBean.getClass(), beanName);
				}
			}

			boolean factoryIsCaller = beanMethod.equals(SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod());
			boolean factoryAlreadyContainsSingleton = this.beanFactory.containsSingleton(beanName);
			if (factoryIsCaller && !factoryAlreadyContainsSingleton) {
				// the factory is calling the bean method in order to instantiate and register the bean
				// (i.e. via a getBean() call) -> invoke the super implementation of the method to actually
				// create the bean instance.
				if (BeanFactoryPostProcessor.class.isAssignableFrom(beanMethod.getReturnType())) {
					logger.warn(String.format("@Bean method %s.%s is non-static and returns an object " +
							"assignable to Spring's BeanFactoryPostProcessor interface. This will " +
							"result in a failure to process annotations such as @Autowired, " +
							"@Resource and @PostConstruct within the method's declaring " +
							"@Configuration class. Add the 'static' modifier to this method to avoid " +
							"these container lifecycle issues; see @Bean Javadoc for complete details",
							beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName()));
				}
				return cglibMethodProxy.invokeSuper(enhancedConfigInstance, beanMethodArgs);
			}
			else {
				// the user (i.e. not the factory) is requesting this bean through a
				// call to the bean method, direct or indirect. The bean may have already been
				// marked as 'in creation' in certain autowiring scenarios; if so, temporarily
				// set the in-creation status to false in order to avoid an exception.
				boolean alreadyInCreation = this.beanFactory.isCurrentlyInCreation(beanName);
				try {
					if (alreadyInCreation) {
						this.beanFactory.setCurrentlyInCreation(beanName, false);
					}
					return this.beanFactory.getBean(beanName);
				} finally {
					if (alreadyInCreation) {
						this.beanFactory.setCurrentlyInCreation(beanName, true);
					}
				}
			}

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
			boolean containsBean = this.beanFactory.containsBean(beanName);
			boolean currentlyInCreation = this.beanFactory.isCurrentlyInCreation(beanName);
			return (containsBean && !currentlyInCreation);
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
			enhancer.setSuperclass(fbClass);
			enhancer.setUseFactory(false);
			enhancer.setCallbackFilter(CALLBACK_FITLER);
			// Callback instances must be ordered in the same way as CALLBACK_TYPES and CALLBACK_FILTER
			Callback[] callbackInstances = new Callback[] {
					new GetObjectMethodInterceptor(this.beanFactory, beanName),
					NoOp.INSTANCE
			};

			enhancer.setCallbackTypes(CALLBACK_TYPES);
			Class<?> fbSubclass = enhancer.createClass();
			Enhancer.registerCallbacks(fbSubclass, callbackInstances);
			return fbSubclass.newInstance();
		}

	}
}
