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

package org.springframework.orm.jpa.vendor;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.OptimisticLockException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PessimisticLockException;
import org.hibernate.PropertyValueException;
import org.hibernate.QueryException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.orm.jpa.JpaDialect} implementation for
 * Hibernate EntityManager. Developed and tested against Hibernate 3.6,
 * 4.2/4.3 as well as 5.0/5.1/5.2.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.0
 * @see HibernateJpaVendorAdapter
 * @see org.hibernate.Session#setFlushMode
 * @see org.hibernate.Transaction#setTimeout
 */
@SuppressWarnings("serial")
public class HibernateJpaDialect extends DefaultJpaDialect {

	private static Class<?> optimisticLockExceptionClass;

	private static Class<?> pessimisticLockExceptionClass;

	private static Method getFlushMode;

	static {
		// Checking for Hibernate 4.x's Optimistic/PessimisticEntityLockException
		ClassLoader cl = HibernateJpaDialect.class.getClassLoader();
		try {
			optimisticLockExceptionClass = cl.loadClass("org.hibernate.dialect.lock.OptimisticEntityLockException");
		}
		catch (ClassNotFoundException ex) {
			// OptimisticLockException is deprecated on Hibernate 4.x; we're just using it on 3.x anyway
			optimisticLockExceptionClass = OptimisticLockException.class;
		}
		try {
			pessimisticLockExceptionClass = cl.loadClass("org.hibernate.dialect.lock.PessimisticEntityLockException");
		}
		catch (ClassNotFoundException ex) {
			pessimisticLockExceptionClass = null;
		}

		try {
			// Hibernate 5.2+ getHibernateFlushMode()
			getFlushMode = Session.class.getMethod("getHibernateFlushMode");
		}
		catch (NoSuchMethodException ex) {
			try {
				// Classic Hibernate getFlushMode() with FlushMode return type
				getFlushMode = Session.class.getMethod("getFlushMode");
			}
			catch (NoSuchMethodException ex2) {
				throw new IllegalStateException("No compatible Hibernate getFlushMode signature found", ex2);
			}
		}
		// Check that it is the Hibernate FlushMode type, not JPA's...
		Assert.state(FlushMode.class == getFlushMode.getReturnType());
	}


	boolean prepareConnection = (HibernateConnectionHandle.sessionConnectionMethod == null);


	/**
	 * Set whether to prepare the underlying JDBC Connection of a transactional
	 * Hibernate Session, that is, whether to apply a transaction-specific
	 * isolation level and/or the transaction's read-only flag to the underlying
	 * JDBC Connection.
	 * <p>Default is "true" on Hibernate EntityManager 4.x (with its 'on-close'
	 * connection release mode, and "false" on Hibernate EntityManager 3.6 (due to
	 * the 'after-transaction' release mode there). <b>Note that Hibernate 4.2+ is
	 * strongly recommended in order to make isolation levels work efficiently.</b>
	 * <p>If you turn this flag off, JPA transaction management will not support
	 * per-transaction isolation levels anymore. It will not call
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


	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		Session session = getSession(entityManager);

		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			session.getTransaction().setTimeout(definition.getTimeout());
		}

		boolean isolationLevelNeeded = (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT);
		Integer previousIsolationLevel = null;
		Connection preparedCon = null;

		if (isolationLevelNeeded || definition.isReadOnly()) {
			if (this.prepareConnection) {
				preparedCon = HibernateConnectionHandle.doGetConnection(session);
				previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(preparedCon, definition);
			}
			else if (isolationLevelNeeded) {
				throw new InvalidIsolationLevelException(getClass().getSimpleName() +
						" does not support custom isolation levels since the 'prepareConnection' flag is off. " +
						"This is the case on Hibernate 3.6 by default; either switch that flag at your own risk " +
						"or upgrade to Hibernate 4.x, with 4.2+ recommended.");
			}
		}

		// Standard JPA transaction begin call for full JPA context setup...
		entityManager.getTransaction().begin();

		// Adapt flush mode and store previous isolation level, if any.
		FlushMode previousFlushMode = prepareFlushMode(session, definition.isReadOnly());
		return new SessionTransactionData(session, previousFlushMode, preparedCon, previousIsolationLevel);
	}

	@Override
	public Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
			throws PersistenceException {

		Session session = getSession(entityManager);
		FlushMode previousFlushMode = prepareFlushMode(session, readOnly);
		return new SessionTransactionData(session, previousFlushMode, null, null);
	}

	protected FlushMode prepareFlushMode(Session session, boolean readOnly) throws PersistenceException {
		FlushMode flushMode = (FlushMode) ReflectionUtils.invokeMethod(getFlushMode, session);
		if (readOnly) {
			// We should suppress flushing for a read-only transaction.
			if (!flushMode.equals(FlushMode.MANUAL)) {
				session.setFlushMode(FlushMode.MANUAL);
				return flushMode;
			}
		}
		else {
			// We need AUTO or COMMIT for a non-read-only transaction.
			if (flushMode.lessThan(FlushMode.COMMIT)) {
				session.setFlushMode(FlushMode.AUTO);
				return flushMode;
			}
		}
		// No FlushMode change needed...
		return null;
	}

	@Override
	public void cleanupTransaction(Object transactionData) {
		((SessionTransactionData) transactionData).resetSessionState();
	}

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		Session session = getSession(entityManager);
		return new HibernateConnectionHandle(session);
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof HibernateException) {
			return convertHibernateAccessException((HibernateException) ex);
		}
		if (ex instanceof PersistenceException && ex.getCause() instanceof HibernateException) {
			return convertHibernateAccessException((HibernateException) ex.getCause());
		}
		return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
	}

	/**
	 * Convert the given HibernateException to an appropriate exception
	 * from the {@code org.springframework.dao} hierarchy.
	 * @param ex HibernateException that occurred
	 * @return the corresponding DataAccessException instance
	 */
	protected DataAccessException convertHibernateAccessException(HibernateException ex) {
		if (ex instanceof JDBCConnectionException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}
		if (ex instanceof SQLGrammarException) {
			SQLGrammarException jdbcEx = (SQLGrammarException) ex;
			return new InvalidDataAccessResourceUsageException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof QueryTimeoutException) {
			QueryTimeoutException jdbcEx = (QueryTimeoutException) ex;
			return new org.springframework.dao.QueryTimeoutException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof LockAcquisitionException) {
			LockAcquisitionException jdbcEx = (LockAcquisitionException) ex;
			return new CannotAcquireLockException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof PessimisticLockException) {
			PessimisticLockException jdbcEx = (PessimisticLockException) ex;
			return new PessimisticLockingFailureException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof ConstraintViolationException) {
			ConstraintViolationException jdbcEx = (ConstraintViolationException) ex;
			return new DataIntegrityViolationException(ex.getMessage()  + "; SQL [" + jdbcEx.getSQL() +
					"]; constraint [" + jdbcEx.getConstraintName() + "]", ex);
		}
		if (ex instanceof DataException) {
			DataException jdbcEx = (DataException) ex;
			return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		// end of JDBCException subclass handling

		if (ex instanceof QueryException) {
			return new InvalidDataAccessResourceUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof NonUniqueResultException) {
			return new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
		}
		if (ex instanceof NonUniqueObjectException) {
			return new DuplicateKeyException(ex.getMessage(), ex);
		}
		if (ex instanceof PropertyValueException) {
			return new DataIntegrityViolationException(ex.getMessage(), ex);
		}
		if (ex instanceof PersistentObjectException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof TransientObjectException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof ObjectDeletedException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof UnresolvableObjectException) {
			UnresolvableObjectException hibEx = (UnresolvableObjectException) ex;
			return new ObjectRetrievalFailureException(hibEx.getEntityName(), hibEx.getIdentifier(), ex.getMessage(), ex);
		}
		if (ex instanceof WrongClassException) {
			WrongClassException hibEx = (WrongClassException) ex;
			return new ObjectRetrievalFailureException(hibEx.getEntityName(), hibEx.getIdentifier(), ex.getMessage(), ex);
		}
		if (ex instanceof StaleObjectStateException) {
			StaleObjectStateException hibEx = (StaleObjectStateException) ex;
			return new ObjectOptimisticLockingFailureException(hibEx.getEntityName(), hibEx.getIdentifier(), ex);
		}
		if (ex instanceof StaleStateException) {
			return new ObjectOptimisticLockingFailureException(ex.getMessage(), ex);
		}
		if (optimisticLockExceptionClass.isInstance(ex)) {
			return new ObjectOptimisticLockingFailureException(ex.getMessage(), ex);
		}
		if (pessimisticLockExceptionClass != null && pessimisticLockExceptionClass.isInstance(ex)) {
			if (ex.getCause() instanceof LockAcquisitionException) {
				return new CannotAcquireLockException(ex.getMessage(), ex.getCause());
			}
			return new PessimisticLockingFailureException(ex.getMessage(), ex);
		}

		// fallback
		return new JpaSystemException(ex);
	}

	protected Session getSession(EntityManager entityManager) {
		return entityManager.unwrap(Session.class);
	}


	private static class SessionTransactionData {

		private final Session session;

		private final FlushMode previousFlushMode;

		private final Connection preparedCon;

		private final Integer previousIsolationLevel;

		public SessionTransactionData(
				Session session, FlushMode previousFlushMode, Connection preparedCon, Integer previousIsolationLevel) {
			this.session = session;
			this.previousFlushMode = previousFlushMode;
			this.preparedCon = preparedCon;
			this.previousIsolationLevel = previousIsolationLevel;
		}

		public void resetSessionState() {
			if (this.previousFlushMode != null) {
				this.session.setFlushMode(this.previousFlushMode);
			}
			if (this.preparedCon != null && this.session.isConnected()) {
				Connection conToReset = HibernateConnectionHandle.doGetConnection(this.session);
				if (conToReset != this.preparedCon) {
					LogFactory.getLog(HibernateJpaDialect.class).warn(
							"JDBC Connection to reset not identical to originally prepared Connection - please " +
							"make sure to use connection release mode ON_CLOSE (the default) and to run against " +
							"Hibernate 4.2+ (or switch HibernateJpaDialect's prepareConnection flag to false");
				}
				DataSourceUtils.resetConnectionAfterTransaction(conToReset, this.previousIsolationLevel);
			}
		}
	}


	private static class HibernateConnectionHandle implements ConnectionHandle {

		// This will find a corresponding method on Hibernate 3.x but not on 4.x
		private static final Method sessionConnectionMethod =
				ClassUtils.getMethodIfAvailable(Session.class, "connection");

		private static volatile Method connectionMethodToUse = sessionConnectionMethod;

		private final Session session;

		public HibernateConnectionHandle(Session session) {
			this.session = session;
		}

		@Override
		public Connection getConnection() {
			return doGetConnection(this.session);
		}

		@Override
		public void releaseConnection(Connection con) {
			if (sessionConnectionMethod != null) {
				// Need to explicitly call close() with Hibernate 3.x in order to allow
				// for eager release of the underlying physical Connection if necessary.
				// However, do not do this on Hibernate 4.2+ since it would return the
				// physical Connection to the pool right away, making it unusable for
				// further operations within the current transaction!
				JdbcUtils.closeConnection(con);
			}
		}

		public static Connection doGetConnection(Session session) {
			try {
				if (connectionMethodToUse == null) {
					// Reflective lookup to find SessionImpl's connection() method on Hibernate 4.x
					connectionMethodToUse = session.getClass().getMethod("connection");
				}
				return (Connection) ReflectionUtils.invokeMethod(connectionMethodToUse, session);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Cannot find connection() method on Hibernate Session", ex);
			}
		}
	}

}
