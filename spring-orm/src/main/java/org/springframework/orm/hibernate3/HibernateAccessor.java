/*
 * Copyright 2002-2014 the original author or authors.
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

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.GenericJDBCException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Constants;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * Base class for {@link HibernateTemplate} and {@link HibernateInterceptor},
 * defining common properties such as SessionFactory and flushing behavior.
 *
 * <p>Not intended to be used directly.
 * See {@link HibernateTemplate} and {@link HibernateInterceptor}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see HibernateTemplate
 * @see HibernateInterceptor
 * @see #setFlushMode
 */
public abstract class HibernateAccessor implements InitializingBean, BeanFactoryAware {

	/**
	 * Never flush is a good strategy for read-only units of work.
	 * Hibernate will not track and look for changes in this case,
	 * avoiding any overhead of modification detection.
	 * <p>In case of an existing Session, FLUSH_NEVER will turn the flush mode
	 * to NEVER for the scope of the current operation, resetting the previous
	 * flush mode afterwards.
	 * @see #setFlushMode
	 */
	public static final int FLUSH_NEVER = 0;

	/**
	 * Automatic flushing is the default mode for a Hibernate Session.
	 * A session will get flushed on transaction commit, and on certain find
	 * operations that might involve already modified instances, but not
	 * after each unit of work like with eager flushing.
	 * <p>In case of an existing Session, FLUSH_AUTO will participate in the
	 * existing flush mode, not modifying it for the current operation.
	 * This in particular means that this setting will not modify an existing
	 * flush mode NEVER, in contrast to FLUSH_EAGER.
	 * @see #setFlushMode
	 */
	public static final int FLUSH_AUTO = 1;

	/**
	 * Eager flushing leads to immediate synchronization with the database,
	 * even if in a transaction. This causes inconsistencies to show up and throw
	 * a respective exception immediately, and JDBC access code that participates
	 * in the same transaction will see the changes as the database is already
	 * aware of them then. But the drawbacks are:
	 * <ul>
	 * <li>additional communication roundtrips with the database, instead of a
	 * single batch at transaction commit;
	 * <li>the fact that an actual database rollback is needed if the Hibernate
	 * transaction rolls back (due to already submitted SQL statements).
	 * </ul>
	 * <p>In case of an existing Session, FLUSH_EAGER will turn the flush mode
	 * to AUTO for the scope of the current operation and issue a flush at the
	 * end, resetting the previous flush mode afterwards.
	 * @see #setFlushMode
	 */
	public static final int FLUSH_EAGER = 2;

	/**
	 * Flushing at commit only is intended for units of work where no
	 * intermediate flushing is desired, not even for find operations
	 * that might involve already modified instances.
	 * <p>In case of an existing Session, FLUSH_COMMIT will turn the flush mode
	 * to COMMIT for the scope of the current operation, resetting the previous
	 * flush mode afterwards. The only exception is an existing flush mode
	 * NEVER, which will not be modified through this setting.
	 * @see #setFlushMode
	 */
	public static final int FLUSH_COMMIT = 3;

	/**
	 * Flushing before every query statement is rarely necessary.
	 * It is only available for special needs.
	 * <p>In case of an existing Session, FLUSH_ALWAYS will turn the flush mode
	 * to ALWAYS for the scope of the current operation, resetting the previous
	 * flush mode afterwards.
	 * @see #setFlushMode
	 */
	public static final int FLUSH_ALWAYS = 4;


	/** Constants instance for HibernateAccessor */
	private static final Constants constants = new Constants(HibernateAccessor.class);

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;

	private Object entityInterceptor;

	private SQLExceptionTranslator jdbcExceptionTranslator;

	private SQLExceptionTranslator defaultJdbcExceptionTranslator;

	private int flushMode = FLUSH_AUTO;

	private String[] filterNames;

	/**
	 * Just needed for entityInterceptorBeanName.
	 * @see #setEntityInterceptorBeanName
	 */
	private BeanFactory beanFactory;


	/**
	 * Set the Hibernate SessionFactory that should be used to create
	 * Hibernate Sessions.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Return the Hibernate SessionFactory that should be used to create
	 * Hibernate Sessions.
	 */
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	/**
	 * Set the bean name of a Hibernate entity interceptor that allows to inspect
	 * and change property values before writing to and reading from the database.
	 * Will get applied to any new Session created by this transaction manager.
	 * <p>Requires the bean factory to be known, to be able to resolve the bean
	 * name to an interceptor instance on session creation. Typically used for
	 * prototype interceptors, i.e. a new interceptor instance per session.
	 * <p>Can also be used for shared interceptor instances, but it is recommended
	 * to set the interceptor reference directly in such a scenario.
	 * @param entityInterceptorBeanName the name of the entity interceptor in
	 * the bean factory
	 * @see #setBeanFactory
	 * @see #setEntityInterceptor
	 */
	public void setEntityInterceptorBeanName(String entityInterceptorBeanName) {
		this.entityInterceptor = entityInterceptorBeanName;
	}

	/**
	 * Set a Hibernate entity interceptor that allows to inspect and change
	 * property values before writing to and reading from the database.
	 * Will get applied to any <b>new</b> Session created by this object.
	 * <p>Such an interceptor can either be set at the SessionFactory level,
	 * i.e. on LocalSessionFactoryBean, or at the Session level, i.e. on
	 * HibernateTemplate, HibernateInterceptor, and HibernateTransactionManager.
	 * It's preferable to set it on LocalSessionFactoryBean or HibernateTransactionManager
	 * to avoid repeated configuration and guarantee consistent behavior in transactions.
	 * @see #setEntityInterceptorBeanName
	 * @see LocalSessionFactoryBean#setEntityInterceptor
	 * @see HibernateTransactionManager#setEntityInterceptor
	 */
	public void setEntityInterceptor(Interceptor entityInterceptor) {
		this.entityInterceptor = entityInterceptor;
	}

	/**
	 * Return the current Hibernate entity interceptor, or {@code null} if none.
	 * Resolves an entity interceptor bean name via the bean factory,
	 * if necessary.
	 * @throws IllegalStateException if bean name specified but no bean factory set
	 * @throws org.springframework.beans.BeansException if bean name resolution via the bean factory failed
	 * @see #setEntityInterceptor
	 * @see #setEntityInterceptorBeanName
	 * @see #setBeanFactory
	 */
	public Interceptor getEntityInterceptor() throws IllegalStateException, BeansException {
		if (this.entityInterceptor instanceof String) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("Cannot get entity interceptor via bean name if no bean factory set");
			}
			return this.beanFactory.getBean((String) this.entityInterceptor, Interceptor.class);
		}
		return (Interceptor) this.entityInterceptor;
	}

	/**
	 * Set the JDBC exception translator for this instance.
	 * <p>Applied to any SQLException root cause of a Hibernate JDBCException,
	 * overriding Hibernate's default SQLException translation (which is
	 * based on Hibernate's Dialect for a specific target database).
	 * @param jdbcExceptionTranslator the exception translator
	 * @see java.sql.SQLException
	 * @see org.hibernate.JDBCException
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
	 * Set the flush behavior by the name of the respective constant
	 * in this class, e.g. "FLUSH_AUTO". Default is "FLUSH_AUTO".
	 * @param constantName name of the constant
	 * @see #setFlushMode
	 * @see #FLUSH_AUTO
	 */
	public void setFlushModeName(String constantName) {
		setFlushMode(constants.asNumber(constantName).intValue());
	}

	/**
	 * Set the flush behavior to one of the constants in this class.
	 * Default is FLUSH_AUTO.
	 * @see #setFlushModeName
	 * @see #FLUSH_AUTO
	 */
	public void setFlushMode(int flushMode) {
		this.flushMode = flushMode;
	}

	/**
	 * Return if a flush should be forced after executing the callback code.
	 */
	public int getFlushMode() {
		return this.flushMode;
	}

	/**
	 * Set the name of a Hibernate filter to be activated for all
	 * Sessions that this accessor works with.
	 * <p>This filter will be enabled at the beginning of each operation
	 * and correspondingly disabled at the end of the operation.
	 * This will work for newly opened Sessions as well as for existing
	 * Sessions (for example, within a transaction).
	 * @see #enableFilters(org.hibernate.Session)
	 * @see org.hibernate.Session#enableFilter(String)
	 * @see LocalSessionFactoryBean#setFilterDefinitions
	 */
	public void setFilterName(String filter) {
		this.filterNames = new String[] {filter};
	}

	/**
	 * Set one or more names of Hibernate filters to be activated for all
	 * Sessions that this accessor works with.
	 * <p>Each of those filters will be enabled at the beginning of each
	 * operation and correspondingly disabled at the end of the operation.
	 * This will work for newly opened Sessions as well as for existing
	 * Sessions (for example, within a transaction).
	 * @see #enableFilters(org.hibernate.Session)
	 * @see org.hibernate.Session#enableFilter(String)
	 * @see LocalSessionFactoryBean#setFilterDefinitions
	 */
	public void setFilterNames(String... filterNames) {
		this.filterNames = filterNames;
	}

	/**
	 * Return the names of Hibernate filters to be activated, if any.
	 */
	public String[] getFilterNames() {
		return this.filterNames;
	}

	/**
	 * The bean factory just needs to be known for resolving entity interceptor
	 * bean names. It does not need to be set for any other mode of operation.
	 * @see #setEntityInterceptorBeanName
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
	}


	/**
	 * Apply the flush mode that's been specified for this accessor
	 * to the given Session.
	 * @param session the current Hibernate Session
	 * @param existingTransaction if executing within an existing transaction
	 * @return the previous flush mode to restore after the operation,
	 * or {@code null} if none
	 * @see #setFlushMode
	 * @see org.hibernate.Session#setFlushMode
	 */
	protected FlushMode applyFlushMode(Session session, boolean existingTransaction) {
		if (getFlushMode() == FLUSH_NEVER) {
			if (existingTransaction) {
				FlushMode previousFlushMode = session.getFlushMode();
				if (!previousFlushMode.lessThan(FlushMode.COMMIT)) {
					session.setFlushMode(FlushMode.MANUAL);
					return previousFlushMode;
				}
			}
			else {
				session.setFlushMode(FlushMode.MANUAL);
			}
		}
		else if (getFlushMode() == FLUSH_EAGER) {
			if (existingTransaction) {
				FlushMode previousFlushMode = session.getFlushMode();
				if (!previousFlushMode.equals(FlushMode.AUTO)) {
					session.setFlushMode(FlushMode.AUTO);
					return previousFlushMode;
				}
			}
			else {
				// rely on default FlushMode.AUTO
			}
		}
		else if (getFlushMode() == FLUSH_COMMIT) {
			if (existingTransaction) {
				FlushMode previousFlushMode = session.getFlushMode();
				if (previousFlushMode.equals(FlushMode.AUTO) || previousFlushMode.equals(FlushMode.ALWAYS)) {
					session.setFlushMode(FlushMode.COMMIT);
					return previousFlushMode;
				}
			}
			else {
				session.setFlushMode(FlushMode.COMMIT);
			}
		}
		else if (getFlushMode() == FLUSH_ALWAYS) {
			if (existingTransaction) {
				FlushMode previousFlushMode = session.getFlushMode();
				if (!previousFlushMode.equals(FlushMode.ALWAYS)) {
					session.setFlushMode(FlushMode.ALWAYS);
					return previousFlushMode;
				}
			}
			else {
				session.setFlushMode(FlushMode.ALWAYS);
			}
		}
		return null;
	}

	/**
	 * Flush the given Hibernate Session if necessary.
	 * @param session the current Hibernate Session
	 * @param existingTransaction if executing within an existing transaction
	 * @throws HibernateException in case of Hibernate flushing errors
	 */
	protected void flushIfNecessary(Session session, boolean existingTransaction) throws HibernateException {
		if (getFlushMode() == FLUSH_EAGER || (!existingTransaction && getFlushMode() != FLUSH_NEVER)) {
			logger.debug("Eagerly flushing Hibernate session");
			session.flush();
		}
	}


	/**
	 * Convert the given HibernateException to an appropriate exception
	 * from the {@code org.springframework.dao} hierarchy.
	 * <p>Will automatically apply a specified SQLExceptionTranslator to a
	 * Hibernate JDBCException, else rely on Hibernate's default translation.
	 * @param ex HibernateException that occured
	 * @return a corresponding DataAccessException
	 * @see SessionFactoryUtils#convertHibernateAccessException
	 * @see #setJdbcExceptionTranslator
	 */
	public DataAccessException convertHibernateAccessException(HibernateException ex) {
		if (getJdbcExceptionTranslator() != null && ex instanceof JDBCException) {
			return convertJdbcAccessException((JDBCException) ex, getJdbcExceptionTranslator());
		}
		else if (GenericJDBCException.class.equals(ex.getClass())) {
			return convertJdbcAccessException((GenericJDBCException) ex, getDefaultJdbcExceptionTranslator());
		}
		return SessionFactoryUtils.convertHibernateAccessException(ex);
	}

	/**
	 * Convert the given Hibernate JDBCException to an appropriate exception
	 * from the {@code org.springframework.dao} hierarchy, using the
	 * given SQLExceptionTranslator.
	 * @param ex Hibernate JDBCException that occured
	 * @param translator the SQLExceptionTranslator to use
	 * @return a corresponding DataAccessException
	 */
	protected DataAccessException convertJdbcAccessException(JDBCException ex, SQLExceptionTranslator translator) {
		return translator.translate("Hibernate operation: " + ex.getMessage(), ex.getSQL(), ex.getSQLException());
	}

	/**
	 * Convert the given SQLException to an appropriate exception from the
	 * {@code org.springframework.dao} hierarchy. Can be overridden in subclasses.
	 * <p>Note that a direct SQLException can just occur when callback code
	 * performs direct JDBC access via {@code Session.connection()}.
	 * @param ex the SQLException
	 * @return the corresponding DataAccessException instance
	 * @see #setJdbcExceptionTranslator
	 */
	protected DataAccessException convertJdbcAccessException(SQLException ex) {
		SQLExceptionTranslator translator = getJdbcExceptionTranslator();
		if (translator == null) {
			translator = getDefaultJdbcExceptionTranslator();
		}
		return translator.translate("Hibernate-related JDBC operation", null, ex);
	}

	/**
	 * Obtain a default SQLExceptionTranslator, lazily creating it if necessary.
	 * <p>Creates a default
	 * {@link org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator}
	 * for the SessionFactory's underlying DataSource.
	 */
	protected synchronized SQLExceptionTranslator getDefaultJdbcExceptionTranslator() {
		if (this.defaultJdbcExceptionTranslator == null) {
			this.defaultJdbcExceptionTranslator = SessionFactoryUtils.newJdbcExceptionTranslator(getSessionFactory());
		}
		return this.defaultJdbcExceptionTranslator;
	}


	/**
	 * Enable the specified filters on the given Session.
	 * @param session the current Hibernate Session
	 * @see #setFilterNames
	 * @see org.hibernate.Session#enableFilter(String)
	 */
	protected void enableFilters(Session session) {
		String[] filterNames = getFilterNames();
		if (filterNames != null) {
			for (String filterName : filterNames) {
				session.enableFilter(filterName);
			}
		}
	}

	/**
	 * Disable the specified filters on the given Session.
	 * @param session the current Hibernate Session
	 * @see #setFilterNames
	 * @see org.hibernate.Session#disableFilter(String)
	 */
	protected void disableFilters(Session session) {
		String[] filterNames = getFilterNames();
		if (filterNames != null) {
			for (String filterName : filterNames) {
				session.disableFilter(filterName);
			}
		}
	}

}
