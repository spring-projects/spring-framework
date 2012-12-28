/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jms.support;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Constants;
import org.springframework.jms.JmsException;

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
 * @since 1.2
 * @see org.springframework.jms.support.destination.JmsDestinationAccessor
 * @see org.springframework.jms.core.JmsTemplate
 */
public abstract class JmsAccessor implements InitializingBean {

	/** Constants instance for javax.jms.Session */
	private static final Constants sessionConstants = new Constants(Session.class);


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private ConnectionFactory connectionFactory;

	private boolean sessionTransacted = false;

	private int sessionAcknowledgeMode = Session.AUTO_ACKNOWLEDGE;


	/**
	 * Set the ConnectionFactory to use for obtaining JMS {@link Connection Connections}.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Return the ConnectionFactory that this accessor uses for obtaining
	 * JMS {@link Connection Connections}.
	 */
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * Set the transaction mode that is used when creating a JMS {@link Session}.
	 * Default is "false".
	 * <p>Note that within a JTA transaction, the parameters passed to
	 * {@code create(Queue/Topic)Session(boolean transacted, int acknowledgeMode)}
	 * method are not taken into account. Depending on the J2EE transaction context,
	 * the container makes its own decisions on these values. Analogously, these
	 * parameters are not taken into account within a locally managed transaction
	 * either, since the accessor operates on an existing JMS Session in this case.
	 * <p>Setting this flag to "true" will use a short local JMS transaction
	 * when running outside of a managed transaction, and a synchronized local
	 * JMS transaction in case of a managed transaction (other than an XA
	 * transaction) being present. The latter has the effect of a local JMS
	 * transaction being managed alongside the main transaction (which might
	 * be a native JDBC transaction), with the JMS transaction committing
	 * right after the main transaction.
	 * @see javax.jms.Connection#createSession(boolean, int)
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
	 * Set the JMS acknowledgement mode by the name of the corresponding constant
	 * in the JMS {@link Session} interface, e.g. "CLIENT_ACKNOWLEDGE".
	 * <p>If you want to use vendor-specific extensions to the acknowledgment mode,
	 * use {@link #setSessionAcknowledgeModeName(String)} instead.
	 * @param constantName the name of the {@link Session} acknowledge mode constant
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Connection#createSession(boolean, int)
	 */
	public void setSessionAcknowledgeModeName(String constantName) {
		setSessionAcknowledgeMode(sessionConstants.asNumber(constantName).intValue());
	}

	/**
	 * Set the JMS acknowledgement mode that is used when creating a JMS
	 * {@link Session} to send a message.
	 * <p>Default is {@link Session#AUTO_ACKNOWLEDGE}.
	 * <p>Vendor-specific extensions to the acknowledgment mode can be set here as well.
	 * <p>Note that that inside an EJB the parameters to
	 * create(Queue/Topic)Session(boolean transacted, int acknowledgeMode) method
	 * are not taken into account. Depending on the transaction context in the EJB,
	 * the container makes its own decisions on these values. See section 17.3.5
	 * of the EJB spec.
	 * @param sessionAcknowledgeMode the acknowledgement mode constant
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Connection#createSession(boolean, int)
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

	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	/**
	 * Convert the specified checked {@link javax.jms.JMSException JMSException} to
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


	//-------------------------------------------------------------------------
	// JMS 1.1 factory methods, potentially overridden for JMS 1.0.2
	//-------------------------------------------------------------------------

	/**
	 * Create a JMS Connection via this template's ConnectionFactory.
	 * <p>This implementation uses JMS 1.1 API.
	 * @return the new JMS Connection
	 * @throws JMSException if thrown by JMS API methods
	 * @see javax.jms.ConnectionFactory#createConnection()
	 */
	protected Connection createConnection() throws JMSException {
		return getConnectionFactory().createConnection();
	}

	/**
	 * Create a JMS Session for the given Connection.
	 * <p>This implementation uses JMS 1.1 API.
	 * @param con the JMS Connection to create a Session for
	 * @return the new JMS Session
	 * @throws JMSException if thrown by JMS API methods
	 * @see javax.jms.Connection#createSession(boolean, int)
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(isSessionTransacted(), getSessionAcknowledgeMode());
	}

	/**
	 * Determine whether the given Session is in client acknowledge mode.
	 * <p>This implementation uses JMS 1.1 API.
	 * @param session the JMS Session to check
	 * @return whether the given Session is in client acknowledge mode
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 * @see javax.jms.Session#getAcknowledgeMode()
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 */
	protected boolean isClientAcknowledge(Session session) throws JMSException {
		return (session.getAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE);
	}

}
