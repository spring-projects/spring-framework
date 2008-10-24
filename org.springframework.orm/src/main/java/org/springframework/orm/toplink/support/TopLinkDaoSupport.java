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

package org.springframework.orm.toplink.support;

import oracle.toplink.exceptions.TopLinkException;
import oracle.toplink.sessions.Session;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.support.DaoSupport;
import org.springframework.orm.toplink.SessionFactory;
import org.springframework.orm.toplink.SessionFactoryUtils;
import org.springframework.orm.toplink.TopLinkTemplate;

/**
 * Convenient super class for TopLink data access objects.
 *
 * <p>Requires a SessionFactory to be set, providing a TopLinkTemplate
 * based on it to subclasses. Can alternatively be initialized directly with
 * a TopLinkTemplate, to reuse the latter's settings such as the SessionFactory,
 * exception translator, etc.
 *
 * <p>This base class is mainly intended for TopLinkTemplate usage
 * but can also be used when working with SessionFactoryUtils directly,
 * for example in combination with TopLinkInterceptor-managed Sessions.
 * Convenience <code>getSession</code> and <code>releaseSession</code>
 * methods are provided for that usage style.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setSessionFactory
 * @see #setTopLinkTemplate
 * @see #getSession
 * @see #releaseSession
 * @see org.springframework.orm.toplink.TopLinkTemplate
 * @see org.springframework.orm.toplink.TopLinkInterceptor
 */
public abstract class TopLinkDaoSupport extends DaoSupport {

	private TopLinkTemplate topLinkTemplate;


	/**
	 * Set the TopLink SessionFactory to be used by this DAO.
	 * Will automatically create a TopLinkTemplate for the given SessionFactory.
	 * @see #createTopLinkTemplate
	 * @see #setTopLinkTemplate
	 */
	public final void setSessionFactory(SessionFactory sessionFactory) {
		if (this.topLinkTemplate == null || sessionFactory != this.topLinkTemplate.getSessionFactory()) {
	  	this.topLinkTemplate = createTopLinkTemplate(sessionFactory);
		}
	}

	/**
	 * Create a TopLinkTemplate for the given SessionFactory.
	 * Only invoked if populating the DAO with a SessionFactory reference!
	 * <p>Can be overridden in subclasses to provide a TopLinkTemplate instance
	 * with different configuration, or a custom TopLinkTemplate subclass.
	 * @param sessionFactory the TopLink SessionFactory to create a TopLinkTemplate for
	 * @return the new TopLinkTemplate instance
	 * @see #setSessionFactory
	 */
	protected TopLinkTemplate createTopLinkTemplate(SessionFactory sessionFactory) {
		return new TopLinkTemplate(sessionFactory);
	}

	/**
	 * Return the TopLink SessionFactory used by this DAO.
	 */
	public final SessionFactory getSessionFactory() {
		return (this.topLinkTemplate != null ? this.topLinkTemplate.getSessionFactory() : null);
	}

	/**
	 * Set the TopLinkTemplate for this DAO explicitly,
	 * as an alternative to specifying a SessionFactory.
	 * @see #setSessionFactory
	 */
	public final void setTopLinkTemplate(TopLinkTemplate topLinkTemplate) {
		this.topLinkTemplate = topLinkTemplate;
	}

	/**
	 * Return the TopLinkTemplate for this DAO,
	 * pre-initialized with the SessionFactory or set explicitly.
	 */
	public final TopLinkTemplate getTopLinkTemplate() {
		return topLinkTemplate;
	}

	protected final void checkDaoConfig() {
		if (this.topLinkTemplate == null) {
			throw new IllegalArgumentException("sessionFactory or topLinkTemplate is required");
		}
	}


	/**
	 * Get a TopLink Session, either from the current transaction or a new one.
	 * The latter is only allowed if the "allowCreate" setting of this bean's
	 * TopLinkTemplate is true.
	 * <p><b>Note that this is not meant to be invoked from TopLinkTemplate code
	 * but rather just in plain TopLink code.</b> Either rely on a thread-bound
	 * Session (via TopLinkInterceptor), or use it in combination with
	 * <code>releaseSession</code>.
	 * <p>In general, it is recommended to use TopLinkTemplate, either with
	 * the provided convenience operations or with a custom TopLinkCallback
	 * that provides you with a Session to work on. TopLinkTemplate will care
	 * for all resource management and for proper exception conversion.
	 * @return the TopLink Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and allowCreate false
	 * @see TopLinkTemplate
	 * @see org.springframework.orm.toplink.SessionFactoryUtils#getSession(SessionFactory, boolean)
	 * @see org.springframework.orm.toplink.TopLinkInterceptor
	 * @see org.springframework.orm.toplink.TopLinkTemplate
	 * @see org.springframework.orm.toplink.TopLinkCallback
	 */
	protected final Session getSession()
			throws DataAccessResourceFailureException, IllegalStateException {

		return getSession(this.topLinkTemplate.isAllowCreate());
	}

	/**
	 * Get a TopLink Session, either from the current transaction or a new one.
	 * The latter is only allowed if "allowCreate" is true.
	 * <p><b>Note that this is not meant to be invoked from TopLinkTemplate code
	 * but rather just in plain TopLink code.</b> Either rely on a thread-bound
	 * Session (via TopLinkInterceptor), or use it in combination with
	 * <code>releaseSession</code>.
	 * <p>In general, it is recommended to use TopLinkTemplate, either with
	 * the provided convenience operations or with a custom TopLinkCallback
	 * that provides you with a Session to work on. TopLinkTemplate will care
	 * for all resource management and for proper exception conversion.
	 * @param allowCreate if a new Session should be created if no thread-bound found
	 * @return the TopLink Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and allowCreate false
	 * @see org.springframework.orm.toplink.SessionFactoryUtils#getSession(SessionFactory, boolean)
	 * @see org.springframework.orm.toplink.TopLinkInterceptor
	 * @see org.springframework.orm.toplink.TopLinkTemplate
	 * @see org.springframework.orm.toplink.TopLinkCallback
	 */
	protected final Session getSession(boolean allowCreate)
			throws DataAccessResourceFailureException, IllegalStateException {

		return SessionFactoryUtils.getSession(this.getSessionFactory(), allowCreate);
	}

	/**
	 * Convert the given TopLinkException to an appropriate exception from the
	 * <code>org.springframework.dao</code> hierarchy. Will automatically detect
	 * wrapped SQLExceptions and convert them accordingly.
	 * <p>Delegates to the convertTopLinkAccessException method of this
	 * DAO's TopLinkTemplate.
	 * @param ex TopLinkException that occured
	 * @return the corresponding DataAccessException instance
	 * @see #setTopLinkTemplate
	 * @see org.springframework.orm.toplink.TopLinkTemplate#convertTopLinkAccessException
	 */
	protected final DataAccessException convertTopLinkAccessException(TopLinkException ex) {
		return this.topLinkTemplate.convertTopLinkAccessException(ex);
	}

	/**
	 * Close the given TopLink Session, created via this DAO's SessionFactory,
	 * if it isn't bound to the thread.
	 * @param session the TopLink Session to close
	 * @see org.springframework.orm.toplink.SessionFactoryUtils#releaseSession
	 */
	protected final void releaseSession(Session session) {
		SessionFactoryUtils.releaseSession(session, getSessionFactory());
	}

}
