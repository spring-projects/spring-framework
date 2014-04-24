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
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jms.config.DefaultJmsHandlerMethodFactory;
import org.springframework.jms.config.JmsHandlerMethodFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Bean post-processor that registers methods annotated with @{@link JmsListener}
 * to be invoked by a JMS message listener container created under the cover
 * by a {@link org.springframework.jms.config.JmsListenerContainerFactory} according
 * to the parameters of the annotation.
 *
 * <p>Annotated methods can use flexible arguments as defined by @{@link JmsListener}.
 *
 * <p>This post-processor is automatically registered by Spring's
 * {@code <jms:annotation-driven>} XML element, and also by the @{@link EnableJms}
 * annotation.
 *
 * <p>Auto-detect any {@link JmsListenerConfigurer} instances in the container,
 * allowing for customization of the registry to be used, the default container
 * factory or for fine-grained control over endpoints registration. See
 * @{@link EnableJms} Javadoc for complete usage details.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see JmsListener
 * @see EnableJms
 * @see JmsListenerConfigurer
 * @see JmsListenerEndpointRegistrar
 * @see JmsListenerEndpointRegistry
 * @see org.springframework.jms.config.AbstractJmsListenerEndpoint
 * @see MethodJmsListenerEndpoint
 * @see MessageListenerFactory
 */
public class JmsListenerAnnotationBeanPostProcessor implements BeanPostProcessor, Ordered,
		ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

	/**
	 * The bean name of the default {@link JmsListenerContainerFactory}
	 */
	static final String DEFAULT_JMS_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "jmsListenerContainerFactory";

	private final AtomicInteger counter = new AtomicInteger();

	private ApplicationContext applicationContext;

	private JmsListenerEndpointRegistry endpointRegistry;

	private String containerFactoryBeanName = DEFAULT_JMS_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

	private final JmsHandlerMethodFactoryAdapter jmsHandlerMethodFactory = new JmsHandlerMethodFactoryAdapter();

	private final JmsListenerEndpointRegistrar registrar = new JmsListenerEndpointRegistrar();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
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
	 * <p/>If none is specified, {@value #DEFAULT_JMS_LISTENER_CONTAINER_FACTORY_BEAN_NAME}
	 * is assumed to be defined.
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

	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
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
		endpoint.setJmsHandlerMethodFactory(jmsHandlerMethodFactory);
		endpoint.setId(getEndpointId(jmsListener));
		endpoint.setDestination(jmsListener.destination());
		if (StringUtils.hasText(jmsListener.selector())) {
			endpoint.setSelector(jmsListener.selector());
		}
		if (StringUtils.hasText(jmsListener.subscription())) {
			endpoint.setSubscription(jmsListener.subscription());
		}

		JmsListenerContainerFactory<?> factory = null;
		String containerFactoryBeanName = jmsListener.containerFactory();
		if (StringUtils.hasText(containerFactoryBeanName)) {
			try {
				factory = applicationContext.getBean(containerFactoryBeanName, JmsListenerContainerFactory.class);
			}
			catch (NoSuchBeanDefinitionException e) {
				throw new BeanInitializationException("Could not register jms listener endpoint on ["
						+ method + "], no " + JmsListenerContainerFactory.class.getSimpleName() + " with id '"
						+ containerFactoryBeanName + "' was found in the application context", e);
			}
		}

		registrar.registerEndpoint(endpoint, factory);

	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext() != this.applicationContext) {
			return;
		}

		Map<String, JmsListenerConfigurer> instances =
				this.applicationContext.getBeansOfType(JmsListenerConfigurer.class);
		for (JmsListenerConfigurer configurer : instances.values()) {
			configurer.configureJmsListeners(registrar);
		}

		registrar.setApplicationContext(this.applicationContext);

		if (registrar.getEndpointRegistry() == null) {
			if (endpointRegistry == null) {
				endpointRegistry = applicationContext
						.getBean(AnnotationConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME,
								JmsListenerEndpointRegistry.class);
			}
			registrar.setEndpointRegistry(endpointRegistry);
		}

		if (this.containerFactoryBeanName != null) {
			registrar.setContainerFactoryBeanName(this.containerFactoryBeanName);
		}


		// Set the custom handler method factory once resolved by the configurer
		JmsHandlerMethodFactory handlerMethodFactory = registrar.getJmsHandlerMethodFactory();
		if (handlerMethodFactory != null) {
			this.jmsHandlerMethodFactory.setJmsHandlerMethodFactory(handlerMethodFactory);
		}

		// Create all the listeners and starts them
		try {
			registrar.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new BeanInitializationException(e.getMessage(), e);
		}
	}

	private String getEndpointId(JmsListener jmsListener) {
		if (StringUtils.hasText(jmsListener.id())) {
			return jmsListener.id();
		}
		else {
			return "org.springframework.jms.JmsListenerEndpointContainer#"
					+ counter.getAndIncrement();
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

		private void setJmsHandlerMethodFactory(JmsHandlerMethodFactory jmsHandlerMethodFactory) {
			this.jmsHandlerMethodFactory = jmsHandlerMethodFactory;
		}

		@Override
		public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
			return getJmsHandlerMethodFactory().createInvocableHandlerMethod(bean, method);
		}

		private JmsHandlerMethodFactory getJmsHandlerMethodFactory() {
			if (jmsHandlerMethodFactory == null) {
				jmsHandlerMethodFactory = createDefaultJmsHandlerMethodFactory();
			}
			return jmsHandlerMethodFactory;
		}

		private JmsHandlerMethodFactory createDefaultJmsHandlerMethodFactory() {
			DefaultJmsHandlerMethodFactory defaultFactory = new DefaultJmsHandlerMethodFactory();
			defaultFactory.setApplicationContext(applicationContext);
			defaultFactory.afterPropertiesSet();
			return defaultFactory;
		}
	}

}
