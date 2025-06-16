/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.jms.connection;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An adapter for a target JMS {@link jakarta.jms.ConnectionFactory}, applying the
 * given user credentials to every standard methods that can also be used with
 * authentication, this  {@code createConnection()} and {@code createContext()}. In
 * other words, it is implicitly invoking {@code createConnection(username, password)} or
 * {@code createContext(username, password)} on the target. All other methods simply
 * delegate to the corresponding methods of the target ConnectionFactory.
 *
 * <p>Can be used to proxy a target JNDI ConnectionFactory that does not have user
 * credentials configured. Client code can work with the ConnectionFactory without
 * passing in username and password on every {@code createConnection()} and
 * {@code createContext()} call.
 *
 * <p>In the following example, client code can simply transparently work
 * with the preconfigured "myConnectionFactory", implicitly accessing
 * "myTargetConnectionFactory" with the specified user credentials.
 *
 * <pre class="code">
 * &lt;bean id="myTargetConnectionFactory" class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/jms/mycf"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myConnectionFactory" class="org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter"&gt;
 *   &lt;property name="targetConnectionFactory" ref="myTargetConnectionFactory"/&gt;
 *   &lt;property name="username" value="myusername"/&gt;
 *   &lt;property name="password" value="mypassword"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>If the "username" is empty, this proxy will simply delegate to the standard
 * {@code createConnection()} or {@code createContext()} method of the target
 * ConnectionFactory. This can be used to keep a UserCredentialsConnectionFactoryAdapter
 * bean definition just for the <i>option</i> of implicitly passing in user credentials
 * if the particular target ConnectionFactory requires it.
 *
 * <p>As of Spring Framework 5, this class delegates JMS 2.0 {@code JMSContext}
 * calls and therefore requires the JMS 2.0 API to be present at runtime.
 * It may nevertheless run against a JMS 1.1 driver (bound to the JMS 2.0 API)
 * as long as no actual JMS 2.0 calls are triggered by the application's setup.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 1.2
 * @see #createConnection
 * @see #createContext
 * @see #createQueueConnection
 * @see #createTopicConnection
 */
public class UserCredentialsConnectionFactoryAdapter
		implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory, InitializingBean {

	private @Nullable ConnectionFactory targetConnectionFactory;

	private @Nullable String username;

	private @Nullable String password;

	private final ThreadLocal<JmsUserCredentials> threadBoundCredentials =
			new NamedThreadLocal<>("Current JMS user credentials");


	/**
	 * Set the target ConnectionFactory that this ConnectionFactory should delegate to.
	 */
	public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "'targetConnectionFactory' must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * Set the username that this adapter should use for retrieving Connections.
	 * Default is no specific user.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Set the password that this adapter should use for retrieving Connections.
	 * Default is no specific password.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.targetConnectionFactory == null) {
			throw new IllegalArgumentException("Property 'targetConnectionFactory' is required");
		}
	}


	/**
	 * Set user credentials for this proxy and the current thread.
	 * The given username and password will be applied to all subsequent
	 * {@code createConnection()} calls on this ConnectionFactory proxy.
	 * <p>This will override any statically specified user credentials,
	 * that is, values of the "username" and "password" bean properties.
	 * @param username the username to apply
	 * @param password the password to apply
	 * @see #removeCredentialsFromCurrentThread
	 */
	public void setCredentialsForCurrentThread(String username, String password) {
		this.threadBoundCredentials.set(new JmsUserCredentials(username, password));
	}

	/**
	 * Remove any user credentials for this proxy from the current thread.
	 * Statically specified user credentials apply again afterwards.
	 * @see #setCredentialsForCurrentThread
	 */
	public void removeCredentialsFromCurrentThread() {
		this.threadBoundCredentials.remove();
	}


	/**
	 * Determine whether there are currently thread-bound credentials,
	 * using them if available, falling back to the statically specified
	 * username and password (i.e. values of the bean properties) otherwise.
	 * @see #doCreateConnection
	 */
	@Override
	public final Connection createConnection() throws JMSException {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateConnection(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doCreateConnection(this.username, this.password);
		}
	}

	/**
	 * Delegate the call straight to the target ConnectionFactory.
	 */
	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		return doCreateConnection(username, password);
	}

	/**
	 * This implementation delegates to the {@code createConnection(username, password)}
	 * method of the target ConnectionFactory, passing in the specified user credentials.
	 * If the specified username is empty, it will simply delegate to the standard
	 * {@code createConnection()} method of the target ConnectionFactory.
	 * @param username the username to use
	 * @param password the password to use
	 * @return the Connection
	 * @see jakarta.jms.ConnectionFactory#createConnection(String, String)
	 * @see jakarta.jms.ConnectionFactory#createConnection()
	 */
	protected Connection doCreateConnection(@Nullable String username, @Nullable String password) throws JMSException {
		ConnectionFactory target = obtainTargetConnectionFactory();
		if (StringUtils.hasLength(username)) {
			return target.createConnection(username, password);
		}
		else {
			return target.createConnection();
		}
	}

	/**
	 * Determine whether there are currently thread-bound credentials,
	 * using them if available, falling back to the statically specified
	 * username and password (i.e. values of the bean properties) else.
	 * @see #doCreateQueueConnection
	 */
	@Override
	public final QueueConnection createQueueConnection() throws JMSException {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateQueueConnection(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doCreateQueueConnection(this.username, this.password);
		}
	}

	/**
	 * Delegate the call straight to the target QueueConnectionFactory.
	 */
	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		return doCreateQueueConnection(username, password);
	}

	/**
	 * This implementation delegates to the {@code createQueueConnection(username, password)}
	 * method of the target QueueConnectionFactory, passing in the specified user credentials.
	 * If the specified username is empty, it will simply delegate to the standard
	 * {@code createQueueConnection()} method of the target ConnectionFactory.
	 * @param username the username to use
	 * @param password the password to use
	 * @return the Connection
	 * @see jakarta.jms.QueueConnectionFactory#createQueueConnection(String, String)
	 * @see jakarta.jms.QueueConnectionFactory#createQueueConnection()
	 */
	protected QueueConnection doCreateQueueConnection(
			@Nullable String username, @Nullable String password) throws JMSException {

		ConnectionFactory target = obtainTargetConnectionFactory();
		if (!(target instanceof QueueConnectionFactory queueFactory)) {
			throw new jakarta.jms.IllegalStateException("'targetConnectionFactory' is not a QueueConnectionFactory");
		}
		if (StringUtils.hasLength(username)) {
			return queueFactory.createQueueConnection(username, password);
		}
		else {
			return queueFactory.createQueueConnection();
		}
	}

	/**
	 * Determine whether there are currently thread-bound credentials,
	 * using them if available, falling back to the statically specified
	 * username and password (i.e. values of the bean properties) else.
	 * @see #doCreateTopicConnection
	 */
	@Override
	public final TopicConnection createTopicConnection() throws JMSException {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateTopicConnection(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doCreateTopicConnection(this.username, this.password);
		}
	}

	/**
	 * Delegate the call straight to the target TopicConnectionFactory.
	 */
	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		return doCreateTopicConnection(username, password);
	}

	/**
	 * This implementation delegates to the {@code createTopicConnection(username, password)}
	 * method of the target TopicConnectionFactory, passing in the specified user credentials.
	 * If the specified username is empty, it will simply delegate to the standard
	 * {@code createTopicConnection()} method of the target ConnectionFactory.
	 * @param username the username to use
	 * @param password the password to use
	 * @return the Connection
	 * @see jakarta.jms.TopicConnectionFactory#createTopicConnection(String, String)
	 * @see jakarta.jms.TopicConnectionFactory#createTopicConnection()
	 */
	protected TopicConnection doCreateTopicConnection(
			@Nullable String username, @Nullable String password) throws JMSException {

		ConnectionFactory target = obtainTargetConnectionFactory();
		if (!(target instanceof TopicConnectionFactory topicFactory)) {
			throw new jakarta.jms.IllegalStateException("'targetConnectionFactory' is not a TopicConnectionFactory");
		}
		if (StringUtils.hasLength(username)) {
			return topicFactory.createTopicConnection(username, password);
		}
		else {
			return topicFactory.createTopicConnection();
		}
	}

	@Override
	public JMSContext createContext() {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateContext(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doCreateContext(this.username, this.password);
		}
	}

	protected JMSContext doCreateContext(@Nullable String username, @Nullable String password) {
		if (StringUtils.hasLength(username)) {
			return obtainTargetConnectionFactory().createContext(username, password);
		}
		else {
			return obtainTargetConnectionFactory().createContext();
		}
	}

	@Override
	public JMSContext createContext(String userName, String password) {
		return obtainTargetConnectionFactory().createContext(userName, password);
	}

	@Override
	public JMSContext createContext(String userName, String password, int sessionMode) {
		return obtainTargetConnectionFactory().createContext(userName, password, sessionMode);
	}

	@Override
	public JMSContext createContext(int sessionMode) {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateContext(threadCredentials.username, threadCredentials.password, sessionMode);
		}
		else {
			return doCreateContext(this.username, this.password, sessionMode);
		}
	}

	protected JMSContext doCreateContext(@Nullable String username, @Nullable String password, int sessionMode) {
		if (StringUtils.hasLength(username)) {
			return obtainTargetConnectionFactory().createContext(username, password, sessionMode);
		}
		else {
			return obtainTargetConnectionFactory().createContext(sessionMode);
		}
	}

	private ConnectionFactory obtainTargetConnectionFactory() {
		Assert.state(this.targetConnectionFactory != null, "'targetConnectionFactory' is required");
		return this.targetConnectionFactory;
	}


	/**
	 * Inner class used as ThreadLocal value.
	 */
	private static final class JmsUserCredentials {

		public final String username;

		public final String password;

		public JmsUserCredentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public String toString() {
			return "JmsUserCredentials[username='" + this.username + "',password='" + this.password + "']";
		}
	}

}
