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

package org.springframework.jms.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

/**
 * Helper bean for registering {@link JmsListenerEndpoint} with
 * a {@link JmsListenerEndpointRegistry}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see org.springframework.jms.annotation.JmsListenerConfigurer
 */
public class JmsListenerEndpointRegistrar implements ApplicationContextAware, InitializingBean {

	private JmsListenerEndpointRegistry endpointRegistry;

	private String containerFactoryBeanName;

	private JmsListenerContainerFactory<?> containerFactory;

	private JmsHandlerMethodFactory jmsHandlerMethodFactory;

	private ApplicationContext applicationContext;

	private final List<JmsListenerEndpointDescriptor> endpointDescriptors
			= new ArrayList<JmsListenerEndpointDescriptor>();

	/**
	 * Set the {@link JmsListenerEndpointRegistry} instance to use.
	 */
	public void setEndpointRegistry(JmsListenerEndpointRegistry endpointRegistry) {
		this.endpointRegistry = endpointRegistry;
	}

	/**
	 * Return the {@link JmsListenerEndpointRegistry} instance for this
	 * registrar, may be {@code null}.
	 */
	public JmsListenerEndpointRegistry getEndpointRegistry() {
		return endpointRegistry;
	}

	/**
	 * Set the bean name of the {@link JmsListenerContainerFactory} to use in
	 * case a {@link JmsListenerEndpoint} is registered with a {@code null}
	 * container factory. Alternatively, the container factory instance can
	 * be registered directly, see {@link #setContainerFactory(JmsListenerContainerFactory)}
	 */
	public void setContainerFactoryBeanName(String containerFactoryBeanName) {
		this.containerFactoryBeanName = containerFactoryBeanName;
	}

	/**
	 * Set the {@link JmsListenerContainerFactory} to use in case a
	 * {@link JmsListenerEndpoint} is registered with a {@code null} container
	 * factory.
	 * <p>Alternatively, the bean name of the {@link JmsListenerContainerFactory}
	 * to use can be specified for a lazy lookup, see {@see #setContainerFactoryBeanName}
	 */
	public void setContainerFactory(JmsListenerContainerFactory<?> containerFactory) {
		this.containerFactory = containerFactory;
	}

	/**
	 * Set the {@link JmsHandlerMethodFactory} to use to configure the message
	 * listener responsible to serve an endpoint detected by this processor.
	 * <p>By default, {@link DefaultJmsHandlerMethodFactory} is used and it
	 * can be configured further to support additional method arguments
	 * or to customize conversion and validation support. See
	 * {@link DefaultJmsHandlerMethodFactory} javadoc for more details.
	 */
	public void setJmsHandlerMethodFactory(JmsHandlerMethodFactory jmsHandlerMethodFactory) {
		this.jmsHandlerMethodFactory = jmsHandlerMethodFactory;
	}

	/**
	 * Return the custom {@link JmsHandlerMethodFactory} to use, if any.
	 */
	public JmsHandlerMethodFactory getJmsHandlerMethodFactory() {
		return jmsHandlerMethodFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Register a new {@link JmsListenerEndpoint} alongside the {@link JmsListenerContainerFactory}
	 * to use to create the underlying container.
	 * <p>The {@code factory} may be {@code null} if the default factory has to be used for that
	 * endpoint.
	 */
	public void registerEndpoint(JmsListenerEndpoint endpoint, JmsListenerContainerFactory<?> factory) {
		Assert.notNull(endpoint, "Endpoint must be set");
		Assert.notNull(endpoint.getId(), "Endpoint id must be set");
		// Factory may be null, we defer the resolution right before actually creating the container
		this.endpointDescriptors.add(new JmsListenerEndpointDescriptor(endpoint, factory));
	}

	/**
	 * Register a new {@link JmsListenerEndpoint} using the default {@link JmsListenerContainerFactory}
	 * to create the underlying container.
	 *
	 * @see #setContainerFactory(JmsListenerContainerFactory)
	 * @see #registerEndpoint(JmsListenerEndpoint, JmsListenerContainerFactory)
	 */
	public void registerEndpoint(JmsListenerEndpoint endpoint) {
		registerEndpoint(endpoint, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	 	Assert.notNull(applicationContext, "ApplicationContext must not be null");
		startAllEndpoints();
	}

	protected void startAllEndpoints() throws Exception {
		for (JmsListenerEndpointDescriptor descriptor : endpointDescriptors) {
			endpointRegistry.createJmsListenerContainer(
					descriptor.endpoint, resolveContainerFactory(descriptor));
		}
	}

	private JmsListenerContainerFactory<?> resolveContainerFactory(JmsListenerEndpointDescriptor descriptor) {
		if (descriptor.containerFactory != null) {
			return descriptor.containerFactory;
		}
		else if (this.containerFactory != null) {
			return this.containerFactory;
		}
		else if (this.containerFactoryBeanName != null) {
			this.containerFactory =  applicationContext.getBean(
					this.containerFactoryBeanName, JmsListenerContainerFactory.class);
			return this.containerFactory; // Consider changing this if live change of the factory is required
		}
		else {
			throw new IllegalStateException("Could not resolve the "
					+ JmsListenerContainerFactory.class.getSimpleName() + " to use for ["
					+ descriptor.endpoint + "] no factory was given and no default is set.");
		}
	}


	private static class JmsListenerEndpointDescriptor {
		private final JmsListenerEndpoint endpoint;

		private final JmsListenerContainerFactory<?> containerFactory;

		private JmsListenerEndpointDescriptor(JmsListenerEndpoint endpoint,
				JmsListenerContainerFactory<?> containerFactory) {
			this.endpoint = endpoint;
			this.containerFactory = containerFactory;
		}
	}

}
