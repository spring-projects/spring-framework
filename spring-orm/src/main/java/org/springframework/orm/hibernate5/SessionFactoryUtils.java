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

package org.springframework.orm.hibernate5;

import java.lang.reflect.Method;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.persistence.PersistenceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PessimisticLockException;
import org.hibernate.PropertyValueException;
import org.hibernate.QueryException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.service.UnknownServiceException;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Helper class featuring methods for Hibernate Session handling.
 * Also provides support for exception translation.
 *
 * <p>Used internally by {@link HibernateTransactionManager}.
 * Can also be used directly in application code.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see HibernateExceptionTranslator
 * @see HibernateTransactionManager
 */
public abstract class SessionFactoryUtils {

	/**
	 * Order value for TransactionSynchronization objects that clean up Hibernate Sessions.
	 * Returns {@code DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100}
	 * to execute Session cleanup before JDBC Connection cleanup, if any.
	 * @see DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
	 */
	public static final int SESSION_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

	static final Log logger = LogFactory.getLog(SessionFactoryUtils.class);


	/**
	 * Trigger a flush on the given Hibernate Session, converting regular
	 * {@link HibernateException} instances as well as Hibernate 5.2's
	 * {@link PersistenceException} wrappers accordingly.
	 * @param session the Hibernate Session to flush
	 * @param synch whether this flush is triggered by transaction synchronization
	 * @throws DataAccessException in case of flush failures
	 * @since 4.3.2
	 */
	static void flush(Session session, boolean synch) throws DataAccessException {
		if (synch) {
			logger.debug("Flushing Hibernate Session on transaction synchronization");
		}
		else {
			logger.debug("Flushing Hibernate Session on explicit request");
		}
		try {
			session.flush();
		}
		catch (HibernateException ex) {
			throw convertHibernateAccessException(ex);
		}
		catch (PersistenceException ex) {
			if (ex.getCause() instanceof HibernateException hibernateException) {
				throw convertHibernateAccessException(hibernateException);
			}
			throw ex;
		}

	}

	/**
	 * Perform actual closing of the Hibernate Session,
	 * catching and logging any cleanup exceptions thrown.
	 * @param session the Hibernate Session to close (may be {@code null})
	 * @see Session#close()
	 */
	public static void closeSession(@Nullable Session session) {
		if (session != null) {
			try {
				if (session.isOpen()) {
					session.close();
				}
			}
			catch (Throwable ex) {
				logger.error("Failed to release Hibernate Session", ex);
			}
		}
	}

	/**
	 * Determine the DataSource of the given SessionFactory.
	 * @param sessionFactory the SessionFactory to check
	 * @return the DataSource, or {@code null} if none found
	 * @see ConnectionProvider
	 */
	@Nullable
	public static DataSource getDataSource(SessionFactory sessionFactory) {
		Method getProperties = ClassUtils.getMethodIfAvailable(sessionFactory.getClass(), "getProperties");
		if (getProperties != null) {
			Map<?, ?> props = (Map<?, ?>) ReflectionUtils.invokeMethod(getProperties, sessionFactory);
			if (props != null) {
				Object dataSourceValue = props.get(Environment.DATASOURCE);
				if (dataSourceValue instanceof DataSource dataSource) {
					return dataSource;
				}
			}
		}
		if (sessionFactory instanceof SessionFactoryImplementor sfi) {
			try {
				ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);
				if (cp != null) {
					return cp.unwrap(DataSource.class);
				}
			}
			catch (UnknownServiceException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No ConnectionProvider found - cannot determine DataSource for SessionFactory: " + ex);
				}
			}
		}
		return null;
	}

	/**
	 * Convert the given HibernateException to an appropriate exception
	 * from the {@code org.springframework.dao} hierarchy.
	 * @param ex the HibernateException that occurred
	 * @return the corresponding DataAccessException instance
	 * @see HibernateExceptionTranslator#convertHibernateAccessException
	 * @see HibernateTransactionManager#convertHibernateAccessException
	 */
	public static DataAccessException convertHibernateAccessException(HibernateException ex) {
		if (ex instanceof JDBCConnectionException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}
		if (ex instanceof SQLGrammarException hibJdbcEx) {
			return new InvalidDataAccessResourceUsageException(ex.getMessage() + "; SQL [" + hibJdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof QueryTimeoutException hibJdbcEx) {
			return new org.springframework.dao.QueryTimeoutException(ex.getMessage() + "; SQL [" + hibJdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof LockAcquisitionException hibJdbcEx) {
			return new CannotAcquireLockException(ex.getMessage() + "; SQL [" + hibJdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof PessimisticLockException hibJdbcEx) {
			return new PessimisticLockingFailureException(ex.getMessage() + "; SQL [" + hibJdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof ConstraintViolationException hibJdbcEx) {
			return new DataIntegrityViolationException(ex.getMessage()  + "; SQL [" + hibJdbcEx.getSQL() +
					"]; constraint [" + hibJdbcEx.getConstraintName() + "]", ex);
		}
		if (ex instanceof DataException hibJdbcEx) {
			return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + hibJdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof JDBCException hibJdbcEx) {
			return new HibernateJdbcException(hibJdbcEx);
		}
		// end of JDBCException (subclass) handling

		if (ex instanceof QueryException queryException) {
			return new HibernateQueryException(queryException);
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
		if (ex instanceof UnresolvableObjectException unresolvableObjectException) {
			return new HibernateObjectRetrievalFailureException(unresolvableObjectException);
		}
		if (ex instanceof WrongClassException wrongClassException) {
			return new HibernateObjectRetrievalFailureException(wrongClassException);
		}
		if (ex instanceof StaleObjectStateException staleObjectStateException) {
			return new HibernateOptimisticLockingFailureException(staleObjectStateException);
		}
		if (ex instanceof StaleStateException staleStateException) {
			return new HibernateOptimisticLockingFailureException(staleStateException);
		}
		if (ex instanceof OptimisticEntityLockException optimisticEntityLockException) {
			return new HibernateOptimisticLockingFailureException(optimisticEntityLockException);
		}
		if (ex instanceof PessimisticEntityLockException) {
			if (ex.getCause() instanceof LockAcquisitionException) {
				return new CannotAcquireLockException(ex.getMessage(), ex.getCause());
			}
			return new PessimisticLockingFailureException(ex.getMessage(), ex);
		}

		// fallback
		return new HibernateSystemException(ex);
	}

}
