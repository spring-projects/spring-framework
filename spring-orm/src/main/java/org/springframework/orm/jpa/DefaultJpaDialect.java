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

package org.springframework.orm.jpa;

import java.io.Serializable;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * Default implementation of the {@link JpaDialect} interface.
 * Used as default dialect by {@link JpaAccessor} and {@link JpaTransactionManager}.
 *
 * <p>Simply begins a standard JPA transaction in {@link #beginTransaction}
 * and performs standard exception translation through {@link EntityManagerFactoryUtils}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see JpaAccessor#setJpaDialect
 * @see JpaTransactionManager#setJpaDialect
 */
@SuppressWarnings("serial")
public class DefaultJpaDialect implements JpaDialect, Serializable {

	//-------------------------------------------------------------------------
	// Hooks for transaction management (used by JpaTransactionManager)
	//-------------------------------------------------------------------------

	/**
	 * This implementation invokes the standard JPA {@code Transaction.begin}
	 * method. Throws an InvalidIsolationLevelException if a non-default isolation
	 * level is set.
	 * <p>This implementation does not return any transaction data Object, since there
	 * is no state to be kept for a standard JPA transaction. Hence, subclasses do not
	 * have to care about the return value ({@code null}) of this implementation
	 * and are free to return their own transaction data Object.
	 * @see javax.persistence.EntityTransaction#begin
	 * @see org.springframework.transaction.InvalidIsolationLevelException
	 * @see #cleanupTransaction
	 */
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException(
					"Standard JPA does not support custom isolation levels - " +
					"use a special JpaDialect for your JPA implementation");
		}
		entityManager.getTransaction().begin();
		return null;
	}

	public Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
			throws PersistenceException {

		return null;
	}

	/**
	 * This implementation does nothing, since the default {@code beginTransaction}
	 * implementation does not require any cleanup.
	 * @see #beginTransaction
	 */
	public void cleanupTransaction(Object transactionData) {
	}

	/**
	 * This implementation always returns {@code null},
	 * indicating that no JDBC Connection can be provided.
	 */
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		return null;
	}

	/**
	 * This implementation does nothing, assuming that the Connection
	 * will implicitly be closed with the EntityManager.
	 * <p>If the JPA implementation returns a Connection handle that it expects
	 * the application to close after use, the dialect implementation needs to invoke
	 * {@code Connection.close()} (or some other method with similar effect) here.
	 * @see java.sql.Connection#close()
	 */
	public void releaseJdbcConnection(ConnectionHandle conHandle, EntityManager em)
			throws PersistenceException, SQLException {
	}


	//-----------------------------------------------------------------------------------
	// Hook for exception translation (used by JpaTransactionManager and JpaTemplate)
	//-----------------------------------------------------------------------------------

	/**
	 * This implementation delegates to EntityManagerFactoryUtils.
	 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
	 */
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
	}


	public boolean supportsEntityManagerFactoryPlusOperations() {
		return false;
	}

	public boolean supportsEntityManagerPlusOperations() {
		return false;
	}

	public EntityManagerFactoryPlusOperations getEntityManagerFactoryPlusOperations(EntityManagerFactory rawEntityManager) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support EntityManagerFactoryPlusOperations");
	}

	public EntityManagerPlusOperations getEntityManagerPlusOperations(EntityManager rawEntityManager) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support EntityManagerPlusOperations");
	}

}
