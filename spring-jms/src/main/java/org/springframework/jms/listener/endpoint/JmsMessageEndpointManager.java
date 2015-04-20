/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.jms.listener.endpoint;

import javax.jms.MessageListener;
import javax.resource.ResourceException;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.jca.endpoint.GenericMessageEndpointManager;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * Extension of the generic JCA 1.5
 * {@link org.springframework.jca.endpoint.GenericMessageEndpointManager},
 * adding JMS-specific support for ActivationSpec configuration.
 *
 * <p>Allows for defining a common {@link JmsActivationSpecConfig} object
 * that gets converted into a provider-specific JCA 1.5 ActivationSpec
 * object for activating the endpoint.
 *
 * <p><b>NOTE:</b> This JCA-based endpoint manager supports standard JMS
 * {@link javax.jms.MessageListener} endpoints only. It does <i>not</i> support
 * Spring's {@link org.springframework.jms.listener.SessionAwareMessageListener}
 * variant, simply because the JCA endpoint management contract does not allow
 * for obtaining the current JMS {@link javax.jms.Session}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5
 * @see javax.jms.MessageListener
 * @see #setActivationSpecConfig
 * @see JmsActivationSpecConfig
 * @see JmsActivationSpecFactory
 * @see JmsMessageEndpointFactory
 */
public class JmsMessageEndpointManager extends GenericMessageEndpointManager
		implements BeanNameAware, MessageListenerContainer {

	private final JmsMessageEndpointFactory endpointFactory = new JmsMessageEndpointFactory();

	private boolean messageListenerSet = false;

	private JmsActivationSpecFactory activationSpecFactory = new DefaultJmsActivationSpecFactory();

	private JmsActivationSpecConfig activationSpecConfig;


	/**
	 * Set the JMS MessageListener for this endpoint.
	 * <p>This is a shortcut for configuring a dedicated JmsMessageEndpointFactory.
	 * @see JmsMessageEndpointFactory#setMessageListener
	 */
	public void setMessageListener(MessageListener messageListener) {
		this.endpointFactory.setMessageListener(messageListener);
		this.messageListenerSet = true;
	}

	/**
	 * Return the JMS MessageListener for this endpoint.
	 */
	public MessageListener getMessageListener() {
		return this.endpointFactory.getMessageListener();
	}

	/**
	 * Set the XA transaction manager to use for wrapping endpoint
	 * invocations, enlisting the endpoint resource in each such transaction.
	 * <p>The passed-in object may be a transaction manager which implements
	 * Spring's {@link org.springframework.transaction.jta.TransactionFactory}
	 * interface, or a plain {@link javax.transaction.TransactionManager}.
	 * <p>If no transaction manager is specified, the endpoint invocation
	 * will simply not be wrapped in an XA transaction. Consult your
	 * resource provider's ActivationSpec documentation for the local
	 * transaction options of your particular provider.
	 * <p>This is a shortcut for configuring a dedicated JmsMessageEndpointFactory.
	 * @see JmsMessageEndpointFactory#setTransactionManager
	 */
	public void setTransactionManager(Object transactionManager) {
		this.endpointFactory.setTransactionManager(transactionManager);
	}

	/**
	 * Set the factory for concrete JCA 1.5 ActivationSpec objects,
	 * creating JCA ActivationSpecs based on
	 * {@link #setActivationSpecConfig JmsActivationSpecConfig} objects.
	 * <p>This factory is dependent on the concrete JMS provider, e.g. on ActiveMQ.
	 * The default implementation simply guesses the ActivationSpec class name
	 * from the provider's class name (e.g. "ActiveMQResourceAdapter" ->
	 * "ActiveMQActivationSpec" in the same package), and populates the
	 * ActivationSpec properties as suggested by the JCA 1.5 specification
	 * (plus a couple of autodetected vendor-specific properties).
	 * @see DefaultJmsActivationSpecFactory
	 */
	public void setActivationSpecFactory(JmsActivationSpecFactory activationSpecFactory) {
		this.activationSpecFactory =
				(activationSpecFactory != null ? activationSpecFactory : new DefaultJmsActivationSpecFactory());
	}

	/**
	 * Set the DestinationResolver to use for resolving destination names
	 * into the JCA 1.5 ActivationSpec "destination" property.
	 * <p>If not specified, destination names will simply be passed in as Strings.
	 * If specified, destination names will be resolved into Destination objects first.
	 * <p>Note that a DestinationResolver is usually specified on the JmsActivationSpecFactory
	 * (see {@link StandardJmsActivationSpecFactory#setDestinationResolver}). This is simply
	 * a shortcut for parameterizing the default JmsActivationSpecFactory; it will replace
	 * any custom JmsActivationSpecFactory that might have been set before.
	 * @see StandardJmsActivationSpecFactory#setDestinationResolver
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		DefaultJmsActivationSpecFactory factory = new DefaultJmsActivationSpecFactory();
		factory.setDestinationResolver(destinationResolver);
		this.activationSpecFactory = factory;
	}

	/**
	 * Specify the {@link JmsActivationSpecConfig} object that this endpoint manager
	 * should use for activating its listener.
	 * <p>This config object will be turned into a concrete JCA 1.5 ActivationSpec
	 * object through a {@link #setActivationSpecFactory JmsActivationSpecFactory}.
	 */
	public void setActivationSpecConfig(JmsActivationSpecConfig activationSpecConfig) {
		this.activationSpecConfig = activationSpecConfig;
	}

	/**
	 * Return the {@link JmsActivationSpecConfig} object that this endpoint manager
	 * should use for activating its listener. Return {@code null} if none is set.
	 */
	public JmsActivationSpecConfig getActivationSpecConfig() {
		return this.activationSpecConfig;
	}

	/**
	 * Set the name of this message endpoint. Populated with the bean name
	 * automatically when defined within Spring's bean factory.
	 */
	@Override
	public void setBeanName(String beanName) {
		this.endpointFactory.setBeanName(beanName);
	}


	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (this.messageListenerSet) {
			setMessageEndpointFactory(this.endpointFactory);
		}
		if (this.activationSpecConfig != null) {
			setActivationSpec(
					this.activationSpecFactory.createActivationSpec(getResourceAdapter(), this.activationSpecConfig));
		}
		super.afterPropertiesSet();
	}


	@Override
	public void setupMessageListener(Object messageListener) {
		if (messageListener instanceof MessageListener) {
			setMessageListener((MessageListener) messageListener);
		}
		else {
			throw new IllegalArgumentException("Unsupported message listener '" +
					messageListener.getClass().getName() + "': only '" + MessageListener.class.getName() +
					"' type is supported");
		}
	}

	@Override
	public MessageConverter getMessageConverter() {
		JmsActivationSpecConfig config = getActivationSpecConfig();
		if (config != null) {
			return config.getMessageConverter();
		}
		return null;
	}

	@Override
	public DestinationResolver getDestinationResolver() {
		if (this.activationSpecFactory instanceof StandardJmsActivationSpecFactory) {
			return ((StandardJmsActivationSpecFactory) this.activationSpecFactory).getDestinationResolver();
		}
		return null;
	}

	@Override
	public boolean isPubSubDomain() {
		JmsActivationSpecConfig config = getActivationSpecConfig();
		if (config != null) {
			return config.isPubSubDomain();
		}
		throw new IllegalStateException("Could not determine pubSubDomain - no activation spec config is set");
	}

	@Override
	public boolean isReplyPubSubDomain() {
		JmsActivationSpecConfig config = getActivationSpecConfig();
		if (config != null) {
			return config.isReplyPubSubDomain();
		}
		throw new IllegalStateException("Could not determine reply pubSubDomain - no activation spec config is set");
	}

}
