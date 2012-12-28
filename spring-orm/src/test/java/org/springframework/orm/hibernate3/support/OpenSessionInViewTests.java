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

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.transaction.TransactionManager;

import org.easymock.EasyMock;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.mock.web.test.MockFilterConfig;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.mock.web.test.PassThroughFilterChain;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;


/**
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 05.03.2005
 */
public class OpenSessionInViewTests {

	private MockServletContext sc;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private ServletWebRequest webRequest;


	@Before
	public void setup() {
		this.sc = new MockServletContext();
		this.request = new MockHttpServletRequest(sc);
		this.response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.request);
	}

	@Test
	public void testOpenSessionInViewInterceptorWithSingleSession() throws Exception {

		SessionFactory sf = createStrictMock(SessionFactory.class);
		Session session = createStrictMock(Session.class);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		expect(sf.openSession()).andReturn(session);
		expect(session.getSessionFactory()).andReturn(sf);
		session.setFlushMode(FlushMode.MANUAL);
		expect(session.getSessionFactory()).andReturn(sf);
		expect(session.isOpen()).andReturn(true);
		replay(sf);
		replay(session);

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		// check that further invocations simply participate
		interceptor.preHandle(this.webRequest);
		assertEquals(session, SessionFactoryUtils.getSession(sf, false));

		interceptor.preHandle(this.webRequest);
		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		interceptor.preHandle(this.webRequest);
		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		verify(sf);
		verify(session);
		reset(sf);
		reset(session);
		replay(sf);
		replay(session);

		interceptor.postHandle(this.webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		verify(sf);
		verify(session);
		reset(sf);
		reset(session);
		expect(session.close()).andReturn(null);
		replay(sf);
		replay(session);

		interceptor.afterCompletion(this.webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));

		verify(sf);
		verify(session);
	}

	@Test
	public void testOpenSessionInViewInterceptorAsyncScenario() throws Exception {

		// Initial request thread

		final SessionFactory sf = createStrictMock(SessionFactory.class);
		Session session = createStrictMock(Session.class);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		expect(sf.openSession()).andReturn(session);
		expect(session.getSessionFactory()).andReturn(sf);
		session.setFlushMode(FlushMode.MANUAL);
		replay(sf);
		replay(session);

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		verify(sf);
		verify(session);

		AsyncWebRequest asyncWebRequest = createStrictMock(AsyncWebRequest.class);
		asyncWebRequest.addCompletionHandler((Runnable) anyObject());
		asyncWebRequest.addTimeoutHandler((Runnable) anyObject());
		asyncWebRequest.addCompletionHandler((Runnable) anyObject());
		asyncWebRequest.startAsync();
		replay(asyncWebRequest);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(new SyncTaskExecutor());
		asyncManager.setAsyncWebRequest(asyncWebRequest);

		asyncManager.startCallableProcessing(new Callable<String>() {
			public String call() throws Exception {
				return "anything";
			}
		});

		verify(asyncWebRequest);

		interceptor.afterConcurrentHandlingStarted(this.webRequest);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));

		// Async dispatch thread

		interceptor.preHandle(this.webRequest);
		assertTrue("Session not bound to async thread", TransactionSynchronizationManager.hasResource(sf));

		verify(sf);
		reset(sf);
		replay(sf);

		verify(session);
		reset(session);
		replay(session);

		interceptor.postHandle(this.webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		verify(sf);
		reset(sf);

		verify(session);
		reset(session);

		expect(session.close()).andReturn(null);

		replay(sf);
		replay(session);

		interceptor.afterCompletion(this.webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));

		verify(sf);
		verify(session);
	}

	@Test
	public void testOpenSessionInViewInterceptorWithSingleSessionAndJtaTm() throws Exception {
		final SessionFactoryImplementor sf = createStrictMock(SessionFactoryImplementor.class);
		Session session = createStrictMock(Session.class);

		TransactionManager tm = createStrictMock(TransactionManager.class);
		expect(tm.getTransaction()).andReturn(null);
		expect(tm.getTransaction()).andReturn(null);
		replay(tm);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		expect(sf.openSession()).andReturn(session);
		expect(sf.getTransactionManager()).andReturn(tm);
		session.setFlushMode(FlushMode.MANUAL);
		expect(sf.getTransactionManager()).andReturn(tm);
		expect(session.isOpen()).andReturn(true);

		replay(sf);
		replay(session);

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		// check that further invocations simply participate
		interceptor.preHandle(this.webRequest);

		assertEquals(session, SessionFactoryUtils.getSession(sf, false));

		interceptor.preHandle(this.webRequest);
		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		interceptor.preHandle(this.webRequest);
		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		verify(sf);
		verify(session);

		reset(sf);
		reset(session);
		replay(sf);
		replay(session);

		interceptor.postHandle(this.webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		verify(sf);
		verify(session);

		reset(sf);
		reset(session);
		expect(session.close()).andReturn(null);
		replay(sf);
		replay(session);

		interceptor.afterCompletion(this.webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));

		verify(sf);
		verify(session);
	}

	@Test
	public void testOpenSessionInViewInterceptorWithSingleSessionAndFlush() throws Exception {
		SessionFactory sf = createStrictMock(SessionFactory.class);
		Session session = createStrictMock(Session.class);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);
		interceptor.setFlushMode(HibernateAccessor.FLUSH_AUTO);

		expect(sf.openSession()).andReturn(session);
		expect(session.getSessionFactory()).andReturn(sf);
		replay(sf);
		replay(session);
		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));
		verify(sf);
		verify(session);

		reset(sf);
		reset(session);
		session.flush();
		replay(sf);
		replay(session);
		interceptor.postHandle(this.webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));
		verify(sf);
		verify(session);

		reset(sf);
		reset(session);
		expect(session.close()).andReturn(null);
		replay(sf);
		replay(session);
		interceptor.afterCompletion(this.webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		verify(sf);
		verify(session);
	}

	@Test
	public void testOpenSessionInViewInterceptorAndDeferredClose() throws Exception {
		SessionFactory sf = createStrictMock(SessionFactory.class);
		Session session = createStrictMock(Session.class);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);
		interceptor.setSingleSession(false);

		expect(sf.openSession()).andReturn(session);
		expect(session.getSessionFactory()).andReturn(sf);
		session.setFlushMode(FlushMode.MANUAL);
		replay(sf);
		replay(session);

		interceptor.preHandle(this.webRequest);
		org.hibernate.Session sess = SessionFactoryUtils.getSession(sf, true);
		SessionFactoryUtils.releaseSession(sess, sf);

		// check that further invocations simply participate
		interceptor.preHandle(this.webRequest);

		interceptor.preHandle(this.webRequest);
		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		interceptor.preHandle(this.webRequest);
		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		verify(sf);
		verify(session);

		reset(sf);
		reset(session);
		expect(session.close()).andReturn(null);
		replay(sf);
		replay(session);

		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		verify(sf);
		verify(session);
	}

	@Test
	public void testOpenSessionInViewFilterWithSingleSession() throws Exception {
		final SessionFactory sf = createStrictMock(SessionFactory.class);
		Session session = createStrictMock(Session.class);

		expect(sf.openSession()).andReturn(session);
		expect(session.getSessionFactory()).andReturn(sf);
		session.setFlushMode(FlushMode.MANUAL);
		expect(session.close()).andReturn(null);
		replay(sf);
		replay(session);

		final SessionFactory sf2 = createStrictMock(SessionFactory.class);
		Session session2 = createStrictMock(Session.class);

		expect(sf2.openSession()).andReturn(session2);
		expect(session2.getSessionFactory()).andReturn(sf2);
		session2.setFlushMode(FlushMode.AUTO);
		expect(session2.close()).andReturn(null);
		replay(sf2);
		replay(session2);

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sf);
		wac.getDefaultListableBeanFactory().registerSingleton("mySessionFactory", sf2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

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
		filter2.doFilter(this.request, this.response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		assertFalse(TransactionSynchronizationManager.hasResource(sf2));
		assertNotNull(this.request.getAttribute("invoked"));

		verify(sf);
		verify(session);
		verify(sf2);
		verify(session2);

		wac.close();
	}

	@Test
	public void testOpenSessionInViewFilterAsyncScenario() throws Exception {
		final SessionFactory sf = createStrictMock(SessionFactory.class);
		Session session = createStrictMock(Session.class);

		// Initial request during which concurrent handling starts..

		expect(sf.openSession()).andReturn(session);
		expect(session.getSessionFactory()).andReturn(sf);
		session.setFlushMode(FlushMode.MANUAL);
		replay(sf);
		replay(session);

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sf);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");

		final AtomicInteger count = new AtomicInteger(0);

		final OpenSessionInViewFilter filter = new OpenSessionInViewFilter();
		filter.init(filterConfig);

		final FilterChain filterChain = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(sf));
				count.incrementAndGet();
			}
		};

		AsyncWebRequest asyncWebRequest = createMock(AsyncWebRequest.class);
		asyncWebRequest.addCompletionHandler((Runnable) anyObject());
		asyncWebRequest.addTimeoutHandler(EasyMock.<Runnable>anyObject());
		asyncWebRequest.addCompletionHandler((Runnable) anyObject());
		asyncWebRequest.startAsync();
		expect(asyncWebRequest.isAsyncStarted()).andReturn(true).anyTimes();
		replay(asyncWebRequest);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(new SyncTaskExecutor());
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			public String call() throws Exception {
				return "anything";
			}
		});

		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		filter.doFilter(this.request, this.response, filterChain);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		assertEquals(1, count.get());

		verify(sf);
		verify(session);
		verify(asyncWebRequest);

		reset(sf);
		reset(session);
		reset(asyncWebRequest);

		// Async dispatch after concurrent handling produces result ...

		expect(session.close()).andReturn(null);
		expect(asyncWebRequest.isAsyncStarted()).andReturn(false).anyTimes();

		replay(sf);
		replay(session);
		replay(asyncWebRequest);

		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		filter.doFilter(this.request, this.response, filterChain);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		assertEquals(2, count.get());

		verify(sf);
		verify(session);
		verify(asyncWebRequest);

		wac.close();
	}

	@Test
	public void testOpenSessionInViewFilterWithSingleSessionAndPreBoundSession() throws Exception {
		final SessionFactory sf = createStrictMock(SessionFactory.class);
		Session session = createStrictMock(Session.class);

		expect(sf.openSession()).andReturn(session);
		expect(session.getSessionFactory()).andReturn(sf);
		session.setFlushMode(FlushMode.MANUAL);
		expect(session.close()).andReturn(null);
		replay(sf);
		replay(session);

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sf);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig2.addInitParameter("sessionFactoryBeanName", "mySessionFactory");

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		interceptor.preHandle(this.webRequest);

		final OpenSessionInViewFilter filter = new OpenSessionInViewFilter();
		filter.init(filterConfig);

		final FilterChain filterChain = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(sf));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		assertTrue(TransactionSynchronizationManager.hasResource(sf));
		filter.doFilter(this.request, this.response, filterChain);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));
		assertNotNull(this.request.getAttribute("invoked"));

		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		verify(sf);
		verify(session);

		wac.close();
	}

	@Test
	public void testOpenSessionInViewFilterWithDeferredClose() throws Exception {
		final SessionFactory sf = createStrictMock(SessionFactory.class);
		final Session session = createStrictMock(Session.class);

		expect(sf.openSession()).andReturn(session);
		expect(session.getSessionFactory()).andReturn(sf);
		expect(session.getFlushMode()).andReturn(FlushMode.MANUAL);
		session.setFlushMode(FlushMode.MANUAL);
		replay(sf);
		replay(session);

		final SessionFactory sf2 = createStrictMock(SessionFactory.class);
		final Session session2 = createStrictMock(Session.class);

		Transaction tx = createStrictMock(Transaction.class);
		Connection con = createStrictMock(Connection.class);

		expect(sf2.openSession()).andReturn(session2);
		expect(session2.connection()).andReturn(con);
		expect(session2.beginTransaction()).andReturn(tx);
		expect(session2.isConnected()).andReturn(true);
		expect(session2.connection()).andReturn(con);
		tx.commit();
		expect(con.isReadOnly()).andReturn(false);
		session2.setFlushMode(FlushMode.MANUAL);

		replay(sf2);
		replay(session2);
		replay(tx);
		replay(con);

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sf);
		wac.getDefaultListableBeanFactory().registerSingleton("mySessionFactory", sf2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

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

				verify(session);
				reset(session);

				expect(session.close()).andReturn(null);
				replay(session);

				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IOException, ServletException {

				HibernateTransactionManager tm = new HibernateTransactionManager(sf2);
				TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
				tm.commit(ts);

				verify(session2);
				reset(session2);

				expect(session2.close()).andReturn(null);
				replay(session2);

				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		filter2.doFilter(this.request, this.response, filterChain3);
		assertNotNull(this.request.getAttribute("invoked"));

		verify(sf);
		verify(session);

		verify(sf2);
		verify(session2);
		verify(tx);
		verify(con);

		wac.close();
	}

	@Test
	public void testOpenSessionInViewFilterWithDeferredCloseAndAlreadyActiveDeferredClose() throws Exception {
		final SessionFactory sf = createStrictMock(SessionFactory.class);
		final Session session = createStrictMock(Session.class);

		expect(sf.openSession()).andReturn(session);
		expect(session.getSessionFactory()).andReturn(sf);
		expect(session.getFlushMode()).andReturn(FlushMode.MANUAL);
		session.setFlushMode(FlushMode.MANUAL);
		replay(sf);
		replay(session);

//		sf.openSession();
//		sfControl.setReturnValue(session, 1);
//		session.getSessionFactory();
//		sessionControl.setReturnValue(sf);
//		session.getFlushMode();
//		sessionControl.setReturnValue(FlushMode.MANUAL, 1);
//		session.setFlushMode(FlushMode.MANUAL);
//		sessionControl.setVoidCallable(1);
//		sfControl.replay();
//		sessionControl.replay();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sf);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig.addInitParameter("singleSession", "false");
		filterConfig2.addInitParameter("singleSession", "false");
		filterConfig2.addInitParameter("sessionFactoryBeanName", "mySessionFactory");

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);
		interceptor.setSingleSession(false);

		interceptor.preHandle(webRequest);

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

				verify(session);
				reset(session);
				try {
					expect(session.close()).andReturn(null);
				}
				catch (HibernateException ex) {
				}
				replay(session);

				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		FilterChain filterChain2 = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IOException, ServletException {
				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		filter.doFilter(this.request, this.response, filterChain2);
		assertNotNull(this.request.getAttribute("invoked"));

		interceptor.postHandle(webRequest, null);
		interceptor.afterCompletion(webRequest, null);

		verify(sf);
		verify(session);

		wac.close();
	}


	@SuppressWarnings("serial")
	private static class SyncTaskExecutor extends SimpleAsyncTaskExecutor {

		@Override
		public void execute(Runnable task, long startTimeout) {
			task.run();
		}
	}
}
