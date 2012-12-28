/*
 * Copyright 2002-2011 the original author or authors.
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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DataSource} implementation that delegates all calls to a WebSphere
 * target {@link DataSource}, typically obtained from JNDI, applying a current
 * isolation level and/or current user credentials to every Connection obtained
 * from it.
 *
 * <p>Uses IBM-specific API to get a JDBC Connection with a specific isolation
 * level (and read-only flag) from a WebSphere DataSource
 * (<a href="http://publib.boulder.ibm.com/infocenter/wasinfo/v5r1//topic/com.ibm.websphere.base.doc/info/aes/ae/rdat_extiapi.html">IBM code example</a>).
 * Supports the transaction-specific isolation level exposed by
 * {@link org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()}.
 * It's also possible to specify a default isolation level, to be applied when the
 * current Spring-managed transaction does not define a specific isolation level.
 *
 * <p>Usage example, defining the target DataSource as an inner-bean JNDI lookup
 * (of course, you can link to any WebSphere DataSource through a bean reference):
 *
 * <pre class="code">
 * &lt;bean id="myDataSource" class="org.springframework.jdbc.datasource.WebSphereDataSourceAdapter"&gt;
 *   &lt;property name="targetDataSource"&gt;
 *     &lt;bean class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *       &lt;property name="jndiName" value="jdbc/myds"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * Thanks to Ricardo Olivieri for submitting the original implementation
 * of this approach!
 *
 * @author Juergen Hoeller
 * @author <a href="mailto:lari.hotari@sagire.fi">Lari Hotari</a>
 * @author <a href="mailto:roliv@us.ibm.com">Ricardo N. Olivieri</a>
 * @since 2.0.3
 * @see com.ibm.websphere.rsadapter.JDBCConnectionSpec
 * @see com.ibm.websphere.rsadapter.WSDataSource#getConnection(com.ibm.websphere.rsadapter.JDBCConnectionSpec)
 * @see org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()
 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
 */
public class WebSphereDataSourceAdapter extends IsolationLevelDataSourceAdapter {

	protected final Log logger = LogFactory.getLog(getClass());

	private Class wsDataSourceClass;

	private Method newJdbcConnSpecMethod;

	private Method wsDataSourceGetConnectionMethod;

	private Method setTransactionIsolationMethod;

	private Method setReadOnlyMethod;

	private Method setUserNameMethod;

	private Method setPasswordMethod;


	/**
	 * This constructor retrieves the WebSphere JDBC connection spec API,
	 * so we can get obtain specific WebSphere Connections using reflection.
	 */
	public WebSphereDataSourceAdapter() {
		try {
			this.wsDataSourceClass = getClass().getClassLoader().loadClass("com.ibm.websphere.rsadapter.WSDataSource");
			Class jdbcConnSpecClass = getClass().getClassLoader().loadClass("com.ibm.websphere.rsadapter.JDBCConnectionSpec");
			Class wsrraFactoryClass = getClass().getClassLoader().loadClass("com.ibm.websphere.rsadapter.WSRRAFactory");
			this.newJdbcConnSpecMethod = wsrraFactoryClass.getMethod("createJDBCConnectionSpec", (Class[]) null);
			this.wsDataSourceGetConnectionMethod =
					this.wsDataSourceClass.getMethod("getConnection", new Class[] {jdbcConnSpecClass});
			this.setTransactionIsolationMethod =
					jdbcConnSpecClass.getMethod("setTransactionIsolation", new Class[] {int.class});
			this.setReadOnlyMethod = jdbcConnSpecClass.getMethod("setReadOnly", new Class[] {Boolean.class});
			this.setUserNameMethod = jdbcConnSpecClass.getMethod("setUserName", new Class[] {String.class});
			this.setPasswordMethod = jdbcConnSpecClass.getMethod("setPassword", new Class[] {String.class});
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize WebSphereDataSourceAdapter because WebSphere API classes are not available: " + ex);
		}
	}

	/**
	 * Checks that the specified 'targetDataSource' actually is
	 * a WebSphere WSDataSource.
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		if (!this.wsDataSourceClass.isInstance(getTargetDataSource())) {
			throw new IllegalStateException(
					"Specified 'targetDataSource' is not a WebSphere WSDataSource: " + getTargetDataSource());
		}
	}


	/**
	 * Builds a WebSphere JDBCConnectionSpec object for the current settings
	 * and calls {@code WSDataSource.getConnection(JDBCConnectionSpec)}.
	 * @see #createConnectionSpec
	 * @see com.ibm.websphere.rsadapter.WSDataSource#getConnection(com.ibm.websphere.rsadapter.JDBCConnectionSpec)
	 */
	@Override
	protected Connection doGetConnection(String username, String password) throws SQLException {
		// Create JDBCConnectionSpec using current isolation level value and read-only flag.
		Object connSpec = createConnectionSpec(
				getCurrentIsolationLevel(), getCurrentReadOnlyFlag(), username, password);
		if (logger.isDebugEnabled()) {
			logger.debug("Obtaining JDBC Connection from WebSphere DataSource [" +
					getTargetDataSource() + "], using ConnectionSpec [" + connSpec + "]");
		}
		// Create Connection through invoking WSDataSource.getConnection(JDBCConnectionSpec)
		return (Connection) ReflectionUtils.invokeJdbcMethod(
				this.wsDataSourceGetConnectionMethod, getTargetDataSource(), connSpec);
	}

	/**
	 * Create a WebSphere {@code JDBCConnectionSpec} object for the given charateristics.
	 * <p>The default implementation uses reflection to apply the given settings.
	 * Can be overridden in subclasses to customize the JDBCConnectionSpec object
	 * (<a href="http://publib.boulder.ibm.com/infocenter/wasinfo/v6r0/topic/com.ibm.websphere.javadoc.doc/public_html/api/com/ibm/websphere/rsadapter/JDBCConnectionSpec.html">JDBCConnectionSpec javadoc</a>;
	 * <a href="http://www.ibm.com/developerworks/websphere/library/techarticles/0404_tang/0404_tang.html">IBM developerWorks article</a>).
	 * @param isolationLevel the isolation level to apply (or {@code null} if none)
	 * @param readOnlyFlag the read-only flag to apply (or {@code null} if none)
	 * @param username the username to apply ({@code null} or empty indicates the default)
	 * @param password the password to apply (may be {@code null} or empty)
	 * @throws SQLException if thrown by JDBCConnectionSpec API methods
	 * @see com.ibm.websphere.rsadapter.JDBCConnectionSpec
	 */
	protected Object createConnectionSpec(
			Integer isolationLevel, Boolean readOnlyFlag, String username, String password) throws SQLException {

		Object connSpec = ReflectionUtils.invokeJdbcMethod(this.newJdbcConnSpecMethod, null);
		if (isolationLevel != null) {
			ReflectionUtils.invokeJdbcMethod(this.setTransactionIsolationMethod, connSpec, isolationLevel);
		}
		if (readOnlyFlag != null) {
			ReflectionUtils.invokeJdbcMethod(this.setReadOnlyMethod, connSpec, readOnlyFlag);
		}
		// If the username is empty, we'll simply let the target DataSource
		// use its default credentials.
		if (StringUtils.hasLength(username)) {
			ReflectionUtils.invokeJdbcMethod(this.setUserNameMethod, connSpec, username);
			ReflectionUtils.invokeJdbcMethod(this.setPasswordMethod, connSpec, password);
		}
		return connSpec;
	}

}
