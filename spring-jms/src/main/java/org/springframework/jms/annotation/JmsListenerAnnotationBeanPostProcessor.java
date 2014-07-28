/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jms.annotation;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jms.config.DefaultJmsHandlerMethodFactory;
import org.springframework.jms.config.JmsHandlerMethodFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Bean post-processor that registers methods annotated with {@link JmsListener}
 * to be invoked by a JMS message listener container created under the cover
 * by a {@link org.springframework.jms.config.JmsListenerContainerFactory} according
 * to the parameters of the annotation.
 *
 * <p>Annotated methods can use flexible arguments as defined by {@link JmsListener}.
 *
 * <p>This post-processor is automatically registered by Spring's
 * {@code <jms:annotation-driven>} XML element, and also by the {@link EnableJms}
 * annotation.
 *
 * <p>Auto-detect any {@link JmsListenerConfigurer} instances in the container,
 * allowing for customization of the registry to be used, the default container
 * factory or for fine-grained control over endpoints registration. See
 * {@link EnableJms} Javadoc for complete usage details.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see JmsListener
 * @see EnableJms
 * @see JmsListenerConfigurer
 * @see JmsListenerEndpointRegistrar
 * @see JmsListenerEndpointRegistry
 * @see org.springframework.jms.config.AbstractJmsListenerEndpoint
 * @see MethodJmsListenerEndpoint
 */
public class JmsListenerAnnotationBeanPostProcessor
		implements BeanPostProcessor, Ordered, BeanFactoryAware, SmartInitializingSingleton {

	/**
	 * The bean name of the default {@link JmsListenerContainerFactory}.
	 */
	static final String DEFAULT_JMS_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "jmsListenerContainerFactory";


	private JmsListenerEndpointRegistry endpointRegistry;

	private String containerFactoryBeanName = DEFAULT_JMS_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

	private BeanFactory beanFactory;

	private final JmsHandlerMethodFactoryAdapter jmsHandlerMethodFactory = new JmsHandlerMethodFactoryAdapter();

	private final JmsListenerEndpointRegistrar registrar = new JmsListenerEndpointRegistrar();

	private final AtomicInteger counter = new AtomicInteger();


	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	/**
	 * Set the {@link JmsListenerEndpointRegistry} that will hold the created
	 * endpoint and manage the lifecycle of the related listener container.
	 */
	public void setEndpointRegistry(JmsListenerEndpointRegistry endpointRegistry) {
		this.endpointRegistry = endpointRegistry;
	}

	/**
	 * Set the name of the {@link JmsListenerContainerFactory} to use by default.
	 * <p>If none is specified, "jmsListenerContainerFactory" is assumed to be defined.
	 */
	public void setContainerFactoryBeanName(String containerFactoryBeanName) {
		this.containerFactoryBeanName = containerFactoryBeanName;
	}

	/**
	 * Set the {@link JmsHandlerMethodFactory} to use to configure the message
	 * listener responsible to serve an endpoint detected by this processor.
	 * <p>By default, {@link DefaultJmsHandlerMethodFactory} is used and it
	 * can be configured further to support additional method arguments
	 * or to customize conversion and validation support. See
	 * {@link DefaultJmsHandlerMethodFactory} Javadoc for more details.
	 */
	public void setJmsHandlerMethodFactory(JmsHandlerMethodFactory jmsHandlerMethodFactory) {
		this.jmsHandlerMethodFactory.setJmsHandlerMethodFactory(jmsHandlerMethodFactory);
	}

	/**
	 * Making a {@link BeanFactory} available is optional; if not set,
	 * {@link JmsListenerConfigurer} beans won't get autodetected and an
	 * {@link #setEndpointRegistry endpoint registry} has to be explicitly configured.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void afterSingletonsInstantiated() {
		this.registrar.setBeanFactory(this.beanFactory);

		if (this.beanFactory instanceof ListableBeanFactory) {
			Map<String, JmsListenerConfigurer> instances =
					((ListableBeanFactory) this.beanFactory).getBeansOfType(JmsListenerConfigurer.class);
			for (JmsListenerConfigurer configurer : instances.values()) {
				configurer.configureJmsListeners(this.registrar);
			}
		}

		if (this.registrar.getEndpointRegistry() == null) {
			if (this.endpointRegistry == null) {
				Assert.state(this.beanFactory != null, "BeanFactory must be set to find endpoint registry by bean name");
				this.endpointRegistry = this.beanFactory.getBean(
						AnnotationConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME, JmsListenerEndpointRegistry.class);
			}
			this.registrar.setEndpointRegistry(this.endpointRegistry);
		}

		if (this.containerFactoryBeanName != null) {
			this.registrar.setContainerFactoryBeanName(this.containerFactoryBeanName);
		}

		// Set the custom handler method factory once resolved by the configurer
		JmsHandlerMethodFactory handlerMethodFactory = this.registrar.getJmsHandlerMethodFactory();
		if (handlerMethodFactory != null) {
			this.jmsHandlerMethodFactory.setJmsHandlerMethodFactory(handlerMethodFactory);
		}

		// Actually register all listeners
		this.registrar.afterPropertiesSet();
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				JmsListener jmsListener = AnnotationUtils.getAnnotation(method, JmsListener.class);
				if (jmsListener != null) {
					processJmsListener(jmsListener, method, bean);
				}
			}
		});
		return bean;
	}

	protected void processJmsListener(JmsListener jmsListener, Method method, Object bean) {
		if (AopUtils.isJdkDynamicProxy(bean)) {
			try {
				// Found a @JmsListener method on the target class for this JDK proxy ->
				// is it also present on the proxy itself?
				method = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
			}
			catch (SecurityException ex) {
				ReflectionUtils.handleReflectionException(ex);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException(String.format(
						"@JmsListener method '%s' found on bean target class '%s', " +
						"but not found in any interface(s) for bean JDK proxy. Either " +
						"pull the method up to an interface or switch to subclass (CGLIB) " +
						"proxies by setting proxy-target-class/proxyTargetClass " +
						"attribute to 'true'", method.getName(), method.getDeclaringClass().getSimpleName()));
			}
		}

		MethodJmsListenerEndpoint endpoint = new MethodJmsListenerEndpoint();
		endpoint.setBean(bean);
		endpoint.setMethod(method);
		endpoint.setJmsHandlerMethodFactory(this.jmsHandlerMethodFactory);
		endpoint.setId(getEndpointId(jmsListener));
		endpoint.setDestination(jmsListener.destination());
		if (StringUtils.hasText(jmsListener.selector())) {
			endpoint.setSelector(jmsListener.selector());
		}
		if (StringUtils.hasText(jmsListener.subscription())) {
			endpoint.setSubscription(jmsListener.subscription());
		}
		if (StringUtils.hasText(jmsListener.concurrency())) {
			endpoint.setConcurrency(jmsListener.concurrency());
		}

		JmsListenerContainerFactory<?> factory = null;
		String containerFactoryBeanName = jmsListener.containerFactory();
		if (StringUtils.hasText(containerFactoryBeanName)) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
			try {
				factory = this.beanFactory.getBean(containerFactoryBeanName, JmsListenerContainerFactory.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				throw new BeanInitializationException("Could not register jms listener endpoint on [" +
						method + "], no " + JmsListenerContainerFactory.class.getSimpleName() + " with id '" +
						containerFactoryBeanName + "' was found in the application context", ex);
			}
		}

		this.registrar.registerEndpoint(endpoint, factory);
	}

	private String getEndpointId(JmsListener jmsListener) {
		if (StringUtils.hasText(jmsListener.id())) {
			return jmsListener.id();
		}
		else {
			return "org.springframework.jms.JmsListenerEndpointContainer#" + counter.getAndIncrement();
		}
	}


	/**
	 * An {@link JmsHandlerMethodFactory} adapter that offers a configurable underlying
	 * instance to use. Useful if the factory to use is determined once the endpoints
	 * have been registered but not created yet.
	 * @see JmsListenerEndpointRegistrar#setJmsHandlerMethodFactory(JmsHandlerMethodFactory)
	 */
	private class JmsHandlerMethodFactoryAdapter implements JmsHandlerMethodFactory {

		private JmsHandlerMethodFactory jmsHandlerMethodFactory;

		public void setJmsHandlerMethodFactory(JmsHandlerMethodFactory jmsHandlerMethodFactory) {
			this.jmsHandlerMethodFactory = jmsHandlerMethodFactory;
		}

		@Override
		public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
			return getJmsHandlerMethodFactory().createInvocableHandlerMethod(bean, method);
		}

		private JmsHandlerMethodFactory getJmsHandlerMethodFactory() {
			if (this.jmsHandlerMethodFactory == null) {
				this.jmsHandlerMethodFactory = createDefaultJmsHandlerMethodFactory();
			}
			return this.jmsHandlerMethodFactory;
		}

		private JmsHandlerMethodFactory createDefaultJmsHandlerMethodFactory() {
			DefaultJmsHandlerMethodFactory defaultFactory = new DefaultJmsHandlerMethodFactory();
			defaultFactory.setBeanFactory(beanFactory);
			defaultFactory.afterPropertiesSet();
			return defaultFactory;
		}
	}

}
