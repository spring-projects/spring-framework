/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.orm.hibernate3;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * {@link PersistenceExceptionTranslator} capable of translating {@link HibernateException}
 * instances to Spring's {@link DataAccessException} hierarchy.
 *
 * <p>Extended by {@link LocalSessionFactoryBean}, so there is no need to declare this
 * translator in addition to a {@code LocalSessionFactoryBean}.
 *
 * <p>When configuring the container with {@code @Configuration} classes, a {@code @Bean}
 * of this type must be registered manually.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * @see SessionFactoryUtils#convertHibernateAccessException(HibernateException)
 * @see SQLExceptionTranslator
 */
public class HibernateExceptionTranslator implements PersistenceExceptionTranslator {

	private SQLExceptionTranslator jdbcExceptionTranslator;


	/**
	 * Set the JDBC exception translator for the SessionFactory,
	 * exposed via the PersistenceExceptionTranslator interface.
	 * <p>Applied to any SQLException root cause of a Hibernate JDBCException,
	 * overriding Hibernate's default SQLException translation (which is
	 * based on Hibernate's Dialect for a specific target database).
	 * @param jdbcExceptionTranslator the exception translator
	 * @see java.sql.SQLException
	 * @see org.hibernate.JDBCException
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLStateSQLExceptionTranslator
	 * @see org.springframework.dao.support.PersistenceExceptionTranslator
	 */
	public void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator) {
		this.jdbcExceptionTranslator = jdbcExceptionTranslator;
	}


	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof HibernateException) {
			return convertHibernateAccessException((HibernateException) ex);
		}
		return null;
	}

	/**
	 * Convert the given HibernateException to an appropriate exception from the
	 * {@code org.springframework.dao} hierarchy.
	 * <p>Will automatically apply a specified SQLExceptionTranslator to a
	 * Hibernate JDBCException, else rely on Hibernate's default translation.
	 * @param ex HibernateException that occured
	 * @return a corresponding DataAccessException
	 * @see SessionFactoryUtils#convertHibernateAccessException
	 * @see #setJdbcExceptionTranslator
	 */
	protected DataAccessException convertHibernateAccessException(HibernateException ex) {
		if (this.jdbcExceptionTranslator != null && ex instanceof JDBCException) {
			JDBCException jdbcEx = (JDBCException) ex;
			return this.jdbcExceptionTranslator.translate(
					"Hibernate operation: " + jdbcEx.getMessage(), jdbcEx.getSQL(), jdbcEx.getSQLException());
		}
		return SessionFactoryUtils.convertHibernateAccessException(ex);
	}

}
