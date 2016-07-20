/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An adapter for a target JDBC {@link javax.sql.DataSource}, applying the specified
 * user credentials to every standard {@code getConnection()} call, implicitly
 * invoking {@code getConnection(username, password)} on the target.
 * All other methods simply delegate to the corresponding methods of the
 * target DataSource.
 *
 * <p>Can be used to proxy a target JNDI DataSource that does not have user
 * credentials configured. Client code can work with this DataSource as usual,
 * using the standard {@code getConnection()} call.
 *
 * <p>In the following example, client code can simply transparently work with
 * the preconfigured "myDataSource", implicitly accessing "myTargetDataSource"
 * with the specified user credentials.
 *
 * <pre class="code">
 * &lt;bean id="myTargetDataSource" class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/jdbc/myds"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myDataSource" class="org.springframework.jdbc.datasource.UserCredentialsDataSourceAdapter"&gt;
 *   &lt;property name="targetDataSource" ref="myTargetDataSource"/&gt;
 *   &lt;property name="username" value="myusername"/&gt;
 *   &lt;property name="password" value="mypassword"/&gt;
 * &lt;/bean></pre>
 *
 * <p>If the "username" is empty, this proxy will simply delegate to the
 * standard {@code getConnection()} method of the target DataSource.
 * This can be used to keep a UserCredentialsDataSourceAdapter bean definition
 * just for the <i>option</i> of implicitly passing in user credentials if
 * the particular target DataSource requires it.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see #getConnection
 */
public class UserCredentialsDataSourceAdapter extends DelegatingDataSource {

	private String username;

	private String password;

	private final ThreadLocal<JdbcUserCredentials> threadBoundCredentials =
			new NamedThreadLocal<>("Current JDBC user credentials");


	/**
	 * Set the default username that this adapter should use for retrieving Connections.
	 * <p>Default is no specific user. Note that an explicitly specified username
	 * will always override any username/password specified at the DataSource level.
	 * @see #setPassword
	 * @see #setCredentialsForCurrentThread(String, String)
	 * @see #getConnection(String, String)
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Set the default user's password that this adapter should use for retrieving Connections.
	 * <p>Default is no specific password. Note that an explicitly specified username
	 * will always override any username/password specified at the DataSource level.
	 * @see #setUsername
	 * @see #setCredentialsForCurrentThread(String, String)
	 * @see #getConnection(String, String)
	 */
	public void setPassword(String password) {
		this.password = password;
	}


	/**
	 * Set user credententials for this proxy and the current thread.
	 * The given username and password will be applied to all subsequent
	 * {@code getConnection()} calls on this DataSource proxy.
	 * <p>This will override any statically specified user credentials,
	 * that is, values of the "username" and "password" bean properties.
	 * @param username the username to apply
	 * @param password the password to apply
	 * @see #removeCredentialsFromCurrentThread
	 */
	public void setCredentialsForCurrentThread(String username, String password) {
		this.threadBoundCredentials.set(new JdbcUserCredentials(username, password));
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
	 * username and password (i.e. values of the bean properties) else.
	 * <p>Delegates to {@link #doGetConnection(String, String)} with the
	 * determined credentials as parameters.
	 */
	@Override
	public Connection getConnection() throws SQLException {
		JdbcUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doGetConnection(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doGetConnection(this.username, this.password);
		}
	}

	/**
	 * Simply delegates to {@link #doGetConnection(String, String)},
	 * keeping the given user credentials as-is.
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return doGetConnection(username, password);
	}

	/**
	 * This implementation delegates to the {@code getConnection(username, password)}
	 * method of the target DataSource, passing in the specified user credentials.
	 * If the specified username is empty, it will simply delegate to the standard
	 * {@code getConnection()} method of the target DataSource.
	 * @param username the username to use
	 * @param password the password to use
	 * @return the Connection
	 * @see javax.sql.DataSource#getConnection(String, String)
	 * @see javax.sql.DataSource#getConnection()
	 */
	protected Connection doGetConnection(String username, String password) throws SQLException {
		Assert.state(getTargetDataSource() != null, "'targetDataSource' is required");
		if (StringUtils.hasLength(username)) {
			return getTargetDataSource().getConnection(username, password);
		}
		else {
			return getTargetDataSource().getConnection();
		}
	}


	/**
	 * Inner class used as ThreadLocal value.
	 */
	private static class JdbcUserCredentials {

		public final String username;

		public final String password;

		private JdbcUserCredentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public String toString() {
			return "JdbcUserCredentials[username='" + this.username + "',password='" + this.password + "']";
		}
	}

}
