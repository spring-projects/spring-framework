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

package org.springframework.orm.hibernate3.support;

import java.io.IOException;
import java.sql.Connection;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.transaction.TransactionManager;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;

import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.PassThroughFilterChain;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * @author Juergen Hoeller
 * @since 05.03.2005
 */
public class OpenSessionInViewTests extends TestCase {

	public void testOpenSessionInViewInterceptorWithSingleSession() throws Exception {
		
		//SessionFactory sf = createMock(SessionFactory.class);
		//Session session = createMock(Session.class);
		
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		
		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);

		//expect(mockStorage.size()).andReturn(expectedValue);

		//expect(sf.openSession()).andReturn(session);
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 2);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);
		sfControl.replay();
		sessionControl.replay();
		interceptor.preHandle(new ServletWebRequest(request));
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		// check that further invocations simply participate
		interceptor.preHandle(new ServletWebRequest(request));

		assertEquals(session, SessionFactoryUtils.getSession(sf, false));

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		sfControl.verify();
		sessionControl.verify();

		sfControl.reset();
		sessionControl.reset();
		sfControl.replay();
		sessionControl.replay();
		interceptor.postHandle(new ServletWebRequest(request), null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));
		sfControl.verify();
		sessionControl.verify();

		sfControl.reset();
		sessionControl.reset();
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		sfControl.verify();
		sessionControl.verify();
	}

	public void testOpenSessionInViewInterceptorWithSingleSessionAndJtaTm() throws Exception {
		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();

		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		tm.getTransaction();
		tmControl.setReturnValue(null, 2);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		sf.getTransactionManager();
		sfControl.setReturnValue(tm, 2);
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);

		tmControl.replay();
		sfControl.replay();
		sessionControl.replay();

		interceptor.preHandle(new ServletWebRequest(request));
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		// check that further invocations simply participate
		interceptor.preHandle(new ServletWebRequest(request));

		assertEquals(session, SessionFactoryUtils.getSession(sf, false));

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		sfControl.verify();
		sessionControl.verify();

		sfControl.reset();
		sessionControl.reset();
		sfControl.replay();
		sessionControl.replay();
		interceptor.postHandle(new ServletWebRequest(request), null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));
		sfControl.verify();
		sessionControl.verify();

		sfControl.reset();
		sessionControl.reset();
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		sfControl.verify();
		sessionControl.verify();
	}

	public void testOpenSessionInViewInterceptorWithSingleSessionAndFlush() throws Exception {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);
		interceptor.setFlushMode(HibernateAccessor.FLUSH_AUTO);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf);
		sfControl.replay();
		sessionControl.replay();
		interceptor.preHandle(new ServletWebRequest(request));
		assertTrue(TransactionSynchronizationManager.hasResource(sf));
		sfControl.verify();
		sessionControl.verify();

		sfControl.reset();
		sessionControl.reset();
		session.flush();
		sessionControl.setVoidCallable(1);
		sfControl.replay();
		sessionControl.replay();
		interceptor.postHandle(new ServletWebRequest(request), null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));
		sfControl.verify();
		sessionControl.verify();

		sfControl.reset();
		sessionControl.reset();
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		sfControl.verify();
		sessionControl.verify();
	}

	public void testOpenSessionInViewInterceptorAndDeferredClose() throws Exception {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);
		interceptor.setSingleSession(false);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);
		sfControl.replay();
		sessionControl.replay();

		interceptor.preHandle(new ServletWebRequest(request));
		org.hibernate.Session sess = SessionFactoryUtils.getSession(sf, true);
		SessionFactoryUtils.releaseSession(sess, sf);

		// check that further invocations simply participate
		interceptor.preHandle(new ServletWebRequest(request));

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		interceptor.preHandle(new ServletWebRequest(request));
		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		sfControl.verify();
		sessionControl.verify();
		sfControl.reset();
		sessionControl.reset();

		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);
		sfControl.verify();
		sessionControl.verify();
	}

	public void testOpenSessionInViewFilterWithSingleSession() throws Exception {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		MockControl sf2Control = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf2 = (SessionFactory) sf2Control.getMock();
		MockControl session2Control = MockControl.createControl(Session.class);
		Session session2 = (Session) session2Control.getMock();

		sf2.openSession();
		sf2Control.setReturnValue(session2, 1);
		session2.getSessionFactory();
		session2Control.setReturnValue(sf);
		session2.setFlushMode(FlushMode.AUTO);
		session2Control.setVoidCallable(1);
		session2.close();
		session2Control.setReturnValue(null, 1);
		sf2Control.replay();
		session2Control.replay();

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sf);
		wac.getDefaultListableBeanFactory().registerSingleton("mySessionFactory", sf2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig2.addInitParameter("sessionFactoryBeanName", "mySessionFactory");
		filterConfig2.addInitParameter("flushMode", "AUTO");

		final OpenSessionInViewFilter filter = new OpenSessionInViewFilter();
		filter.init(filterConfig);
		final OpenSessionInViewFilter filter2 = new OpenSessionInViewFilter();
		filter2.init(filterConfig2);

		final FilterChain filterChain = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(sf));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
			    throws IOException, ServletException {
				assertTrue(TransactionSynchronizationManager.hasResource(sf2));
				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		assertFalse(TransactionSynchronizationManager.hasResource(sf2));
		filter2.doFilter(request, response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		assertFalse(TransactionSynchronizationManager.hasResource(sf2));
		assertNotNull(request.getAttribute("invoked"));

		sfControl.verify();
		sessionControl.verify();
		sf2Control.verify();
		session2Control.verify();

		wac.close();
	}

	public void testOpenSessionInViewFilterWithSingleSessionAndPreBoundSession() throws Exception {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sf);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig2.addInitParameter("sessionFactoryBeanName", "mySessionFactory");

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		interceptor.preHandle(new ServletWebRequest(request));

		final OpenSessionInViewFilter filter = new OpenSessionInViewFilter();
		filter.init(filterConfig);

		final FilterChain filterChain = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(sf));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		assertTrue(TransactionSynchronizationManager.hasResource(sf));
		filter.doFilter(request, response, filterChain);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));
		assertNotNull(request.getAttribute("invoked"));

		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		sfControl.verify();
		sessionControl.verify();

		wac.close();
	}

	public void testOpenSessionInViewFilterWithDeferredClose() throws Exception {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.MANUAL, 1);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);
		sfControl.replay();
		sessionControl.replay();

		MockControl sf2Control = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf2 = (SessionFactory) sf2Control.getMock();
		final MockControl session2Control = MockControl.createControl(Session.class);
		final Session session2 = (Session) session2Control.getMock();
		MockControl txControl = MockControl.createControl(Transaction.class);
		Transaction tx = (Transaction) txControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();

		sf2.openSession();
		sf2Control.setReturnValue(session2, 1);
		session2.beginTransaction();
		session2Control.setReturnValue(tx, 1);
		session2.connection();
		session2Control.setReturnValue(con, 2);
		tx.commit();
		txControl.setVoidCallable(1);
		session2.isConnected();
		session2Control.setReturnValue(true, 1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		session2.setFlushMode(FlushMode.MANUAL);
		session2Control.setVoidCallable(1);

		sf2Control.replay();
		session2Control.replay();
		txControl.replay();
		conControl.replay();

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sf);
		wac.getDefaultListableBeanFactory().registerSingleton("mySessionFactory", sf2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig.addInitParameter("singleSession", "false");
		filterConfig2.addInitParameter("singleSession", "false");
		filterConfig2.addInitParameter("sessionFactoryBeanName", "mySessionFactory");

		final OpenSessionInViewFilter filter = new OpenSessionInViewFilter();
		filter.init(filterConfig);
		final OpenSessionInViewFilter filter2 = new OpenSessionInViewFilter();
		filter2.init(filterConfig2);

		final FilterChain filterChain = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				HibernateTransactionManager tm = new HibernateTransactionManager(sf);
				TransactionStatus ts = tm.getTransaction(
						new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS));
				org.hibernate.Session sess = SessionFactoryUtils.getSession(sf, true);
				SessionFactoryUtils.releaseSession(sess, sf);
				tm.commit(ts);

				sessionControl.verify();
				sessionControl.reset();

				session.close();
				sessionControl.setReturnValue(null, 1);
				sessionControl.replay();

				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
			    throws IOException, ServletException {

				HibernateTransactionManager tm = new HibernateTransactionManager(sf2);
				TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
				tm.commit(ts);

				session2Control.verify();
				session2Control.reset();

				session2.close();
				session2Control.setReturnValue(null, 1);
				session2Control.replay();

				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		filter2.doFilter(request, response, filterChain3);
		assertNotNull(request.getAttribute("invoked"));

		sfControl.verify();
		sessionControl.verify();
		sf2Control.verify();
		session2Control.verify();
		txControl.verify();
		conControl.verify();

		wac.close();
	}

	public void testOpenSessionInViewFilterWithDeferredCloseAndAlreadyActiveDeferredClose() throws Exception {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.MANUAL, 1);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);
		sfControl.replay();
		sessionControl.replay();

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sf);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig.addInitParameter("singleSession", "false");
		filterConfig2.addInitParameter("singleSession", "false");
		filterConfig2.addInitParameter("sessionFactoryBeanName", "mySessionFactory");

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);
		interceptor.setSingleSession(false);

		interceptor.preHandle(new ServletWebRequest(request));

		final OpenSessionInViewFilter filter = new OpenSessionInViewFilter();
		filter.init(filterConfig);
		final OpenSessionInViewFilter filter2 = new OpenSessionInViewFilter();
		filter2.init(filterConfig2);

		final FilterChain filterChain = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				HibernateTransactionManager tm = new HibernateTransactionManager(sf);
				TransactionStatus ts = tm.getTransaction(
						new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS));
				org.hibernate.Session sess = SessionFactoryUtils.getSession(sf, true);
				SessionFactoryUtils.releaseSession(sess, sf);
				tm.commit(ts);

				sessionControl.verify();
				sessionControl.reset();
				try {
					session.close();
				}
				catch (HibernateException ex) {
				}
				sessionControl.setReturnValue(null, 1);
				sessionControl.replay();

				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		FilterChain filterChain2 = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
			    throws IOException, ServletException {
				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		filter.doFilter(request, response, filterChain2);
		assertNotNull(request.getAttribute("invoked"));

		interceptor.postHandle(new ServletWebRequest(request), null);
		interceptor.afterCompletion(new ServletWebRequest(request), null);

		sfControl.verify();
		sessionControl.verify();

		wac.close();
	}

}
