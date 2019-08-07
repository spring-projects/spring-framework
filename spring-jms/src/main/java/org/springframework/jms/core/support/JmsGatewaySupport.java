/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.core.support;

import javax.jms.ConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.lang.Nullable;

/**
 * Convenient super class for application classes that need JMS access.
 *
 * <p>Requires a ConnectionFactory or a JmsTemplate instance to be set.
 * It will create its own JmsTemplate if a ConnectionFactory is passed in.
 * A custom JmsTemplate instance can be created for a given ConnectionFactory
 * through overriding the {@link #createJmsTemplate} method.
 *
 * @author Mark Pollack
 * @since 1.1.1
 * @see #setConnectionFactory
 * @see #setJmsTemplate
 * @see #createJmsTemplate
 * @see org.springframework.jms.core.JmsTemplate
 */
public abstract class JmsGatewaySupport implements InitializingBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private JmsTemplate jmsTemplate;


	/**
	 * Set the JMS connection factory to be used by the gateway.
	 * Will automatically create a JmsTemplate for the given ConnectionFactory.
	 * @see #createJmsTemplate
	 * @see #setConnectionFactory(javax.jms.ConnectionFactory)
	 */
	public final void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.jmsTemplate = createJmsTemplate(connectionFactory);
	}

	/**
	 * Create a JmsTemplate for the given ConnectionFactory.
	 * Only invoked if populating the gateway with a ConnectionFactory reference.
	 * <p>Can be overridden in subclasses to provide a JmsTemplate instance with
	 * a different configuration.
	 * @param connectionFactory the JMS ConnectionFactory to create a JmsTemplate for
	 * @return the new JmsTemplate instance
	 * @see #setConnectionFactory
	 */
	protected JmsTemplate createJmsTemplate(ConnectionFactory connectionFactory) {
		return new JmsTemplate(connectionFactory);
	}

	/**
	 * Return the JMS ConnectionFactory used by the gateway.
	 */
	@Nullable
	public final ConnectionFactory getConnectionFactory() {
		return (this.jmsTemplate != null ? this.jmsTemplate.getConnectionFactory() : null);
	}

	/**
	 * Set the JmsTemplate for the gateway.
	 * @see #setConnectionFactory(javax.jms.ConnectionFactory)
	 */
	public final void setJmsTemplate(@Nullable JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	/**
	 * Return the JmsTemplate for the gateway.
	 */
	@Nullable
	public final JmsTemplate getJmsTemplate() {
		return this.jmsTemplate;
	}

	@Override
	public final void afterPropertiesSet() throws IllegalArgumentException, BeanInitializationException {
		if (this.jmsTemplate == null) {
			throw new IllegalArgumentException("'connectionFactory' or 'jmsTemplate' is required");
		}
		try {
			initGateway();
		}
		catch (Exception ex) {
			throw new BeanInitializationException("Initialization of JMS gateway failed: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Subclasses can override this for custom initialization behavior.
	 * Gets called after population of this instance's bean properties.
	 * @throws java.lang.Exception if initialization fails
	 */
	protected void initGateway() throws Exception {
	}

}
