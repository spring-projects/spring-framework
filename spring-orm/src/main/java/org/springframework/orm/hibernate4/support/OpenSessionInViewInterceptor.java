/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncWebRequestInterceptor;
import org.springframework.web.context.request.async.AsyncWebUtils;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncManager.WebAsyncThreadInitializer;

/**
 * Spring web request interceptor that binds a Hibernate <code>Session</code> to the
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
 * <p><b>NOTE</b>: This interceptor will by default <i>not</i> flush the Hibernate
 * <code>Session</code>, with the flush mode being set to <code>FlushMode.NEVER</code>.
 * It assumes that it will be used in combination with service layer transactions
 * that handle the flushing: the active transaction manager will temporarily change
 * the flush mode to <code>FlushMode.AUTO</code> during a read-write transaction,
 * with the flush mode reset to <code>FlushMode.NEVER</code> at the end of each
 * transaction. If you intend to use this interceptor without transactions, consider
 * changing the default flush mode (through the {@link #setFlushMode "flushMode"} property).
 *
 * <p>In contrast to {@link OpenSessionInViewFilter}, this interceptor is configured
 * in a Spring application context and can thus take advantage of bean wiring..
 *
 * <p><b>WARNING:</b> Applying this interceptor to existing logic can cause issues
 * that have not appeared before, through the use of a single Hibernate
 * <code>Session</code> for the processing of an entire request. In particular, the
 * reassociation of persistent objects with a Hibernate <code>Session</code> has to
 * occur at the very beginning of request processing, to avoid clashes with already
 * loaded instances of the same objects.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setSingleSession
 * @see #setFlushMode
 * @see OpenSessionInViewFilter
 * @see org.springframework.orm.hibernate4.HibernateTransactionManager
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public class OpenSessionInViewInterceptor implements AsyncWebRequestInterceptor {

	/**
	 * Suffix that gets appended to the <code>SessionFactory</code>
	 * <code>toString()</code> representation for the "participate in existing
	 * session handling" request attribute.
	 * @see #getParticipateAttributeName
	 */
	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";

	protected final Log logger = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;


	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}


	/**
	 * Open a new Hibernate <code>Session</code> according to the settings of this
	 * <code>HibernateAccessor</code> and bind it to the thread via the
	 * {@link org.springframework.transaction.support.TransactionSynchronizationManager}.
	 */
	public void preHandle(WebRequest request) throws DataAccessException {

		String participateAttributeName = getParticipateAttributeName();

		WebAsyncManager asyncManager = AsyncWebUtils.getAsyncManager(request);
		if (asyncManager.hasConcurrentResult()) {
			if (asyncManager.initializeAsyncThread(participateAttributeName)) {
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

			WebAsyncThreadInitializer asyncThreadInitializer = createThreadInitializer(sessionHolder);
			asyncManager.registerAsyncThreadInitializer(participateAttributeName, asyncThreadInitializer);
		}
	}

	public void postHandle(WebRequest request, ModelMap model) {
	}

	/**
	 * Unbind the Hibernate <code>Session</code> from the thread and close it).
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager
	 */
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

	public void afterConcurrentHandlingStarted(WebRequest request) {
		if (!decrementParticipateCount(request)) {
			TransactionSynchronizationManager.unbindResource(getSessionFactory());
		}
	}

	/**
	 * Open a Session for the SessionFactory that this interceptor uses.
	 * <p>The default implementation delegates to the
	 * <code>SessionFactory.openSession</code> method and
	 * sets the <code>Session</code>'s flush mode to "MANUAL".
	 * @return the Session to use
	 * @throws DataAccessResourceFailureException if the Session could not be created
	 * @see org.hibernate.FlushMode#MANUAL
	 */
	protected Session openSession() throws DataAccessResourceFailureException {
		try {
			Session session = SessionFactoryUtils.openSession(getSessionFactory());
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
	 * <p>The default implementation takes the <code>toString()</code> representation
	 * of the <code>SessionFactory</code> instance and appends {@link #PARTICIPATE_SUFFIX}.
	 */
	protected String getParticipateAttributeName() {
		return getSessionFactory().toString() + PARTICIPATE_SUFFIX;
	}

	private WebAsyncThreadInitializer createThreadInitializer(final SessionHolder sessionHolder) {
		return new WebAsyncThreadInitializer() {
			public void initialize() {
				TransactionSynchronizationManager.bindResource(getSessionFactory(), sessionHolder);
			}
			public void reset() {
				TransactionSynchronizationManager.unbindResource(getSessionFactory());
			}
		};
	}

}
