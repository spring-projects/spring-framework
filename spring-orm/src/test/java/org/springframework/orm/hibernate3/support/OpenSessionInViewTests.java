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

package org.springframework.orm.hibernate3.support;

import java.io.IOException;
import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.transaction.TransactionManager;

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.mock.web.test.MockAsyncContext;
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
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.util.NestedServletException;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 05.03.2005
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class OpenSessionInViewTests {

	private MockServletContext sc;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private ServletWebRequest webRequest;


	@Before
	public void setup() {
		this.sc = new MockServletContext();
		this.request = new MockHttpServletRequest(sc);
		this.request.setAsyncSupported(true);
		this.response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.request);
	}

	@Test
	public void testOpenSessionInterceptor() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);

		OpenSessionInterceptor interceptor = new OpenSessionInterceptor();
		interceptor.setSessionFactory(sf);

		Runnable tb = new Runnable() {
			@Override
			public void run() {
				assertTrue(TransactionSynchronizationManager.hasResource(sf));
				assertEquals(session, SessionFactoryUtils.getSession(sf, false));
			}
		};
		ProxyFactory pf = new ProxyFactory(tb);
		pf.addAdvice(interceptor);
		Runnable tbProxy = (Runnable) pf.getProxy();

		given(sf.openSession()).willReturn(session);
		given(session.isOpen()).willReturn(true);
		tbProxy.run();
		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session).close();
	}

	@Test
	public void testOpenSessionInViewInterceptorWithSingleSession() throws Exception {
		SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.isOpen()).willReturn(true);

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

		interceptor.postHandle(this.webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		interceptor.afterCompletion(this.webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session).close();
	}

	@Test
	public void testOpenSessionInViewInterceptorAsyncScenario() throws Exception {
		// Initial request thread

		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(new SyncTaskExecutor());
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "anything";
			}
		});

		interceptor.afterConcurrentHandlingStarted(this.webRequest);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));

		// Async dispatch thread

		interceptor.preHandle(this.webRequest);
		assertTrue("Session not bound to async thread", TransactionSynchronizationManager.hasResource(sf));

		interceptor.postHandle(this.webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		verify(session, never()).close();

		interceptor.afterCompletion(this.webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session).close();
	}

	@Test
	public void testOpenSessionInViewInterceptorAsyncTimeoutScenario() throws Exception {
		// Initial request thread

		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(new SyncTaskExecutor());
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "anything";
			}
		});

		interceptor.afterConcurrentHandlingStarted(this.webRequest);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		verify(session, never()).close();

		// Async request timeout

		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		for (AsyncListener listener : asyncContext.getListeners()) {
			listener.onTimeout(new AsyncEvent(asyncContext));
		}
		for (AsyncListener listener : asyncContext.getListeners()) {
			listener.onComplete(new AsyncEvent(asyncContext));
		}

		verify(session).close();
	}

	@Test
	public void testOpenSessionInViewInterceptorWithSingleSessionAndJtaTm() throws Exception {
		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		Session session = mock(Session.class);

		TransactionManager tm = mock(TransactionManager.class);
		given(tm.getTransaction()).willReturn(null);
		given(tm.getTransaction()).willReturn(null);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);

		given(sf.openSession()).willReturn(session);
		given(sf.getTransactionManager()).willReturn(tm);
		given(sf.getTransactionManager()).willReturn(tm);
		given(session.isOpen()).willReturn(true);

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		// Check that further invocations simply participate
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

		interceptor.postHandle(this.webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		interceptor.afterCompletion(this.webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session).close();
	}

	@Test
	public void testOpenSessionInViewInterceptorWithSingleSessionAndFlush() throws Exception {
		SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);
		interceptor.setFlushMode(HibernateAccessor.FLUSH_AUTO);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		interceptor.postHandle(this.webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(sf));

		interceptor.afterCompletion(this.webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testOpenSessionInViewInterceptorAndDeferredClose() throws Exception {
		SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);

		OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();
		interceptor.setSessionFactory(sf);
		interceptor.setSingleSession(false);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);

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

		interceptor.postHandle(this.webRequest, null);
		interceptor.afterCompletion(this.webRequest, null);

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session).close();
	}

	@Test
	public void testOpenSessionInViewFilterWithSingleSession() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.close()).willReturn(null);

		final SessionFactory sf2 = mock(SessionFactory.class);
		Session session2 = mock(Session.class);

		given(sf2.openSession()).willReturn(session2);
		given(session2.getSessionFactory()).willReturn(sf2);
		given(session2.close()).willReturn(null);

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
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(sf));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
			@Override
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

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session2).setFlushMode(FlushMode.AUTO);
		wac.close();
	}

	@Test
	public void testOpenSessionInViewFilterAsyncScenario() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);

		// Initial request during which concurrent handling starts..

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);

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
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(sf));
				count.incrementAndGet();
			}
		};

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(new SyncTaskExecutor());
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "anything";
			}
		});

		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		filter.doFilter(this.request, this.response, filterChain);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		assertEquals(1, count.get());
		verify(session, never()).close();

		// Async dispatch after concurrent handling produces result ...

		this.request.setAsyncStarted(false);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		filter.doFilter(this.request, this.response, filterChain);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		assertEquals(2, count.get());

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session).close();

		wac.close();
	}

	@Test
	public void testOpenSessionInViewFilterAsyncTimeoutScenario() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);

		// Initial request during which concurrent handling starts..

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("sessionFactory", sf);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		final OpenSessionInViewFilter filter = new OpenSessionInViewFilter();
		filter.init(filterConfig);

		final AtomicInteger count = new AtomicInteger(0);
		final AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		final MockHttpServletRequest request = this.request;

		final FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
					throws NestedServletException {

				assertTrue(TransactionSynchronizationManager.hasResource(sf));
				count.incrementAndGet();

				WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
				asyncManager.setTaskExecutor(new SyncTaskExecutor());
				asyncManager.setAsyncWebRequest(asyncWebRequest);
				try {
					asyncManager.startCallableProcessing(new Callable<String>() {
						@Override
						public String call() throws Exception {
							return "anything";
						}
					});
				}
				catch (Exception e) {
					throw new NestedServletException("", e);
				}
			}
		};

		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		filter.doFilter(this.request, this.response, filterChain);
		assertFalse(TransactionSynchronizationManager.hasResource(sf));
		assertEquals(1, count.get());
		verify(session, never()).close();

		// Async request timeout ...

		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		for (AsyncListener listener : asyncContext.getListeners()) {
			listener.onTimeout(new AsyncEvent(asyncContext));
		}
		for (AsyncListener listener : asyncContext.getListeners()) {
			listener.onComplete(new AsyncEvent(asyncContext));
		}

		verify(session).close();

		wac.close();
	}

	@Test
	public void testOpenSessionInViewFilterWithSingleSessionAndPreBoundSession() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		Session session = mock(Session.class);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);

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
			@Override
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

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session).close();

		wac.close();
	}

	@Test
	public void testOpenSessionInViewFilterWithDeferredClose() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);

		final SessionFactory sf2 = mock(SessionFactory.class);
		final Session session2 = mock(Session.class);

		Transaction tx = mock(Transaction.class);
		Connection con = mock(Connection.class);

		given(sf2.openSession()).willReturn(session2);
		given(session2.connection()).willReturn(con);
		given(session2.beginTransaction()).willReturn(tx);
		given(session2.isConnected()).willReturn(true);
		given(session2.connection()).willReturn(con);

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
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				HibernateTransactionManager tm = new HibernateTransactionManager(sf);
				TransactionStatus ts = tm.getTransaction(
						new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS));
				org.hibernate.Session sess = SessionFactoryUtils.getSession(sf, true);
				SessionFactoryUtils.releaseSession(sess, sf);
				tm.commit(ts);
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IOException, ServletException {
				HibernateTransactionManager tm = new HibernateTransactionManager(sf2);
				TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
				tm.commit(ts);
				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		filter2.doFilter(this.request, this.response, filterChain3);
		assertNotNull(this.request.getAttribute("invoked"));

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(tx).commit();
		verify(session2).setFlushMode(FlushMode.MANUAL);
		verify(session).close();
		verify(session2).close();

		wac.close();
	}

	@Test
	public void testOpenSessionInViewFilterWithDeferredCloseAndAlreadyActiveDeferredClose() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);

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
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				HibernateTransactionManager tm = new HibernateTransactionManager(sf);
				TransactionStatus ts = tm.getTransaction(
						new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS));
				org.hibernate.Session sess = SessionFactoryUtils.getSession(sf, true);
				SessionFactoryUtils.releaseSession(sess, sf);
				tm.commit(ts);
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		FilterChain filterChain2 = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IOException, ServletException {
				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		filter.doFilter(this.request, this.response, filterChain2);
		assertNotNull(this.request.getAttribute("invoked"));

		interceptor.postHandle(webRequest, null);
		interceptor.afterCompletion(webRequest, null);

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session).close();

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
