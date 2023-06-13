/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.orm.jpa.vendor;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.UnitOfWork;

import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * {@link org.springframework.orm.jpa.JpaDialect} implementation for Eclipse
 * Persistence Services (EclipseLink). Developed and tested against EclipseLink 2.7;
 * backwards-compatible with EclipseLink 2.5 and 2.6 at runtime.
 *
 * <p>By default, this dialect acquires an early EclipseLink transaction with an
 * early JDBC Connection for non-read-only transactions. This allows for mixing
 * JDBC and JPA operations in the same transaction, with cross visibility of
 * their impact. If this is not needed, set the "lazyDatabaseTransaction" flag to
 * {@code true} or consistently declare all affected transactions as read-only.
 * As of Spring 4.1.2, this will reliably avoid early JDBC Connection retrieval
 * and therefore keep EclipseLink in shared cache mode.
 *
 * <p><b>NOTE: This dialect supports custom isolation levels with limitations.</b>
 * Consistent isolation level handling is only guaranteed when all Spring transaction
 * definitions specify a concrete isolation level, without any default transactions
 * running concurrently. Otherwise, you may see non-default isolation levels exposed
 * to default transactions when custom isolation is used in concurrent transactions.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see #setLazyDatabaseTransaction
 * @see org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
 */
@SuppressWarnings("serial")
public class EclipseLinkJpaDialect extends DefaultJpaDialect {

	private boolean lazyDatabaseTransaction = false;


	/**
	 * Set whether to lazily start a database resource transaction within a
	 * Spring-managed EclipseLink transaction.
	 * <p>By default, read-only transactions are started lazily but regular
	 * non-read-only transactions are started early. This allows for reusing the
	 * same JDBC Connection throughout an entire EclipseLink transaction, for
	 * enforced isolation and consistent visibility with JDBC access code working
	 * on the same DataSource.
	 * <p>Switch this flag to "true" to enforce a lazy database transaction begin
	 * even for non-read-only transactions, allowing access to EclipseLink's
	 * shared cache and following EclipseLink's connection mode configuration,
	 * assuming that isolation and visibility at the JDBC level are less important.
	 * @see org.eclipse.persistence.sessions.UnitOfWork#beginEarlyTransaction()
	 */
	public void setLazyDatabaseTransaction(boolean lazyDatabaseTransaction) {
		this.lazyDatabaseTransaction = lazyDatabaseTransaction;
	}


	@Override
	@Nullable
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		int currentIsolationLevel = definition.getIsolationLevel();
		if (currentIsolationLevel != TransactionDefinition.ISOLATION_DEFAULT) {
			// Pass custom isolation level on to EclipseLink's DatabaseLogin configuration
			// (since Spring 4.1.2 / revised in 5.3.28)
			UnitOfWork uow = entityManager.unwrap(UnitOfWork.class);
			DatabaseLogin databaseLogin = uow.getLogin();
			// Synchronize on shared DatabaseLogin instance for consistent isolation level
			// set and reset in case of concurrent transactions with different isolation.
			synchronized (databaseLogin) {
				int originalIsolationLevel = databaseLogin.getTransactionIsolation();
				// Apply current isolation level value, if necessary.
				if (currentIsolationLevel != originalIsolationLevel) {
					databaseLogin.setTransactionIsolation(currentIsolationLevel);
				}
				// Transaction begin including enforced JDBC Connection access
				// (picking up current isolation level from DatabaseLogin)
				entityManager.getTransaction().begin();
				uow.beginEarlyTransaction();
				entityManager.unwrap(Connection.class);
				// Restore original isolation level value, if necessary.
				if (currentIsolationLevel != originalIsolationLevel) {
					databaseLogin.setTransactionIsolation(originalIsolationLevel);
				}
			}
		}
		else {
			entityManager.getTransaction().begin();
			if (!definition.isReadOnly() && !this.lazyDatabaseTransaction) {
				// Begin an early transaction to force EclipseLink to get a JDBC Connection
				// so that Spring can manage transactions with JDBC as well as EclipseLink.
				entityManager.unwrap(UnitOfWork.class).beginEarlyTransaction();
			}
		}

		return null;
	}

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		// As of Spring 4.1.2, we're using a custom ConnectionHandle for lazy retrieval
		// of the underlying Connection (allowing for deferred internal transaction begin
		// within the EclipseLink EntityManager)
		return new EclipseLinkConnectionHandle(entityManager);
	}


	/**
	 * {@link ConnectionHandle} implementation that lazily fetches an
	 * EclipseLink-provided Connection on the first {@code getConnection} call -
	 * which may never come if no application code requests a JDBC Connection.
	 * This is useful to defer the early transaction begin that obtaining a
	 * JDBC Connection implies within an EclipseLink EntityManager.
	 */
	private static class EclipseLinkConnectionHandle implements ConnectionHandle {

		private final EntityManager entityManager;

		@Nullable
		private Connection connection;

		public EclipseLinkConnectionHandle(EntityManager entityManager) {
			this.entityManager = entityManager;
		}

		@Override
		public Connection getConnection() {
			if (this.connection == null) {
				this.connection = this.entityManager.unwrap(Connection.class);
			}
			return this.connection;
		}
	}

}
