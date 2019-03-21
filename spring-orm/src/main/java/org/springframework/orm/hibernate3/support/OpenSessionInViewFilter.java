/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.orm.hibernate3.support;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet Filter that binds a Hibernate Session to the thread for the entire
 * processing of the request. Intended for the "Open Session in View" pattern,
 * i.e. to allow for lazy loading in web views despite the original transactions
 * already being completed.
 *
 * <p>This filter makes Hibernate Sessions available via the current thread, which
 * will be autodetected by transaction managers. It is suitable for service layer
 * transactions via {@link org.springframework.orm.hibernate3.HibernateTransactionManager}
 * or {@link org.springframework.transaction.jta.JtaTransactionManager} as well
 * as for non-transactional execution (if configured appropriately).
 *
 * <p><b>NOTE</b>: This filter will by default <i>not</i> flush the Hibernate Session,
 * with the flush mode set to {@code FlushMode.NEVER}. It assumes to be used
 * in combination with service layer transactions that care for the flushing: The
 * active transaction manager will temporarily change the flush mode to
 * {@code FlushMode.AUTO} during a read-write transaction, with the flush
 * mode reset to {@code FlushMode.NEVER} at the end of each transaction.
 * If you intend to use this filter without transactions, consider changing
 * the default flush mode (through the "flushMode" property).
 *
 * <p><b>WARNING:</b> Applying this filter to existing logic can cause issues that
 * have not appeared before, through the use of a single Hibernate Session for the
 * processing of an entire request. In particular, the reassociation of persistent
 * objects with a Hibernate Session has to occur at the very beginning of request
 * processing, to avoid clashes with already loaded instances of the same objects.
 *
 * <p>Alternatively, turn this filter into deferred close mode, by specifying
 * "singleSession"="false": It will not use a single session per request then,
 * but rather let each data access operation or transaction use its own session
 * (like without Open Session in View). Each of those sessions will be registered
 * for deferred close, though, actually processed at request completion.
 *
 * <p>A single session per request allows for most efficient first-level caching,
 * but can cause side effects, for example on {@code saveOrUpdate} or when
 * continuing after a rolled-back transaction. The deferred close strategy is as safe
 * as no Open Session in View in that respect, while still allowing for lazy loading
 * in views (but not providing a first-level cache for the entire request).
 *
 * <p>Looks up the SessionFactory in Spring's root web application context.
 * Supports a "sessionFactoryBeanName" filter init-param in {@code web.xml};
 * the default bean name is "sessionFactory".
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setSingleSession
 * @see #setFlushMode
 * @see #lookupSessionFactory
 * @see OpenSessionInViewInterceptor
 * @see OpenSessionInterceptor
 * @see org.springframework.orm.hibernate3.HibernateTransactionManager
 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#getSession
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 * @see org.hibernate.SessionFactory#getCurrentSession()
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class OpenSessionInViewFilter extends OncePerRequestFilter {

	public static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";


	private String sessionFactoryBeanName = DEFAULT_SESSION_FACTORY_BEAN_NAME;

	private boolean singleSession = true;

	private FlushMode flushMode = FlushMode.MANUAL;


	/**
	 * Set the bean name of the SessionFactory to fetch from Spring's
	 * root application context. Default is "sessionFactory".
	 * @see #DEFAULT_SESSION_FACTORY_BEAN_NAME
	 */
	public void setSessionFactoryBeanName(String sessionFactoryBeanName) {
		this.sessionFactoryBeanName = sessionFactoryBeanName;
	}

	/**
	 * Return the bean name of the SessionFactory to fetch from Spring's
	 * root application context.
	 */
	protected String getSessionFactoryBeanName() {
		return this.sessionFactoryBeanName;
	}

	/**
	 * Set whether to use a single session for each request. Default is "true".
	 * <p>If set to "false", each data access operation or transaction will use
	 * its own session (like without Open Session in View). Each of those
	 * sessions will be registered for deferred close, though, actually
	 * processed at request completion.
	 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#initDeferredClose
	 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#processDeferredClose
	 */
	public void setSingleSession(boolean singleSession) {
		this.singleSession = singleSession;
	}

	/**
	 * Return whether to use a single session for each request.
	 */
	protected boolean isSingleSession() {
		return this.singleSession;
	}

	/**
	 * Specify the Hibernate FlushMode to apply to this filter's
	 * {@link org.hibernate.Session}. Only applied in single session mode.
	 * <p>Can be populated with the corresponding constant name in XML bean
	 * definitions: e.g. "AUTO".
	 * <p>The default is "MANUAL". Specify "AUTO" if you intend to use
	 * this filter without service layer transactions.
	 * @see org.hibernate.Session#setFlushMode
	 * @see org.hibernate.FlushMode#MANUAL
	 * @see org.hibernate.FlushMode#AUTO
	 */
	public void setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	/**
	 * Return the Hibernate FlushMode that this filter applies to its
	 * {@link org.hibernate.Session} (in single session mode).
	 */
	protected FlushMode getFlushMode() {
		return this.flushMode;
	}


	/**
	 * Returns "false" so that the filter may re-bind the opened Hibernate
	 * {@code Session} to each asynchronously dispatched thread and postpone
	 * closing it until the very last asynchronous dispatch.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * Returns "false" so that the filter may provide a Hibernate
	 * {@code Session} to each error dispatches.
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		SessionFactory sessionFactory = lookupSessionFactory(request);
		boolean participate = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		String key = getAlreadyFilteredAttributeName();

		if (isSingleSession()) {
			// single session mode
			if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
				// Do not modify the Session: just set the participate flag.
				participate = true;
			}
			else {
				boolean isFirstRequest = !isAsyncDispatch(request);
				if (isFirstRequest || !applySessionBindingInterceptor(asyncManager, key)) {
					logger.debug("Opening single Hibernate Session in OpenSessionInViewFilter");
					Session session = getSession(sessionFactory);
					org.springframework.orm.hibernate3.SessionHolder sessionHolder = new org.springframework.orm.hibernate3.SessionHolder(session);
					TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);

					AsyncRequestInterceptor interceptor = new AsyncRequestInterceptor(sessionFactory, sessionHolder);
					asyncManager.registerCallableInterceptor(key, interceptor);
					asyncManager.registerDeferredResultInterceptor(key, interceptor);
				}
			}
		}
		else {
			// deferred close mode
			Assert.state(!isAsyncStarted(request), "Deferred close mode is not supported on async dispatches");
			if (org.springframework.orm.hibernate3.SessionFactoryUtils.isDeferredCloseActive(sessionFactory)) {
				// Do not modify deferred close: just set the participate flag.
				participate = true;
			}
			else {
				org.springframework.orm.hibernate3.SessionFactoryUtils.initDeferredClose(sessionFactory);
			}
		}

		try {
			filterChain.doFilter(request, response);
		}
		finally {
			if (!participate) {
				if (isSingleSession()) {
					// single session mode
					org.springframework.orm.hibernate3.SessionHolder sessionHolder =
							(org.springframework.orm.hibernate3.SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
					if (!isAsyncStarted(request)) {
						logger.debug("Closing single Hibernate Session in OpenSessionInViewFilter");
						closeSession(sessionHolder.getSession(), sessionFactory);
					}
				}
				else {
					// deferred close mode
					org.springframework.orm.hibernate3.SessionFactoryUtils.processDeferredClose(sessionFactory);
				}
			}
		}
	}

	/**
	 * Look up the SessionFactory that this filter should use,
	 * taking the current HTTP request as argument.
	 * <p>The default implementation delegates to the {@link #lookupSessionFactory()}
	 * variant without arguments.
	 * @param request the current request
	 * @return the SessionFactory to use
	 */
	protected SessionFactory lookupSessionFactory(HttpServletRequest request) {
		return lookupSessionFactory();
	}

	/**
	 * Look up the SessionFactory that this filter should use.
	 * <p>The default implementation looks for a bean with the specified name
	 * in Spring's root application context.
	 * @return the SessionFactory to use
	 * @see #getSessionFactoryBeanName
	 */
	protected SessionFactory lookupSessionFactory() {
		if (logger.isDebugEnabled()) {
			logger.debug("Using SessionFactory '" + getSessionFactoryBeanName() + "' for OpenSessionInViewFilter");
		}
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		return wac.getBean(getSessionFactoryBeanName(), SessionFactory.class);
	}

	/**
	 * Get a Session for the SessionFactory that this filter uses.
	 * Note that this just applies in single session mode!
	 * <p>The default implementation delegates to the
	 * {@code SessionFactoryUtils.getSession} method and
	 * sets the {@code Session}'s flush mode to "MANUAL".
	 * <p>Can be overridden in subclasses for creating a Session with a
	 * custom entity interceptor or JDBC exception translator.
	 * @param sessionFactory the SessionFactory that this filter uses
	 * @return the Session to use
	 * @throws DataAccessResourceFailureException if the Session could not be created
	 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#getSession(SessionFactory, boolean)
	 * @see org.hibernate.FlushMode#MANUAL
	 */
	protected Session getSession(SessionFactory sessionFactory) throws DataAccessResourceFailureException {
		Session session = org.springframework.orm.hibernate3.SessionFactoryUtils.getSession(sessionFactory, true);
		FlushMode flushMode = getFlushMode();
		if (flushMode != null) {
			session.setFlushMode(flushMode);
		}
		return session;
	}

	/**
	 * Close the given Session.
	 * Note that this just applies in single session mode!
	 * <p>Can be overridden in subclasses, e.g. for flushing the Session before
	 * closing it. See class-level javadoc for a discussion of flush handling.
	 * Note that you should also override getSession accordingly, to set
	 * the flush mode to something else than NEVER.
	 * @param session the Session used for filtering
	 * @param sessionFactory the SessionFactory that this filter uses
	 */
	protected void closeSession(Session session, SessionFactory sessionFactory) {
		org.springframework.orm.hibernate3.SessionFactoryUtils.closeSession(session);
	}

	private boolean applySessionBindingInterceptor(WebAsyncManager asyncManager, String key) {
		if (asyncManager.getCallableInterceptor(key) == null) {
			return false;
		}
		((AsyncRequestInterceptor) asyncManager.getCallableInterceptor(key)).bindSession();
		return true;
	}

}
