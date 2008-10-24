/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.orm.toplink;

import java.sql.SQLException;

import oracle.toplink.exceptions.DatabaseException;
import oracle.toplink.exceptions.TopLinkException;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates a
 * TopLink {@link SessionFactory}. This is the usual way to set up a shared
 * TopLink SessionFactory in a Spring application context; the SessionFactory
 * can then be passed to TopLink-based DAOs via dependency injection.
 *
 * <p>See the base class {@link LocalSessionFactory} for configuration details.
 *
 * <p>This class also implements the
 * {@link org.springframework.dao.support.PersistenceExceptionTranslator}
 * interface, as autodetected by Spring's
 * {@link org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor},
 * for AOP-based translation of native exceptions to Spring DataAccessExceptions.
 * Hence, the presence of a LocalSessionFactoryBean automatically enables a
 * PersistenceExceptionTranslationPostProcessor to translate TopLink exceptions.
 *
 * <p>If your DAOs expect to receive a raw TopLink Session, consider defining a
 * {@link org.springframework.orm.toplink.support.TransactionAwareSessionAdapter}
 * in front of this bean. This adapter will provide a TopLink Session rather
 * than a SessionFactory as bean reference. Your DAOs can then, for example,
 * access the currently active Session and UnitOfWork via
 * <code>Session.getActiveSession()</code> and <code>Session.getActiveUnitOfWork()</code>,
 * respectively. Note that you can still access the SessionFactory as well, by
 * defining a bean reference that points directly at the LocalSessionFactoryBean.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see LocalSessionFactory
 * @see org.springframework.orm.toplink.support.TransactionAwareSessionAdapter
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 */
public class LocalSessionFactoryBean extends LocalSessionFactory
		implements FactoryBean, BeanClassLoaderAware, InitializingBean, DisposableBean, PersistenceExceptionTranslator {

	private SessionFactory sessionFactory;

	private SQLExceptionTranslator jdbcExceptionTranslator;


	/**
	 * Set the JDBC exception translator for this SessionFactory.
	 * <p>Applied to any SQLException root cause of a TopLink DatabaseException,
	 * within Spring's PersistenceExceptionTranslator mechanism.
	 * The default is to rely on TopLink's native exception translation.
	 * @see oracle.toplink.exceptions.DatabaseException
	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
	 * @see org.springframework.jdbc.support.SQLStateSQLExceptionTranslator
	 */
	public void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator) {
		this.jdbcExceptionTranslator = jdbcExceptionTranslator;
	}

	/**
	 * Return the JDBC exception translator for this instance, if any.
	 */
	public SQLExceptionTranslator getJdbcExceptionTranslator() {
		return this.jdbcExceptionTranslator;
	}

	/**
	 * Sets the given bean ClassLoader as TopLink Session ClassLoader.
	 * @see #setSessionClassLoader
	 */
	public void setBeanClassLoader(ClassLoader classLoader) {
		setSessionClassLoader(classLoader);
	}

	public void afterPropertiesSet() throws TopLinkException {
		this.sessionFactory = createSessionFactory();
	}


	public Object getObject() {
		return this.sessionFactory;
	}

	public Class getObjectType() {
		return (this.sessionFactory != null ? this.sessionFactory.getClass() : SessionFactory.class);
	}

	public boolean isSingleton() {
		return true;
	}


	/**
	 * Implementation of the PersistenceExceptionTranslator interface,
	 * as autodetected by Spring's PersistenceExceptionTranslationPostProcessor.
	 * <p>Converts the exception if it is a TopLinkException;
	 * else returns <code>null</code> to indicate an unknown exception.
	 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
	 * @see #convertTopLinkAccessException
	 */
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof TopLinkException) {
			return convertTopLinkAccessException((TopLinkException) ex);
		}
		return null;
	}

	/**
	 * Convert the given TopLinkException to an appropriate exception from the
	 * <code>org.springframework.dao</code> hierarchy.
	 * <p>Will automatically apply a specified SQLExceptionTranslator to a
	 * TopLink DatabaseException, else rely on TopLink's default translation.
	 * @param ex TopLinkException that occured
	 * @return a corresponding DataAccessException
	 * @see SessionFactoryUtils#convertTopLinkAccessException
	 * @see #setJdbcExceptionTranslator
	 */
	public DataAccessException convertTopLinkAccessException(TopLinkException ex) {
		if (getJdbcExceptionTranslator() != null && ex instanceof DatabaseException) {
			Throwable internalEx = ex.getInternalException();
			// Should always be a SQLException inside a DatabaseException.
			if (internalEx instanceof SQLException) {
				return getJdbcExceptionTranslator().translate(
						"TopLink operation: " + ex.getMessage(), null, (SQLException) internalEx);
			}
		}
		return SessionFactoryUtils.convertTopLinkAccessException(ex);
	}


	public void destroy() {
		logger.info("Closing TopLink SessionFactory");
		this.sessionFactory.close();
	}

}
