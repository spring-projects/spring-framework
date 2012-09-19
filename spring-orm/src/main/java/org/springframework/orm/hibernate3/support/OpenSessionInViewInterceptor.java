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

package org.springframework.orm.hibernate3.support;

import java.util.concurrent.Callable;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.AsyncWebRequestInterceptor;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;

/**
 * Spring web request interceptor that binds a Hibernate <code>Session</code> to the
 * thread for the entire processing of the request.
 *
 * <p>This class is a concrete expression of the "Open Session in View" pattern, which
 * is a pattern that allows for the lazy loading of associations in web views despite
 * the original transactions already being completed.
 *
 * <p>This interceptor makes Hibernate <code>Sessions</code> available via the current
 * thread, which will be autodetected by transaction managers. It is suitable for
 * service layer transactions via
 * {@link org.springframework.orm.hibernate3.HibernateTransactionManager} or
 * {@link org.springframework.transaction.jta.JtaTransactionManager} as well as for
 * non-transactional execution (if configured appropriately).
 *
 * <p><b>NOTE</b>: This interceptor will by default <i>not</i> flush the Hibernate
 * <code>Session</code>, with the flush mode being set to <code>FlushMode.NEVER</code>.
 * It assumes that it will be used in combination with service layer transactions
 * that handle the flushing: the active transaction manager will temporarily change
 * the flush mode to <code>FlushMode.AUTO</code> during a read-write transaction,
 * with the flush mode reset to <code>FlushMode.NEVER</code> at the end of each
 * transaction. If you intend to use this interceptor without transactions, consider
 * changing the default flush mode (through the
 * {@link #setFlushMode(int) "flushMode"} property).
 *
 * <p>In contrast to {@link OpenSessionInViewFilter}, this interceptor is
 * configured in a Spring application context and can thus take advantage of bean
 * wiring. It inherits common Hibernate configuration properties from
 * {@link org.springframework.orm.hibernate3.HibernateAccessor},
 * to be configured in a bean definition.
 *
 * <p><b>WARNING:</b> Applying this interceptor to existing logic can cause issues
 * that have not appeared before, through the use of a single Hibernate
 * <code>Session</code> for the processing of an entire request. In particular, the
 * reassociation of persistent objects with a Hibernate <code>Session</code> has to
 * occur at the very beginning of request processing, to avoid clashes with already
 * loaded instances of the same objects.
 *
 * <p>Alternatively, turn this interceptor into deferred close mode, by specifying
 * "singleSession"="false": It will not use a single session per request then,
 * but rather let each data access operation or transaction use its own session
 * (as would be the case without Open Session in View). Each of those sessions will
 * be registered for deferred close though, which will actually be processed at
 * request completion.
 *
 * <p>A single session per request allows for the most efficient first-level caching,
 * but can cause side effects, for example on <code>saveOrUpdate</code> or when
 * continuing after a rolled-back transaction. The deferred close strategy is as safe
 * as no Open Session in View in that respect, while still allowing for lazy loading
 * in views (but not providing a first-level cache for the entire request).
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setSingleSession
 * @see #setFlushMode
 * @see OpenSessionInViewFilter
 * @see org.springframework.orm.hibernate3.HibernateInterceptor
 * @see org.springframework.orm.hibernate3.HibernateTransactionManager
 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#getSession
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public class OpenSessionInViewInterceptor extends HibernateAccessor implements AsyncWebRequestInterceptor {

	/**
	 * Suffix that gets appended to the <code>SessionFactory</code>
	 * <code>toString()</code> representation for the "participate in existing
	 * session handling" request attribute.
	 * @see #getParticipateAttributeName
	 */
	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";


	private boolean singleSession = true;


	/**
	 * Create a new <code>OpenSessionInViewInterceptor</code>,
	 * turning the default flushMode to <code>FLUSH_NEVER</code>.
	 * @see #setFlushMode
	 */
	public OpenSessionInViewInterceptor() {
		setFlushMode(FLUSH_NEVER);
	}

	/**
	 * Set whether to use a single session for each request. Default is "true".
	 * <p>If set to false, each data access operation or transaction will use
	 * its own session (like without Open Session in View). Each of those
	 * sessions will be registered for deferred close, though, actually
	 * processed at request completion.
	 * @see SessionFactoryUtils#initDeferredClose
	 * @see SessionFactoryUtils#processDeferredClose
	 */
	public void setSingleSession(boolean singleSession) {
		this.singleSession = singleSession;
	}

	/**
	 * Return whether to use a single session for each request.
	 */
	protected boolean isSingleSession() {
		return singleSession;
	}


	/**
	 * Open a new Hibernate <code>Session</code> according to the settings of this
	 * <code>HibernateAccessor</code> and bind it to the thread via the
	 * {@link TransactionSynchronizationManager}.
	 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#getSession
	 */
	public void preHandle(WebRequest request) throws DataAccessException {

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		String participateAttributeName = getParticipateAttributeName();

		if (asyncManager.hasConcurrentResult()) {
			if (applySessionBindingInterceptor(asyncManager, participateAttributeName)) {
				return;
			}
		}

		if ((isSingleSession() && TransactionSynchronizationManager.hasResource(getSessionFactory())) ||
		    SessionFactoryUtils.isDeferredCloseActive(getSessionFactory())) {
			// Do not modify the Session: just mark the request accordingly.
			Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			int newCount = (count != null ? count + 1 : 1);
			request.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		}
		else {
			if (isSingleSession()) {
				// single session mode
				logger.debug("Opening single Hibernate Session in OpenSessionInViewInterceptor");
				Session session = SessionFactoryUtils.getSession(
						getSessionFactory(), getEntityInterceptor(), getJdbcExceptionTranslator());
				applyFlushMode(session, false);
				SessionHolder sessionHolder = new SessionHolder(session);
				TransactionSynchronizationManager.bindResource(getSessionFactory(), sessionHolder);

				asyncManager.registerCallableInterceptor(participateAttributeName,
						new SessionBindingCallableInterceptor(sessionHolder));
			}
			else {
				// deferred close mode
				SessionFactoryUtils.initDeferredClose(getSessionFactory());
			}
		}
	}

	/**
	 * Flush the Hibernate <code>Session</code> before view rendering, if necessary.
	 * <p>Note that this just applies in {@link #isSingleSession() single session mode}!
	 * <p>The default is <code>FLUSH_NEVER</code> to avoid this extra flushing,
	 * assuming that service layer transactions have flushed their changes on commit.
	 * @see #setFlushMode
	 */
	public void postHandle(WebRequest request, ModelMap model) throws DataAccessException {
		if (isSingleSession()) {
			// Only potentially flush in single session mode.
			SessionHolder sessionHolder =
					(SessionHolder) TransactionSynchronizationManager.getResource(getSessionFactory());
			logger.debug("Flushing single Hibernate Session in OpenSessionInViewInterceptor");
			try {
				flushIfNecessary(sessionHolder.getSession(), false);
			}
			catch (HibernateException ex) {
				throw convertHibernateAccessException(ex);
			}
		}
	}

	/**
	 * Unbind the Hibernate <code>Session</code> from the thread and close it (in
	 * single session mode), or process deferred close for all sessions that have
	 * been opened during the current request (in deferred close mode).
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager
	 */
	public void afterCompletion(WebRequest request, Exception ex) throws DataAccessException {
		if (!decrementParticipateCount(request)) {
			if (isSingleSession()) {
				// single session mode
				SessionHolder sessionHolder =
						(SessionHolder) TransactionSynchronizationManager.unbindResource(getSessionFactory());
				logger.debug("Closing single Hibernate Session in OpenSessionInViewInterceptor");
				SessionFactoryUtils.closeSession(sessionHolder.getSession());
			}
			else {
				// deferred close mode
				SessionFactoryUtils.processDeferredClose(getSessionFactory());
			}
		}
	}

	public void afterConcurrentHandlingStarted(WebRequest request) {
		if (!decrementParticipateCount(request)) {
			if (isSingleSession()) {
				TransactionSynchronizationManager.unbindResource(getSessionFactory());
			}
			else {
				throw new IllegalStateException("Deferred close mode is not supported with async requests.");
			}

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

	/**
	 * Return the name of the request attribute that identifies that a request is
	 * already intercepted.
	 * <p>The default implementation takes the <code>toString()</code> representation
	 * of the <code>SessionFactory</code> instance and appends {@link #PARTICIPATE_SUFFIX}.
	 */
	protected String getParticipateAttributeName() {
		return getSessionFactory().toString() + PARTICIPATE_SUFFIX;
	}

	private boolean applySessionBindingInterceptor(WebAsyncManager asyncManager, String key) {
		if (asyncManager.getCallableInterceptor(key) == null) {
			return false;
		}
		((SessionBindingCallableInterceptor) asyncManager.getCallableInterceptor(key)).initializeThread();
		return true;
	}


	/**
	 * Bind and unbind the Hibernate {@code Session} to the current thread.
	 */
	private class SessionBindingCallableInterceptor implements CallableProcessingInterceptor {

		private final SessionHolder sessionHolder;

		public SessionBindingCallableInterceptor(SessionHolder sessionHolder) {
			this.sessionHolder = sessionHolder;
		}

		public void preProcess(NativeWebRequest request, Callable<?> task) {
			initializeThread();
		}

		private void initializeThread() {
			TransactionSynchronizationManager.bindResource(getSessionFactory(), this.sessionHolder);
		}

		public void postProcess(NativeWebRequest request, Callable<?> task, Object concurrentResult) {
			TransactionSynchronizationManager.unbindResource(getSessionFactory());
		}
	}
}
