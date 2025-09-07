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

package org.springframework.orm.jpa.hibernate;

import java.sql.SQLException;

import jakarta.persistence.PersistenceException;
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
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.jspecify.annotations.Nullable;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaSystemException;

/**
 * {@link PersistenceExceptionTranslator} capable of translating {@link HibernateException}
 * and standard JPA {@link PersistenceException} instances to Spring'
 * {@link DataAccessException} hierarchy.
 *
 * <p>Extended by {@link LocalSessionFactoryBean}, so there is no need to declare this
 * translator in addition to a {@code LocalSessionFactoryBean}.
 *
 * <p>When configuring the container with {@code @Configuration} classes, a {@code @Bean}
 * of this type must be registered manually.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible(RuntimeException)
 */
public class HibernateExceptionTranslator implements PersistenceExceptionTranslator {

	private @Nullable SQLExceptionTranslator jdbcExceptionTranslator;

	private @Nullable SQLExceptionTranslator transactionExceptionTranslator = new SQLExceptionSubclassTranslator();


	/**
	 * Set the JDBC exception translator for Hibernate exception translation purposes.
	 * <p>Applied to any detected {@link java.sql.SQLException} root cause of a Hibernate
	 * {@link JDBCException}, overriding Hibernate's own {@code SQLException} translation
	 * (which is based on a Hibernate Dialect for a specific target database).
	 * @see java.sql.SQLException
	 * @see org.hibernate.JDBCException
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLStateSQLExceptionTranslator
	 */
	public void setJdbcExceptionTranslator(@Nullable SQLExceptionTranslator exceptionTranslator) {
		this.jdbcExceptionTranslator = exceptionTranslator;
		this.transactionExceptionTranslator = exceptionTranslator;
	}


	@Override
	public @Nullable DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof HibernateException hibernateEx) {
			return convertHibernateAccessException(hibernateEx);
		}
		if (ex instanceof PersistenceException) {
			if (ex.getCause() instanceof HibernateException hibernateEx) {
				return convertHibernateAccessException(hibernateEx);
			}
		}
		return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
	}

	/**
	 * Convert the given HibernateException to an appropriate exception from the
	 * {@code org.springframework.dao} hierarchy.
	 * <p>Will automatically apply a specified SQLExceptionTranslator to a
	 * Hibernate JDBCException, otherwise rely on Hibernate's default translation.
	 * @param ex the HibernateException that occurred
	 * @return a corresponding DataAccessException
	 */
	protected DataAccessException convertHibernateAccessException(HibernateException ex) {
		if (this.jdbcExceptionTranslator != null && ex instanceof JDBCException jdbcEx) {
			DataAccessException dae = this.jdbcExceptionTranslator.translate(
					"Hibernate operation: " + jdbcEx.getMessage(), jdbcEx.getSQL(), jdbcEx.getSQLException());
			if (dae != null) {
				return dae;
			}
		}
		return convertHibernateAccessException(ex, ex);
	}

	private DataAccessException convertHibernateAccessException(HibernateException ex, HibernateException exToCheck) {
		if (this.jdbcExceptionTranslator != null && exToCheck instanceof JDBCException jdbcEx) {
			DataAccessException dae = this.jdbcExceptionTranslator.translate(
					"Hibernate operation: " + jdbcEx.getMessage(), jdbcEx.getSQL(), jdbcEx.getSQLException());
			if (dae != null) {
				return dae;
			}
		}
		if (this.transactionExceptionTranslator != null && exToCheck instanceof org.hibernate.TransactionException) {
			if (ex.getCause() instanceof SQLException sqlEx) {
				DataAccessException dae = this.transactionExceptionTranslator.translate(
						"Hibernate transaction: " + ex.getMessage(), null, sqlEx);
				if (dae != null) {
					return dae;
				}
			}
		}

		if (exToCheck instanceof JDBCConnectionException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}
		if (exToCheck instanceof SQLGrammarException hibEx) {
			return new InvalidDataAccessResourceUsageException(ex.getMessage() + "; SQL [" + hibEx.getSQL() + "]", ex);
		}
		if (exToCheck instanceof QueryTimeoutException hibEx) {
			return new org.springframework.dao.QueryTimeoutException(ex.getMessage() + "; SQL [" + hibEx.getSQL() + "]", ex);
		}
		if (exToCheck instanceof LockAcquisitionException hibEx) {
			return new CannotAcquireLockException(ex.getMessage() + "; SQL [" + hibEx.getSQL() + "]", ex);
		}
		if (exToCheck instanceof PessimisticLockException hibEx) {
			return new PessimisticLockingFailureException(ex.getMessage() + "; SQL [" + hibEx.getSQL() + "]", ex);
		}
		if (exToCheck instanceof ConstraintViolationException hibEx) {
			return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + hibEx.getSQL() +
					"]; constraint [" + hibEx.getConstraintName() + "]", ex);
		}
		if (exToCheck instanceof DataException hibEx) {
			return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + hibEx.getSQL() + "]", ex);
		}
		// end of JDBCException subclass handling

		if (exToCheck instanceof QueryException) {
			return new InvalidDataAccessResourceUsageException(ex.getMessage(), ex);
		}
		if (exToCheck instanceof NonUniqueResultException) {
			return new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
		}
		if (exToCheck instanceof NonUniqueObjectException) {
			return new DuplicateKeyException(ex.getMessage(), ex);
		}
		if (exToCheck instanceof PropertyValueException) {
			return new DataIntegrityViolationException(ex.getMessage(), ex);
		}
		if (exToCheck instanceof PersistentObjectException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (exToCheck instanceof TransientObjectException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (exToCheck instanceof ObjectDeletedException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (exToCheck instanceof UnresolvableObjectException hibEx) {
			return new ObjectRetrievalFailureException(hibEx.getEntityName(), hibEx.getIdentifier(), ex.getMessage(), ex);
		}
		if (exToCheck instanceof WrongClassException hibEx) {
			return new ObjectRetrievalFailureException(hibEx.getEntityName(), hibEx.getIdentifier(), ex.getMessage(), ex);
		}
		if (exToCheck instanceof StaleObjectStateException hibEx) {
			return new ObjectOptimisticLockingFailureException(hibEx.getEntityName(), hibEx.getIdentifier(), ex.getMessage(), ex);
		}
		if (exToCheck instanceof StaleStateException) {
			return new ObjectOptimisticLockingFailureException(ex.getMessage(), ex);
		}
		if (exToCheck instanceof OptimisticEntityLockException) {
			return new ObjectOptimisticLockingFailureException(ex.getMessage(), ex);
		}
		if (exToCheck instanceof PessimisticEntityLockException) {
			if (ex.getCause() instanceof LockAcquisitionException) {
				return new CannotAcquireLockException(ex.getMessage(), ex.getCause());
			}
			return new PessimisticLockingFailureException(ex.getMessage(), ex);
		}

		// Fallback: check potentially more specific cause, otherwise JpaSystemException
		if (exToCheck.getCause() instanceof HibernateException causeToCheck) {
			return convertHibernateAccessException(ex, causeToCheck);
		}
		return new JpaSystemException(ex);
	}

}
