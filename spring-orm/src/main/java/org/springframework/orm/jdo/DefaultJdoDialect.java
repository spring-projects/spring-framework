/*
 * Copyright 2002-2013 the original author or authors.
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

import java.sql.Connection;
import java.sql.SQLException;
import javax.jdo.Constants;
import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * Default implementation of the {@link JdoDialect} interface.
 * As of Spring 4.0, designed for JDO 3.0 (or rather, semantics beyond JDO 3.0).
 * Used as default dialect by {@link JdoTransactionManager}.
 *
 * <p>Simply begins a standard JDO transaction in {@code beginTransaction}.
 * Returns a handle for a JDO DataStoreConnection on {@code getJdbcConnection}.
 * Calls the corresponding JDO PersistenceManager operation on {@code flush}
 * Uses a Spring SQLExceptionTranslator for exception translation, if applicable.
 *
 * <p>Note that, even with JDO 3.0, vendor-specific subclasses are still necessary
 * for special transaction semantics and more sophisticated exception translation.
 * Furthermore, vendor-specific subclasses are encouraged to expose the native JDBC
 * Connection on {@code getJdbcConnection}, rather than JDO 3.0's wrapper handle.
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
 * @see JdoTransactionManager#setJdoDialect
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 */
public class DefaultJdoDialect implements JdoDialect, PersistenceExceptionTranslator {

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
	 * This implementation invokes the standard JDO {@link Transaction#begin()}
	 * method and also {@link Transaction#setIsolationLevel(String)} if necessary.
	 * @see javax.jdo.Transaction#begin
	 * @see org.springframework.transaction.InvalidIsolationLevelException
	 */
	@Override
	public Object beginTransaction(Transaction transaction, TransactionDefinition definition)
			throws JDOException, SQLException, TransactionException {

		String jdoIsolationLevel = getJdoIsolationLevel(definition);
		if (jdoIsolationLevel != null) {
			transaction.setIsolationLevel(jdoIsolationLevel);
		}
		transaction.begin();
		return null;
	}

	/**
	 * Determine the JDO isolation level String to use for the given
	 * Spring transaction definition.
	 * @param definition the Spring transaction definition
	 * @return the corresponding JDO isolation level String, or {@code null}
	 * to indicate that no isolation level should be set explicitly
	 * @see Transaction#setIsolationLevel(String)
	 * @see Constants#TX_SERIALIZABLE
	 * @see Constants#TX_REPEATABLE_READ
	 * @see Constants#TX_READ_COMMITTED
	 * @see Constants#TX_READ_UNCOMMITTED
	 */
	protected String getJdoIsolationLevel(TransactionDefinition definition) {
		switch (definition.getIsolationLevel()) {
			case TransactionDefinition.ISOLATION_SERIALIZABLE:
				return Constants.TX_SERIALIZABLE;
			case TransactionDefinition.ISOLATION_REPEATABLE_READ:
				return Constants.TX_REPEATABLE_READ;
			case TransactionDefinition.ISOLATION_READ_COMMITTED:
				return Constants.TX_READ_COMMITTED;
			case TransactionDefinition.ISOLATION_READ_UNCOMMITTED:
				return Constants.TX_READ_UNCOMMITTED;
			default:
				return null;
		}
	}

	/**
	 * This implementation does nothing, as the default beginTransaction implementation
	 * does not require any cleanup.
	 * @see #beginTransaction
	 */
	@Override
	public void cleanupTransaction(Object transactionData) {
	}

	/**
	 * This implementation returns a DataStoreConnectionHandle for JDO.
	 * <p><b>NOTE:</b> A JDO DataStoreConnection is always a wrapper,
	 * never the native JDBC Connection. If you need access to the native JDBC
	 * Connection (or the connection pool handle, to be unwrapped via a Spring
	 * NativeJdbcExtractor), override this method to return the native
	 * Connection through the corresponding vendor-specific mechanism.
	 * <p>A JDO DataStoreConnection is only "borrowed" from the PersistenceManager:
	 * it needs to be returned as early as possible. Effectively, JDO requires the
	 * fetched Connection to be closed before continuing PersistenceManager work.
	 * For this reason, the exposed ConnectionHandle eagerly releases its JDBC
	 * Connection at the end of each JDBC data access operation (that is, on
	 * {@code DataSourceUtils.releaseConnection}).
	 * @see javax.jdo.PersistenceManager#getDataStoreConnection()
	 * @see org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
	 */
	@Override
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
	@Override
	public void releaseJdbcConnection(ConnectionHandle conHandle, PersistenceManager pm)
			throws JDOException, SQLException {
	}


	//-----------------------------------------------------------------------------------
	// Hook for exception translation (used by JdoTransactionManager)
	//-----------------------------------------------------------------------------------

	/**
	 * Implementation of the PersistenceExceptionTranslator interface,
	 * as autodetected by Spring's PersistenceExceptionTranslationPostProcessor.
	 * <p>Converts the exception if it is a JDOException, using this JdoDialect.
	 * Else returns {@code null} to indicate an unknown exception.
	 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
	 * @see #translateException
	 */
	@Override
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
	@Override
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
	 * ConnectionHandle implementation that fetches a new JDO DataStoreConnection
	 * for every {@code getConnection} call and closes the Connection on
	 * {@code releaseConnection}. This is necessary because JDO requires the
	 * fetched Connection to be closed before continuing PersistenceManager work.
	 * @see javax.jdo.PersistenceManager#getDataStoreConnection()
	 */
	private static class DataStoreConnectionHandle implements ConnectionHandle {

		private final PersistenceManager persistenceManager;

		public DataStoreConnectionHandle(PersistenceManager persistenceManager) {
			this.persistenceManager = persistenceManager;
		}

		@Override
		public Connection getConnection() {
			return (Connection) this.persistenceManager.getDataStoreConnection();
		}

		@Override
		public void releaseConnection(Connection con) {
			JdbcUtils.closeConnection(con);
		}
	}

}
