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

package org.springframework.orm.jpa.vendor;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.orm.jpa.hibernate.HibernateExceptionTranslator;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.ResourceTransactionDefinition;

/**
 * {@link org.springframework.orm.jpa.JpaDialect} implementation for Hibernate.
 * Compatible with Hibernate ORM 7.0.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.0
 * @see HibernateJpaVendorAdapter
 * @see org.hibernate.Session#setHibernateFlushMode
 * @see org.hibernate.Transaction#setTimeout
 */
@SuppressWarnings("serial")
public class HibernateJpaDialect extends DefaultJpaDialect {

	private final HibernateExceptionTranslator exceptionTranslator = new HibernateExceptionTranslator();

	boolean prepareConnection = true;


	/**
	 * Set whether to prepare the underlying JDBC Connection of a transactional
	 * Hibernate Session, that is, whether to apply a transaction-specific
	 * isolation level and/or the transaction's read-only flag to the underlying
	 * JDBC Connection.
	 * <p>Default is "true". If you turn this flag off, JPA transaction management
	 * will not support per-transaction isolation levels anymore. It will not call
	 * {@code Connection.setReadOnly(true)} for read-only transactions anymore either.
	 * If this flag is turned off, no cleanup of a JDBC Connection is required after
	 * a transaction, since no Connection settings will get modified.
	 * <p><b>NOTE:</b> The default behavior in terms of read-only handling changed
	 * in Spring 4.1, propagating the read-only status to the JDBC Connection now,
	 * analogous to other Spring transaction managers. This may have the effect
	 * that you're running into read-only enforcement now where previously write
	 * access has accidentally been tolerated: Please revise your transaction
	 * declarations accordingly, removing invalid read-only markers if necessary.
	 * @since 4.1
	 * @see java.sql.Connection#setTransactionIsolation
	 * @see java.sql.Connection#setReadOnly
	 */
	public void setPrepareConnection(boolean prepareConnection) {
		this.prepareConnection = prepareConnection;
	}

	/**
	 * Set the JDBC exception translator for Hibernate exception translation purposes.
	 * <p>Applied to any detected {@link java.sql.SQLException} root cause of a Hibernate
	 * {@link JDBCException}, overriding Hibernate's own {@code SQLException} translation
	 * (which is based on a Hibernate Dialect for a specific target database).
	 * <p>As of 6.1, also applied to {@link org.hibernate.TransactionException} translation
	 * with a {@link SQLException} root cause (where Hibernate does not translate itself
	 * at all), overriding Spring's default {@link SQLExceptionSubclassTranslator} there.
	 * @param exceptionTranslator the {@link SQLExceptionTranslator} to delegate to, or
	 * {@code null} for none. By default, a {@link SQLExceptionSubclassTranslator} will
	 * be used for {@link org.hibernate.TransactionException} translation as of 6.1;
	 * this can be reverted to pre-6.1 behavior through setting {@code null} here.
	 * @since 5.1
	 * @see java.sql.SQLException
	 * @see org.hibernate.JDBCException
	 * @see org.springframework.jdbc.support.SQLExceptionSubclassTranslator
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 */
	public void setJdbcExceptionTranslator(@Nullable SQLExceptionTranslator exceptionTranslator) {
		this.exceptionTranslator.setJdbcExceptionTranslator(exceptionTranslator);
	}


	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		SessionImplementor session = entityManager.unwrap(SessionImplementor.class);

		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			session.getTransaction().setTimeout(definition.getTimeout());
		}

		boolean isolationLevelNeeded = (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT);
		Integer previousIsolationLevel = null;
		Connection preparedCon = null;

		if (isolationLevelNeeded || definition.isReadOnly()) {
			if (this.prepareConnection && ConnectionReleaseMode.ON_CLOSE.equals(
					session.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode().getReleaseMode())) {
				preparedCon = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
				previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(preparedCon, definition);
			}
			else if (isolationLevelNeeded) {
				throw new InvalidIsolationLevelException(
						"HibernateJpaDialect is not allowed to support custom isolation levels: " +
						"make sure that its 'prepareConnection' flag is on (the default) and that the " +
						"Hibernate connection release mode is set to ON_CLOSE.");
			}
		}

		// Standard JPA transaction begin call for full JPA context setup...
		entityManager.getTransaction().begin();

		// Adapt flush mode and store previous isolation level, if any.
		FlushMode previousFlushMode = prepareFlushMode(session, definition.isReadOnly());
		if (definition instanceof ResourceTransactionDefinition rtd && rtd.isLocalResource()) {
			// As of 5.1, we explicitly optimize for a transaction-local EntityManager,
			// aligned with native HibernateTransactionManager behavior.
			previousFlushMode = null;
			if (definition.isReadOnly()) {
				session.setDefaultReadOnly(true);
			}
		}
		return new SessionTransactionData(
				session, previousFlushMode, (preparedCon != null), previousIsolationLevel, definition.isReadOnly());
	}

	@Override
	public Object prepareTransaction(EntityManager entityManager, boolean readOnly, @Nullable String name)
			throws PersistenceException {

		SessionImplementor session = entityManager.unwrap(SessionImplementor.class);
		FlushMode previousFlushMode = prepareFlushMode(session, readOnly);
		return new SessionTransactionData(session, previousFlushMode, false, null, readOnly);
	}

	protected @Nullable FlushMode prepareFlushMode(Session session, boolean readOnly) throws PersistenceException {
		FlushMode flushMode = session.getHibernateFlushMode();
		if (readOnly) {
			// We should suppress flushing for a read-only transaction.
			if (!flushMode.equals(FlushMode.MANUAL)) {
				session.setHibernateFlushMode(FlushMode.MANUAL);
				return flushMode;
			}
		}
		else {
			// We need AUTO or COMMIT for a non-read-only transaction.
			if (flushMode.lessThan(FlushMode.COMMIT)) {
				session.setHibernateFlushMode(FlushMode.AUTO);
				return flushMode;
			}
		}
		// No FlushMode change needed...
		return null;
	}

	@Override
	public void cleanupTransaction(@Nullable Object transactionData) {
		if (transactionData instanceof SessionTransactionData sessionTransactionData) {
			sessionTransactionData.resetSessionState();
		}
	}

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		return new HibernateConnectionHandle(entityManager.unwrap(SessionImplementor.class));
	}

	@Override
	public @Nullable DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return this.exceptionTranslator.translateExceptionIfPossible(ex);
	}


	private static class SessionTransactionData {

		private final SessionImplementor session;

		private final @Nullable FlushMode previousFlushMode;

		private final boolean needsConnectionReset;

		private final @Nullable Integer previousIsolationLevel;

		private final boolean readOnly;

		public SessionTransactionData(SessionImplementor session, @Nullable FlushMode previousFlushMode,
				boolean connectionPrepared, @Nullable Integer previousIsolationLevel, boolean readOnly) {

			this.session = session;
			this.previousFlushMode = previousFlushMode;
			this.needsConnectionReset = connectionPrepared;
			this.previousIsolationLevel = previousIsolationLevel;
			this.readOnly = readOnly;
		}

		public void resetSessionState() {
			if (this.previousFlushMode != null) {
				this.session.setHibernateFlushMode(this.previousFlushMode);
			}
			if (this.needsConnectionReset &&
					this.session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected()) {
				Connection con = this.session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
				DataSourceUtils.resetConnectionAfterTransaction(
						con, this.previousIsolationLevel, this.readOnly);
			}
		}
	}


	private static class HibernateConnectionHandle implements ConnectionHandle {

		private final SessionImplementor session;

		public HibernateConnectionHandle(SessionImplementor session) {
			this.session = session;
		}

		@Override
		public Connection getConnection() {
			return this.session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
		}
	}

}
