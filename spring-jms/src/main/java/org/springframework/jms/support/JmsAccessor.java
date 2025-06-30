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

package org.springframework.jms.support;

import java.util.Map;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.JmsException;
import org.springframework.util.Assert;

/**
 * Base class for {@link org.springframework.jms.core.JmsTemplate} and other
 * JMS-accessing gateway helpers, defining common properties such as the
 * JMS {@link ConnectionFactory} to operate on. The subclass
 * {@link org.springframework.jms.support.destination.JmsDestinationAccessor}
 * adds further, destination-related properties.
 *
 * <p>Not intended to be used directly.
 * See {@link org.springframework.jms.core.JmsTemplate}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2
 * @see org.springframework.jms.support.destination.JmsDestinationAccessor
 * @see org.springframework.jms.core.JmsTemplate
 */
public abstract class JmsAccessor implements InitializingBean {

	/**
	 * Map of constant names to constant values for the constants defined in
	 * {@link jakarta.jms.Session}.
	 */
	private static final Map<String, Integer> sessionConstants = Map.of(
			"AUTO_ACKNOWLEDGE", Session.AUTO_ACKNOWLEDGE,
			"CLIENT_ACKNOWLEDGE", Session.CLIENT_ACKNOWLEDGE,
			"DUPS_OK_ACKNOWLEDGE", Session.DUPS_OK_ACKNOWLEDGE,
			"SESSION_TRANSACTED", Session.SESSION_TRANSACTED
		);


	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	private @Nullable ConnectionFactory connectionFactory;

	private boolean sessionTransacted = false;

	private int sessionAcknowledgeMode = Session.AUTO_ACKNOWLEDGE;


	/**
	 * Set the ConnectionFactory to use for obtaining JMS {@link Connection Connections}.
	 */
	public void setConnectionFactory(@Nullable ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Return the ConnectionFactory that this accessor uses for obtaining
	 * JMS {@link Connection Connections}.
	 */
	public @Nullable ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * Obtain the ConnectionFactory for actual use.
	 * @return the ConnectionFactory (never {@code null})
	 * @throws IllegalStateException in case of no ConnectionFactory set
	 * @since 5.0
	 */
	protected final ConnectionFactory obtainConnectionFactory() {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory;
	}

	/**
	 * Set the transaction mode that is used when creating a JMS {@link Session}.
	 * Default is "false".
	 * <p>Note that within a JTA transaction, the parameters passed to
	 * {@code create(Queue/Topic)Session(boolean transacted, int acknowledgeMode)}
	 * method are not taken into account. Depending on the Jakarta EE transaction context,
	 * the container makes its own decisions on these values. Analogously, these
	 * parameters are not taken into account within a locally managed transaction
	 * either, since the accessor operates on an existing JMS Session in this case.
	 * <p>Setting this flag to "true" will use a short local JMS transaction
	 * when running outside a managed transaction, and a synchronized local
	 * JMS transaction in case of a managed transaction (other than an XA
	 * transaction) being present. This has the effect of a local JMS
	 * transaction being managed alongside the main transaction (which might
	 * be a native JDBC transaction), with the JMS transaction committing
	 * right after the main transaction.
	 * @see jakarta.jms.Connection#createSession(boolean, int)
	 */
	public void setSessionTransacted(boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
	}

	/**
	 * Return whether the JMS {@link Session sessions} used by this
	 * accessor are supposed to be transacted.
	 * @see #setSessionTransacted(boolean)
	 */
	public boolean isSessionTransacted() {
		return this.sessionTransacted;
	}

	/**
	 * Set the JMS acknowledgement mode by the name of the corresponding constant in
	 * the JMS {@link Session} interface &mdash; for example, {@code "CLIENT_ACKNOWLEDGE"}.
	 * <p>If you want to use vendor-specific extensions to the acknowledgement mode,
	 * use {@link #setSessionAcknowledgeMode(int)} instead.
	 * @param constantName the name of the {@link Session} acknowledge mode constant
	 * @see jakarta.jms.Session#AUTO_ACKNOWLEDGE
	 * @see jakarta.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see jakarta.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see jakarta.jms.Session#SESSION_TRANSACTED
	 * @see jakarta.jms.Connection#createSession(int)
	 */
	public void setSessionAcknowledgeModeName(String constantName) {
		Assert.hasText(constantName, "'constantName' must not be null or blank");
		Integer sessionAcknowledgeMode = sessionConstants.get(constantName);
		Assert.notNull(sessionAcknowledgeMode, "Only acknowledge mode constants allowed");
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	/**
	 * Set the JMS acknowledgement mode that is used when creating a JMS
	 * {@link Session} to send a message.
	 * <p>Default is {@link Session#AUTO_ACKNOWLEDGE}.
	 * <p>Vendor-specific extensions to the acknowledgement mode can be set here as well.
	 * <p>Note that inside an EJB, the parameters to the
	 * {@code create(Queue/Topic)Session(boolean transacted, int acknowledgeMode)} method
	 * are not taken into account. Depending on the transaction context in the EJB,
	 * the container makes its own decisions on these values. See section 17.3.5
	 * of the EJB spec.
	 * @param sessionAcknowledgeMode the acknowledgement mode constant
	 * @see jakarta.jms.Session#AUTO_ACKNOWLEDGE
	 * @see jakarta.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see jakarta.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see jakarta.jms.Session#SESSION_TRANSACTED
	 * @see jakarta.jms.Connection#createSession(boolean, int)
	 */
	public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	/**
	 * Return the acknowledgement mode for JMS {@link Session sessions}.
	 */
	public int getSessionAcknowledgeMode() {
		return this.sessionAcknowledgeMode;
	}

	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	/**
	 * Convert the specified checked {@link jakarta.jms.JMSException JMSException} to
	 * a Spring runtime {@link org.springframework.jms.JmsException JmsException}
	 * equivalent.
	 * <p>The default implementation delegates to the
	 * {@link JmsUtils#convertJmsAccessException} method.
	 * @param ex the original checked {@link JMSException} to convert
	 * @return the Spring runtime {@link JmsException} wrapping {@code ex}
	 * @see JmsUtils#convertJmsAccessException
	 */
	protected JmsException convertJmsAccessException(JMSException ex) {
		return JmsUtils.convertJmsAccessException(ex);
	}

	/**
	 * Create a JMS Connection via this template's ConnectionFactory.
	 * <p>This implementation uses JMS 1.1 API.
	 * @return the new JMS Connection
	 * @throws JMSException if thrown by JMS API methods
	 * @see jakarta.jms.ConnectionFactory#createConnection()
	 */
	protected Connection createConnection() throws JMSException {
		ConnectionFactory cf = obtainConnectionFactory();
		Connection con = cf.createConnection();
		if (con == null) {
			throw new jakarta.jms.IllegalStateException(
					"ConnectionFactory returned null from createConnection(): " + cf);
		}
		return con;
	}

	/**
	 * Create a JMS Session for the given Connection.
	 * <p>This implementation uses JMS 1.1 API.
	 * @param con the JMS Connection to create a Session for
	 * @return the new JMS Session
	 * @throws JMSException if thrown by JMS API methods
	 * @see jakarta.jms.Connection#createSession(boolean, int)
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(isSessionTransacted(), getSessionAcknowledgeMode());
	}

	/**
	 * Determine whether the given Session is in client acknowledge mode.
	 * <p>This implementation uses JMS 1.1 API.
	 * @param session the JMS Session to check
	 * @return whether the given Session is in client acknowledge mode
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 * @see jakarta.jms.Session#getAcknowledgeMode()
	 * @see jakarta.jms.Session#CLIENT_ACKNOWLEDGE
	 */
	protected boolean isClientAcknowledge(Session session) throws JMSException {
		int mode = session.getAcknowledgeMode();
		return (mode != Session.SESSION_TRANSACTED &&
				mode != Session.AUTO_ACKNOWLEDGE &&
				mode != Session.DUPS_OK_ACKNOWLEDGE);
	}

}
