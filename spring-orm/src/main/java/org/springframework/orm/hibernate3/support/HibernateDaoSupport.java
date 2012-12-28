/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.orm.hibernate3.support;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.support.DaoSupport;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

/**
 * Convenient super class for Hibernate-based data access objects.
 *
 * <p>Requires a {@link org.hibernate.SessionFactory} to be set, providing a
 * {@link org.springframework.orm.hibernate3.HibernateTemplate} based on it to
 * subclasses through the {@link #getHibernateTemplate()} method.
 * Can alternatively be initialized directly with a HibernateTemplate,
 * in order to reuse the latter's settings such as the SessionFactory,
 * exception translator, flush mode, etc.
 *
 * <p>This base class is mainly intended for HibernateTemplate usage but can
 * also be used when working with a Hibernate Session directly, for example
 * when relying on transactional Sessions. Convenience {@link #getSession}
 * and {@link #releaseSession} methods are provided for that usage style.
 *
 * <p>This class will create its own HibernateTemplate instance if a SessionFactory
 * is passed in. The "allowCreate" flag on that HibernateTemplate will be "true"
 * by default. A custom HibernateTemplate instance can be used through overriding
 * {@link #createHibernateTemplate}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setSessionFactory
 * @see #getHibernateTemplate
 * @see org.springframework.orm.hibernate3.HibernateTemplate
 */
public abstract class HibernateDaoSupport extends DaoSupport {

	private HibernateTemplate hibernateTemplate;


	/**
	 * Set the Hibernate SessionFactory to be used by this DAO.
	 * Will automatically create a HibernateTemplate for the given SessionFactory.
	 * @see #createHibernateTemplate
	 * @see #setHibernateTemplate
	 */
	public final void setSessionFactory(SessionFactory sessionFactory) {
		if (this.hibernateTemplate == null || sessionFactory != this.hibernateTemplate.getSessionFactory()) {
			this.hibernateTemplate = createHibernateTemplate(sessionFactory);
		}
	}

	/**
	 * Create a HibernateTemplate for the given SessionFactory.
	 * Only invoked if populating the DAO with a SessionFactory reference!
	 * <p>Can be overridden in subclasses to provide a HibernateTemplate instance
	 * with different configuration, or a custom HibernateTemplate subclass.
	 * @param sessionFactory the Hibernate SessionFactory to create a HibernateTemplate for
	 * @return the new HibernateTemplate instance
	 * @see #setSessionFactory
	 */
	protected HibernateTemplate createHibernateTemplate(SessionFactory sessionFactory) {
		return new HibernateTemplate(sessionFactory);
	}

	/**
	 * Return the Hibernate SessionFactory used by this DAO.
	 */
	public final SessionFactory getSessionFactory() {
		return (this.hibernateTemplate != null ? this.hibernateTemplate.getSessionFactory() : null);
	}

	/**
	 * Set the HibernateTemplate for this DAO explicitly,
	 * as an alternative to specifying a SessionFactory.
	 * @see #setSessionFactory
	 */
	public final void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * Return the HibernateTemplate for this DAO,
	 * pre-initialized with the SessionFactory or set explicitly.
	 * <p><b>Note: The returned HibernateTemplate is a shared instance.</b>
	 * You may introspect its configuration, but not modify the configuration
	 * (other than from within an {@link #initDao} implementation).
	 * Consider creating a custom HibernateTemplate instance via
	 * <code>new HibernateTemplate(getSessionFactory())</code>, in which
	 * case you're allowed to customize the settings on the resulting instance.
	 */
	public final HibernateTemplate getHibernateTemplate() {
	  return this.hibernateTemplate;
	}

	@Override
	protected final void checkDaoConfig() {
		if (this.hibernateTemplate == null) {
			throw new IllegalArgumentException("'sessionFactory' or 'hibernateTemplate' is required");
		}
	}


	/**
	 * Obtain a Hibernate Session, either from the current transaction or
	 * a new one. The latter is only allowed if the
	 * {@link org.springframework.orm.hibernate3.HibernateTemplate#setAllowCreate "allowCreate"}
	 * setting of this bean's {@link #setHibernateTemplate HibernateTemplate} is "true".
	 * <p><b>Note that this is not meant to be invoked from HibernateTemplate code
	 * but rather just in plain Hibernate code.</b> Either rely on a thread-bound
	 * Session or use it in combination with {@link #releaseSession}.
	 * <p>In general, it is recommended to use HibernateTemplate, either with
	 * the provided convenience operations or with a custom HibernateCallback
	 * that provides you with a Session to work on. HibernateTemplate will care
	 * for all resource management and for proper exception conversion.
	 * @return the Hibernate Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and allowCreate=false
	 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#getSession(SessionFactory, boolean)
	 */
	protected final Session getSession()
		throws DataAccessResourceFailureException, IllegalStateException {

		return getSession(this.hibernateTemplate.isAllowCreate());
	}

	/**
	 * Obtain a Hibernate Session, either from the current transaction or
	 * a new one. The latter is only allowed if "allowCreate" is true.
	 * <p><b>Note that this is not meant to be invoked from HibernateTemplate code
	 * but rather just in plain Hibernate code.</b> Either rely on a thread-bound
	 * Session or use it in combination with {@link #releaseSession}.
	 * <p>In general, it is recommended to use
	 * {@link #getHibernateTemplate() HibernateTemplate}, either with
	 * the provided convenience operations or with a custom
	 * {@link org.springframework.orm.hibernate3.HibernateCallback} that
	 * provides you with a Session to work on. HibernateTemplate will care
	 * for all resource management and for proper exception conversion.
	 * @param allowCreate if a non-transactional Session should be created when no
	 * transactional Session can be found for the current thread
	 * @return the Hibernate Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and allowCreate=false
	 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#getSession(SessionFactory, boolean)
	 */
	protected final Session getSession(boolean allowCreate)
		throws DataAccessResourceFailureException, IllegalStateException {

		return (!allowCreate ?
			SessionFactoryUtils.getSession(getSessionFactory(), false) :
				SessionFactoryUtils.getSession(
						getSessionFactory(),
						this.hibernateTemplate.getEntityInterceptor(),
						this.hibernateTemplate.getJdbcExceptionTranslator()));
	}

	/**
	 * Convert the given HibernateException to an appropriate exception from the
	 * <code>org.springframework.dao</code> hierarchy. Will automatically detect
	 * wrapped SQLExceptions and convert them accordingly.
	 * <p>Delegates to the
	 * {@link org.springframework.orm.hibernate3.HibernateTemplate#convertHibernateAccessException}
	 * method of this DAO's HibernateTemplate.
	 * <p>Typically used in plain Hibernate code, in combination with
	 * {@link #getSession} and {@link #releaseSession}.
	 * @param ex HibernateException that occurred
	 * @return the corresponding DataAccessException instance
	 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#convertHibernateAccessException
	 */
	protected final DataAccessException convertHibernateAccessException(HibernateException ex) {
		return this.hibernateTemplate.convertHibernateAccessException(ex);
	}

	/**
	 * Close the given Hibernate Session, created via this DAO's SessionFactory,
	 * if it isn't bound to the thread (i.e. isn't a transactional Session).
	 * <p>Typically used in plain Hibernate code, in combination with
	 * {@link #getSession} and {@link #convertHibernateAccessException}.
	 * @param session the Session to close
	 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#releaseSession
	 */
	protected final void releaseSession(Session session) {
		SessionFactoryUtils.releaseSession(session, getSessionFactory());
	}

}
