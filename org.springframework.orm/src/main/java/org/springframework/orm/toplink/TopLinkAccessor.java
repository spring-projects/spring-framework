/*
 * Copyright 2002-2006 the original author or authors.
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * Base class for TopLinkTemplate and TopLinkInterceptor, defining common properties
 * such as SessionFactory and JDBC exception translator.
 *
 * <p>Not intended to be used directly. See TopLinkTemplate and TopLinkInterceptor.
 *
 * <p>Thanks to Slavik Markovich for implementing the initial TopLink support prototype!
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see TopLinkTemplate
 * @see TopLinkInterceptor
 */
public abstract class TopLinkAccessor implements InitializingBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;

	private SQLExceptionTranslator jdbcExceptionTranslator;


	/**
	 * Set the the TopLink SessionFactory that should be used to create TopLink
	 * Sessions. This will usually be a ServerSessionFactory in a multi-threaded
	 * environment, but can also be a SingleSessionFactory for testing purposes
	 * or for standalone execution.
	 * <p>The passed-in SessionFactory will usually be asked for a plain Session
	 * to perform data access on, unless an active transaction with a thread-bound
	 * Session is found.
	 * @see ServerSessionFactory
	 * @see SingleSessionFactory
	 * @see SessionFactory#createSession()
	 * @see SessionFactoryUtils#getSession(SessionFactory, boolean)
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Return the TopLink SessionFactory that should be used to create
	 * TopLink Sessions.
	 */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	/**
	 * Set the JDBC exception translator for this instance.
	 * <p>Applied to any SQLException root cause of a TopLink DatabaseException.
	 * The default is to rely on TopLink's native exception translation.
	 * @param jdbcExceptionTranslator the exception translator
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
	 * Check that we were provided with a session to use
	 */
	public void afterPropertiesSet() {
		if (this.sessionFactory == null) {
			throw new IllegalArgumentException("sessionFactory is required");
		}
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

}
