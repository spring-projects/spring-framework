/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.jdo;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Default implementation of the {@link JdoDialect} interface.
 * Updated to build on JDO 2.0 or higher, as of Spring 2.5.
 * Used as default dialect by {@link JdoAccessor} and {@link JdoTransactionManager}.
 *
 * <p>Simply begins a standard JDO transaction in {@code beginTransaction}.
 * Returns a handle for a JDO2 DataStoreConnection on {@code getJdbcConnection}.
 * Calls the corresponding JDO2 PersistenceManager operation on {@code flush}
 * Ignores a given query timeout in {@code applyQueryTimeout}.
 * Uses a Spring SQLExceptionTranslator for exception translation, if applicable.
 *
 * <p>Note that, even with JDO2, vendor-specific subclasses are still necessary
 * for special transaction semantics and more sophisticated exception translation.
 * Furthermore, vendor-specific subclasses are encouraged to expose the native JDBC
 * Connection on {@code getJdbcConnection}, rather than JDO2's wrapper handle.
 *
 * <p>This class also implements the PersistenceExceptionTranslator interface,
 * as autodetected by Spring's PersistenceExceptionTranslationPostProcessor,
 * for AOP-based translation of native exceptions to Spring DataAccessExceptions.
 * Hence, the presence of a standard DefaultJdoDialect bean automatically enables
 * a PersistenceExceptionTranslationPostProcessor to translate JDO exceptions.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJdbcExceptionTranslator
 * @see JdoAccessor#setJdoDialect
 * @see JdoTransactionManager#setJdoDialect
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 */
public class DefaultJdoDialect implements JdoDialect, PersistenceExceptionTranslator {

	// JDO 3.0 setTimeoutMillis method available?
	private static final Method setTimeoutMillisMethod =
			ClassUtils.getMethodIfAvailable(Query.class, "setTimeoutMillis", Integer.class);

	protected final Log logger = LogFactory.getLog(getClass());

	private SQLExceptionTranslator jdbcExceptionTranslator;


	/**
	 * Create a new DefaultJdoDialect.
	 */
	public DefaultJdoDialect() {
	}

	/**
	 * Create a new DefaultJdoDialect.
	 * @param connectionFactory the connection factory of the JDO PersistenceManagerFactory,
	 * which is used to initialize the default JDBC exception translator
	 * @see javax.jdo.PersistenceManagerFactory#getConnectionFactory()
	 * @see PersistenceManagerFactoryUtils#newJdbcExceptionTranslator(Object)
	 */
	public DefaultJdoDialect(Object connectionFactory) {
		this.jdbcExceptionTranslator = PersistenceManagerFactoryUtils.newJdbcExceptionTranslator(connectionFactory);
	}

	/**
	 * Set the JDBC exception translator for this dialect.
	 * <p>Applied to any SQLException root cause of a JDOException, if specified.
	 * The default is to rely on the JDO provider's native exception translation.
	 * @param jdbcExceptionTranslator exception translator
	 * @see java.sql.SQLException
	 * @see javax.jdo.JDOException#getCause()
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLStateSQLExceptionTranslator
	 */
	public void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator) {
		this.jdbcExceptionTranslator = jdbcExceptionTranslator;
	}

	/**
	 * Return the JDBC exception translator for this dialect, if any.
	 */
	public SQLExceptionTranslator getJdbcExceptionTranslator() {
		return this.jdbcExceptionTranslator;
	}


	//-------------------------------------------------------------------------
	// Hooks for transaction management (used by JdoTransactionManager)
	//-------------------------------------------------------------------------

	/**
	 * This implementation invokes the standard JDO {@code Transaction.begin}
	 * method. Throws an InvalidIsolationLevelException if a non-default isolation
	 * level is set.
	 * @see javax.jdo.Transaction#begin
	 * @see org.springframework.transaction.InvalidIsolationLevelException
	 */
	public Object beginTransaction(Transaction transaction, TransactionDefinition definition)
			throws JDOException, SQLException, TransactionException {

		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException(
					"Standard JDO does not support custom isolation levels: " +
					"use a special JdoDialect implementation for your JDO provider");
		}
		transaction.begin();
		return null;
	}

	/**
	 * This implementation does nothing, as the default beginTransaction implementation
	 * does not require any cleanup.
	 * @see #beginTransaction
	 */
	public void cleanupTransaction(Object transactionData) {
	}

	/**
	 * This implementation returns a DataStoreConnectionHandle for JDO2,
	 * which will also work on JDO1 until actually accessing the JDBC Connection.
	 * <p>For pre-JDO2 implementations, override this method to return the
	 * Connection through the corresponding vendor-specific mechanism, or {@code null}
	 * if the Connection is not retrievable.
	 * <p><b>NOTE:</b> A JDO2 DataStoreConnection is always a wrapper,
	 * never the native JDBC Connection. If you need access to the native JDBC
	 * Connection (or the connection pool handle, to be unwrapped via a Spring
	 * NativeJdbcExtractor), override this method to return the native
	 * Connection through the corresponding vendor-specific mechanism.
	 * <p>A JDO2 DataStoreConnection is only "borrowed" from the PersistenceManager:
	 * it needs to be returned as early as possible. Effectively, JDO2 requires the
	 * fetched Connection to be closed before continuing PersistenceManager work.
	 * For this reason, the exposed ConnectionHandle eagerly releases its JDBC
	 * Connection at the end of each JDBC data access operation (that is, on
	 * {@code DataSourceUtils.releaseConnection}).
	 * @see javax.jdo.PersistenceManager#getDataStoreConnection()
	 * @see org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
	 */
	public ConnectionHandle getJdbcConnection(PersistenceManager pm, boolean readOnly)
			throws JDOException, SQLException {

		return new DataStoreConnectionHandle(pm);
	}

	/**
	 * This implementation does nothing, assuming that the Connection
	 * will implicitly be closed with the PersistenceManager.
	 * <p>If the JDO provider returns a Connection handle that it
	 * expects the application to close, the dialect needs to invoke
	 * {@code Connection.close} here.
	 * @see java.sql.Connection#close()
	 */
	public void releaseJdbcConnection(ConnectionHandle conHandle, PersistenceManager pm)
			throws JDOException, SQLException {
	}

	/**
	 * This implementation applies a JDO 3.0 query timeout, if available. Otherwise,
	 * it sets the JPA 2.0 query hints "javax.persistence.lock.timeout" and
	 * "javax.persistence.query.timeout", assuming that JDO providers are often
	 * JPA providers as well.
	 */
	public void applyQueryTimeout(Query query, int remainingTimeInSeconds) throws JDOException {
		if (setTimeoutMillisMethod != null) {
			ReflectionUtils.invokeMethod(setTimeoutMillisMethod, query, remainingTimeInSeconds);
		}
		else {
			query.addExtension("javax.persistence.lock.timeout", remainingTimeInSeconds);
			query.addExtension("javax.persistence.query.timeout", remainingTimeInSeconds);
		}
	}


	//-----------------------------------------------------------------------------------
	// Hook for exception translation (used by JdoTransactionManager and JdoTemplate)
	//-----------------------------------------------------------------------------------

	/**
	 * Implementation of the PersistenceExceptionTranslator interface,
	 * as autodetected by Spring's PersistenceExceptionTranslationPostProcessor.
	 * <p>Converts the exception if it is a JDOException, using this JdoDialect.
	 * Else returns {@code null} to indicate an unknown exception.
	 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
	 * @see #translateException
	 */
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof JDOException) {
			return translateException((JDOException) ex);
		}
		return null;
	}

	/**
	 * This implementation delegates to PersistenceManagerFactoryUtils.
	 * @see PersistenceManagerFactoryUtils#convertJdoAccessException
	 */
	public DataAccessException translateException(JDOException ex) {
		if (getJdbcExceptionTranslator() != null && ex.getCause() instanceof SQLException) {
			return getJdbcExceptionTranslator().translate("JDO operation: " + ex.getMessage(),
					extractSqlStringFromException(ex), (SQLException) ex.getCause());
		}
		return PersistenceManagerFactoryUtils.convertJdoAccessException(ex);
	}

	/**
	 * Template method for extracting a SQL String from the given exception.
	 * <p>Default implementation always returns {@code null}. Can be overridden in
	 * subclasses to extract SQL Strings for vendor-specific exception classes.
	 * @param ex the JDOException, containing a SQLException
	 * @return the SQL String, or {@code null} if none found
	 */
	protected String extractSqlStringFromException(JDOException ex) {
		return null;
	}


	/**
	 * ConnectionHandle implementation that fetches a new JDO2 DataStoreConnection
	 * for every {@code getConnection} call and closes the Connection on
	 * {@code releaseConnection}. This is necessary because JDO2 requires the
	 * fetched Connection to be closed before continuing PersistenceManager work.
	 * @see javax.jdo.PersistenceManager#getDataStoreConnection()
	 */
	private static class DataStoreConnectionHandle implements ConnectionHandle {

		private final PersistenceManager persistenceManager;

		public DataStoreConnectionHandle(PersistenceManager persistenceManager) {
			this.persistenceManager = persistenceManager;
		}

		public Connection getConnection() {
			return (Connection) this.persistenceManager.getDataStoreConnection();
		}

		public void releaseConnection(Connection con) {
			JdbcUtils.closeConnection(con);
		}
	}

}
