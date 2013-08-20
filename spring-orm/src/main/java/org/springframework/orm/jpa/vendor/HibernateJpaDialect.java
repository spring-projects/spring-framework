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

package org.springframework.orm.jpa.vendor;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

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
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.orm.jpa.JpaDialect} implementation for
 * Hibernate EntityManager. Developed against Hibernate 3.6 and 4.2.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class HibernateJpaDialect extends DefaultJpaDialect {

	private static Class<?> optimisticLockExceptionClass;

	private static Class<?> pessimisticLockExceptionClass;

	static {
		// Checking for Hibernate 4.x's Optimistic/PessimisticEntityLockException
		ClassLoader cl = HibernateJpaDialect.class.getClassLoader();
		try {
			optimisticLockExceptionClass = cl.loadClass("org.hibernate.dialect.lock.OptimisticEntityLockException");
		}
		catch (ClassNotFoundException ex) {
			optimisticLockExceptionClass = OptimisticLockException.class;
		}
		try {
			pessimisticLockExceptionClass = cl.loadClass("org.hibernate.dialect.lock.PessimisticEntityLockException");
		}
		catch (ClassNotFoundException ex) {
			pessimisticLockExceptionClass = null;
		}
	}


	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			getSession(entityManager).getTransaction().setTimeout(definition.getTimeout());
		}
		super.beginTransaction(entityManager, definition);
		return prepareTransaction(entityManager, definition.isReadOnly(), definition.getName());
	}

	@Override
	public Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
			throws PersistenceException {

		Session session = getSession(entityManager);
		FlushMode flushMode = session.getFlushMode();
		FlushMode previousFlushMode = null;
		if (readOnly) {
			// We should suppress flushing for a read-only transaction.
			session.setFlushMode(FlushMode.MANUAL);
			previousFlushMode = flushMode;
		}
		else {
			// We need AUTO or COMMIT for a non-read-only transaction.
			if (flushMode.lessThan(FlushMode.COMMIT)) {
				session.setFlushMode(FlushMode.AUTO);
				previousFlushMode = flushMode;
			}
		}
		return new SessionTransactionData(session, previousFlushMode);
	}

	@Override
	public void cleanupTransaction(Object transactionData) {
		((SessionTransactionData) transactionData).resetFlushMode();
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

	protected Session getSession(EntityManager em) {
		return em.unwrap(Session.class);
	}


	private static class SessionTransactionData {

		private final Session session;

		private final FlushMode previousFlushMode;

		public SessionTransactionData(Session session, FlushMode previousFlushMode) {
			this.session = session;
			this.previousFlushMode = previousFlushMode;
		}

		public void resetFlushMode() {
			if (this.previousFlushMode != null) {
				this.session.setFlushMode(this.previousFlushMode);
			}
		}
	}


	private static class HibernateConnectionHandle implements ConnectionHandle {

		private final Session session;

		// This will find a corresponding method on Hibernate 3.x but not on 4.x
		private static final Method sessionConnectionMethod =
				ClassUtils.getMethodIfAvailable(Session.class, "connection");

		private static volatile Method connectionMethodToUse = sessionConnectionMethod;

		public HibernateConnectionHandle(Session session) {
			this.session = session;
		}

		@Override
		public Connection getConnection() {
			try {
				if (connectionMethodToUse == null) {
					// Reflective lookup trying to find SessionImpl's connection() on Hibernate 4.x
					connectionMethodToUse = this.session.getClass().getMethod("connection");
				}
				return (Connection) ReflectionUtils.invokeMethod(connectionMethodToUse, this.session);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Cannot find connection() method on Hibernate Session", ex);
			}
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
	}

}
