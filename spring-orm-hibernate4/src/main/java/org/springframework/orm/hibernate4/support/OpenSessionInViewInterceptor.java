/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.orm.hibernate4.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate4.SessionFactoryUtils;
import org.springframework.orm.hibernate4.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.AsyncWebRequestInterceptor;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;

/**
 * Spring web request interceptor that binds a Hibernate {@code Session} to the
 * thread for the entire processing of the request.
 *
 * <p>This class is a concrete expression of the "Open Session in View" pattern, which
 * is a pattern that allows for the lazy loading of associations in web views despite
 * the original transactions already being completed.
 *
 * <p>This interceptor makes Hibernate Sessions available via the current thread,
 * which will be autodetected by transaction managers. It is suitable for service layer
 * transactions via {@link org.springframework.orm.hibernate4.HibernateTransactionManager}
 * as well as for non-transactional execution (if configured appropriately).
 *
 * <p>In contrast to {@link OpenSessionInViewFilter}, this interceptor is configured
 * in a Spring application context and can thus take advantage of bean wiring.
 *
 * <p><b>WARNING:</b> Applying this interceptor to existing logic can cause issues
 * that have not appeared before, through the use of a single Hibernate
 * {@code Session} for the processing of an entire request. In particular, the
 * reassociation of persistent objects with a Hibernate {@code Session} has to
 * occur at the very beginning of request processing, to avoid clashes with already
 * loaded instances of the same objects.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see OpenSessionInViewFilter
 * @see OpenSessionInterceptor
 * @see org.springframework.orm.hibernate4.HibernateTransactionManager
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 * @see org.hibernate.SessionFactory#getCurrentSession()
 */
public class OpenSessionInViewInterceptor implements AsyncWebRequestInterceptor {

	/**
	 * Suffix that gets appended to the {@code SessionFactory}
	 * {@code toString()} representation for the "participate in existing
	 * session handling" request attribute.
	 * @see #getParticipateAttributeName
	 */
	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";

	protected final Log logger = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;


	/**
	 * Set the Hibernate SessionFactory that should be used to create Hibernate Sessions.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Return the Hibernate SessionFactory that should be used to create Hibernate Sessions.
	 */
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}


	/**
	 * Open a new Hibernate {@code Session} according and bind it to the thread via the
	 * {@link org.springframework.transaction.support.TransactionSynchronizationManager}.
	 */
	@Override
	public void preHandle(WebRequest request) throws DataAccessException {
		String participateAttributeName = getParticipateAttributeName();

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		if (asyncManager.hasConcurrentResult()) {
			if (applySessionBindingInterceptor(asyncManager, participateAttributeName)) {
				return;
			}
		}

		if (TransactionSynchronizationManager.hasResource(getSessionFactory())) {
			// Do not modify the Session: just mark the request accordingly.
			Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			int newCount = (count != null ? count + 1 : 1);
			request.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		}
		else {
			logger.debug("Opening Hibernate Session in OpenSessionInViewInterceptor");
			Session session = openSession();
			SessionHolder sessionHolder = new SessionHolder(session);
			TransactionSynchronizationManager.bindResource(getSessionFactory(), sessionHolder);

			AsyncRequestInterceptor asyncRequestInterceptor =
					new AsyncRequestInterceptor(getSessionFactory(), sessionHolder);
			asyncManager.registerCallableInterceptor(participateAttributeName, asyncRequestInterceptor);
			asyncManager.registerDeferredResultInterceptor(participateAttributeName, asyncRequestInterceptor);
		}
	}

	@Override
	public void postHandle(WebRequest request, ModelMap model) {
	}

	/**
	 * Unbind the Hibernate {@code Session} from the thread and close it).
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager
	 */
	@Override
	public void afterCompletion(WebRequest request, Exception ex) throws DataAccessException {
		if (!decrementParticipateCount(request)) {
			SessionHolder sessionHolder =
					(SessionHolder) TransactionSynchronizationManager.unbindResource(getSessionFactory());
			logger.debug("Closing Hibernate Session in OpenSessionInViewInterceptor");
			SessionFactoryUtils.closeSession(sessionHolder.getSession());
		}
	}

	private boolean decrementParticipateCount(WebRequest request) {
		String participateAttributeName = getParticipateAttributeName();
		Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		if (count == null) {
			return false;
		}
		// Do not modify the Session: just clear the marker.
		if (count > 1) {
			request.setAttribute(participateAttributeName, count - 1, WebRequest.SCOPE_REQUEST);
		}
		else {
			request.removeAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		}
		return true;
	}

	@Override
	public void afterConcurrentHandlingStarted(WebRequest request) {
		if (!decrementParticipateCount(request)) {
			TransactionSynchronizationManager.unbindResource(getSessionFactory());
		}
	}

	/**
	 * Open a Session for the SessionFactory that this interceptor uses.
	 * <p>The default implementation delegates to the {@link SessionFactory#openSession}
	 * method and sets the {@link Session}'s flush mode to "MANUAL".
	 * @return the Session to use
	 * @throws DataAccessResourceFailureException if the Session could not be created
	 * @see org.hibernate.FlushMode#MANUAL
	 */
	protected Session openSession() throws DataAccessResourceFailureException {
		try {
			Session session = getSessionFactory().openSession();
			session.setFlushMode(FlushMode.MANUAL);
			return session;
		}
		catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
	}

	/**
	 * Return the name of the request attribute that identifies that a request is
	 * already intercepted.
	 * <p>The default implementation takes the {@code toString()} representation
	 * of the {@code SessionFactory} instance and appends {@link #PARTICIPATE_SUFFIX}.
	 */
	protected String getParticipateAttributeName() {
		return getSessionFactory().toString() + PARTICIPATE_SUFFIX;
	}

	private boolean applySessionBindingInterceptor(WebAsyncManager asyncManager, String key) {
		if (asyncManager.getCallableInterceptor(key) == null) {
			return false;
		}
		((AsyncRequestInterceptor) asyncManager.getCallableInterceptor(key)).bindSession();
		return true;
	}

}
