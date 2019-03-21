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

package org.springframework.orm.jpa;

import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * SPI strategy that encapsulates certain functionality that standard JPA 2.1 does
 * not offer, such as access to the underlying JDBC Connection. This strategy is
 * mainly intended for standalone usage of a JPA provider; most of its functionality
 * is not relevant when running with JTA transactions.
 *
 * <p>In general, it is recommended to derive from {@link DefaultJpaDialect} instead
 * of implementing this interface directly. This allows for inheriting common behavior
 * (present and future) from DefaultJpaDialect, only overriding specific hooks to
 * plug in concrete vendor-specific behavior.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see DefaultJpaDialect
 * @see JpaTransactionManager#setJpaDialect
 * @see JpaVendorAdapter#getJpaDialect()
 * @see AbstractEntityManagerFactoryBean#setJpaDialect
 * @see AbstractEntityManagerFactoryBean#setJpaVendorAdapter
 */
public interface JpaDialect extends PersistenceExceptionTranslator {

	/**
	 * Begin the given JPA transaction, applying the semantics specified by the
	 * given Spring transaction definition (in particular, an isolation level
	 * and a timeout). Called by JpaTransactionManager on transaction begin.
	 * <p>An implementation can configure the JPA Transaction object and then
	 * invoke {@code begin}, or invoke a special begin method that takes,
	 * for example, an isolation level.
	 * <p>An implementation can apply the read-only flag as flush mode. In that case,
	 * a transaction data object can be returned that holds the previous flush mode
	 * (and possibly other data), to be reset in {@code cleanupTransaction}.
	 * It may also apply the read-only flag and isolation level to the underlying
	 * JDBC Connection before beginning the transaction.
	 * <p>Implementations can also use the Spring transaction name, as exposed by the
	 * passed-in TransactionDefinition, to optimize for specific data access use cases
	 * (effectively using the current transaction name as use case identifier).
	 * <p>This method also allows for exposing savepoint capabilities if supported by
	 * the persistence provider, through returning an Object that implements Spring's
	 * {@link org.springframework.transaction.SavepointManager} interface.
	 * {@link JpaTransactionManager} will use this capability if needed.
	 * @param entityManager the EntityManager to begin a JPA transaction on
	 * @param definition the Spring transaction definition that defines semantics
	 * @return an arbitrary object that holds transaction data, if any
	 * (to be passed into {@link #cleanupTransaction}). May implement the
	 * {@link org.springframework.transaction.SavepointManager} interface.
	 * @throws javax.persistence.PersistenceException if thrown by JPA methods
	 * @throws java.sql.SQLException if thrown by JDBC methods
	 * @throws org.springframework.transaction.TransactionException in case of invalid arguments
	 * @see #cleanupTransaction
	 * @see javax.persistence.EntityTransaction#begin
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#prepareConnectionForTransaction
	 */
	@Nullable
	Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException;

	/**
	 * Prepare a JPA transaction, applying the specified semantics. Called by
	 * EntityManagerFactoryUtils when enlisting an EntityManager in a JTA transaction
	 * or a locally joined transaction (e.g. after upgrading an unsynchronized
	 * EntityManager to a synchronized one).
	 * <p>An implementation can apply the read-only flag as flush mode. In that case,
	 * a transaction data object can be returned that holds the previous flush mode
	 * (and possibly other data), to be reset in {@code cleanupTransaction}.
	 * <p>Implementations can also use the Spring transaction name to optimize for
	 * specific data access use cases (effectively using the current transaction
	 * name as use case identifier).
	 * @param entityManager the EntityManager to begin a JPA transaction on
	 * @param readOnly whether the transaction is supposed to be read-only
	 * @param name the name of the transaction (if any)
	 * @return an arbitrary object that holds transaction data, if any
	 * (to be passed into cleanupTransaction)
	 * @throws javax.persistence.PersistenceException if thrown by JPA methods
	 * @see #cleanupTransaction
	 */
	@Nullable
	Object prepareTransaction(EntityManager entityManager, boolean readOnly, @Nullable String name)
			throws PersistenceException;

	/**
	 * Clean up the transaction via the given transaction data. Called by
	 * JpaTransactionManager and EntityManagerFactoryUtils on transaction cleanup.
	 * <p>An implementation can, for example, reset read-only flag and
	 * isolation level of the underlying JDBC Connection. Furthermore,
	 * an exposed data access use case can be reset here.
	 * @param transactionData arbitrary object that holds transaction data, if any
	 * (as returned by beginTransaction or prepareTransaction)
	 * @see #beginTransaction
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#resetConnectionAfterTransaction
	 */
	void cleanupTransaction(@Nullable Object transactionData);

	/**
	 * Retrieve the JDBC Connection that the given JPA EntityManager uses underneath,
	 * if accessing a relational database. This method will just get invoked if actually
	 * needing access to the underlying JDBC Connection, usually within an active JPA
	 * transaction (for example, by JpaTransactionManager). The returned handle will
	 * be passed into the {@code releaseJdbcConnection} method when not needed anymore.
	 * <p>This strategy is necessary as JPA does not provide a standard way to retrieve
	 * the underlying JDBC Connection (due to the fact that a JPA implementation might not
	 * work with a relational database at all).
	 * <p>Implementations are encouraged to return an unwrapped Connection object, i.e.
	 * the Connection as they got it from the connection pool. This makes it easier for
	 * application code to get at the underlying native JDBC Connection, like an
	 * OracleConnection, which is sometimes necessary for LOB handling etc. We assume
	 * that calling code knows how to properly handle the returned Connection object.
	 * <p>In a simple case where the returned Connection will be auto-closed with the
	 * EntityManager or can be released via the Connection object itself, an
	 * implementation can return a SimpleConnectionHandle that just contains the
	 * Connection. If some other object is needed in {@code releaseJdbcConnection},
	 * an implementation should use a special handle that references that other object.
	 * @param entityManager the current JPA EntityManager
	 * @param readOnly whether the Connection is only needed for read-only purposes
	 * @return a handle for the Connection, to be passed into {@code releaseJdbcConnection},
	 * or {@code null} if no JDBC Connection can be retrieved
	 * @throws javax.persistence.PersistenceException if thrown by JPA methods
	 * @throws java.sql.SQLException if thrown by JDBC methods
	 * @see #releaseJdbcConnection
	 * @see org.springframework.jdbc.datasource.ConnectionHandle#getConnection
	 * @see org.springframework.jdbc.datasource.SimpleConnectionHandle
	 * @see JpaTransactionManager#setDataSource
	 */
	@Nullable
	ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException;

	/**
	 * Release the given JDBC Connection, which has originally been retrieved
	 * via {@code getJdbcConnection}. This should be invoked in any case,
	 * to allow for proper release of the retrieved Connection handle.
	 * <p>An implementation might simply do nothing, if the Connection returned
	 * by {@code getJdbcConnection} will be implicitly closed when the JPA
	 * transaction completes or when the EntityManager is closed.
	 * @param conHandle the JDBC Connection handle to release
	 * @param entityManager the current JPA EntityManager
	 * @throws javax.persistence.PersistenceException if thrown by JPA methods
	 * @throws java.sql.SQLException if thrown by JDBC methods
	 * @see #getJdbcConnection
	 */
	void releaseJdbcConnection(ConnectionHandle conHandle, EntityManager entityManager)
			throws PersistenceException, SQLException;

}
