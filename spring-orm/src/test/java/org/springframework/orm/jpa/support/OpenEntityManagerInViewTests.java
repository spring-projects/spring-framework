/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.orm.jpa.support;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.mock.web.test.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 */
public class OpenEntityManagerInViewTests {

	private EntityManager manager;

	private EntityManagerFactory factory;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private ServletWebRequest webRequest;


	@Before
	public void setUp() throws Exception {
		factory = mock(EntityManagerFactory.class);
		manager = mock(EntityManager.class);

		given(factory.createEntityManager()).willReturn(manager);

		this.request = new MockHttpServletRequest();
		this.request.setAsyncSupported(true);
		this.response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.request);
	}

	@After
	public void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	@Test
	public void testOpenEntityManagerInViewInterceptor() throws Exception {
		OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
		interceptor.setEntityManagerFactory(this.factory);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);

		interceptor.preHandle(new ServletWebRequest(request));
		assertTrue(TransactionSynchronizationManager.hasResource(this.factory));

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

		interceptor.postHandle(new ServletWebRequest(request), null);
		assertTrue(TransactionSynchronizationManager.hasResource(factory));

		given(manager.isOpen()).willReturn(true);

		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));

		verify(manager).close();
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorAsyncScenario() throws Exception {

		// Initial request thread

		OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
		interceptor.setEntityManagerFactory(factory);

		given(factory.createEntityManager()).willReturn(this.manager);

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(factory));

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.webRequest);
		asyncManager.setTaskExecutor(new SyncTaskExecutor());
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "anything";
			}
		});

		interceptor.afterConcurrentHandlingStarted(this.webRequest);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));

		// Async dispatch thread

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(factory));

		asyncManager.clearConcurrentResult();

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

		interceptor.postHandle(this.webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(factory));

		given(this.manager.isOpen()).willReturn(true);

		interceptor.afterCompletion(this.webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));

		verify(this.manager).close();
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorAsyncTimeoutScenario() throws Exception {

		// Initial request thread

		OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
		interceptor.setEntityManagerFactory(factory);

		given(this.factory.createEntityManager()).willReturn(this.manager);

		interceptor.preHandle(this.webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(this.factory));

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
		assertFalse(TransactionSynchronizationManager.hasResource(this.factory));

		// Async request timeout

		given(this.manager.isOpen()).willReturn(true);

		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		for (AsyncListener listener : asyncContext.getListeners()) {
			listener.onTimeout(new AsyncEvent(asyncContext));
		}
		for (AsyncListener listener : asyncContext.getListeners()) {
			listener.onComplete(new AsyncEvent(asyncContext));
		}

		verify(this.manager).close();
	}

	@Test
	public void testOpenEntityManagerInViewFilter() throws Exception {
		given(manager.isOpen()).willReturn(true);

		final EntityManagerFactory factory2 = mock(EntityManagerFactory.class);
		final EntityManager manager2 = mock(EntityManager.class);

		given(factory2.createEntityManager()).willReturn(manager2);
		given(manager2.isOpen()).willReturn(true);

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", factory);
		wac.getDefaultListableBeanFactory().registerSingleton("myEntityManagerFactory", factory2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig2.addInitParameter("entityManagerFactoryBeanName", "myEntityManagerFactory");

		final OpenEntityManagerInViewFilter filter = new OpenEntityManagerInViewFilter();
		filter.init(filterConfig);
		final OpenEntityManagerInViewFilter filter2 = new OpenEntityManagerInViewFilter();
		filter2.init(filterConfig2);

		final FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(factory));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IOException, ServletException {
				assertTrue(TransactionSynchronizationManager.hasResource(factory2));
				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		assertFalse(TransactionSynchronizationManager.hasResource(factory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		filter2.doFilter(request, response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		assertNotNull(request.getAttribute("invoked"));

		verify(manager).close();
		verify(manager2).close();

		wac.close();
	}

	@Test
	public void testOpenEntityManagerInViewFilterAsyncScenario() throws Exception {
		given(manager.isOpen()).willReturn(true);

		final EntityManagerFactory factory2 = mock(EntityManagerFactory.class);
		final EntityManager manager2 = mock(EntityManager.class);

		given(factory2.createEntityManager()).willReturn(manager2);
		given(manager2.isOpen()).willReturn(true);

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("entityManagerFactory", factory);
		wac.getDefaultListableBeanFactory().registerSingleton("myEntityManagerFactory", factory2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig2.addInitParameter("entityManagerFactoryBeanName", "myEntityManagerFactory");

		final OpenEntityManagerInViewFilter filter = new OpenEntityManagerInViewFilter();
		filter.init(filterConfig);
		final OpenEntityManagerInViewFilter filter2 = new OpenEntityManagerInViewFilter();
		filter2.init(filterConfig2);

		final AtomicInteger count = new AtomicInteger(0);

		final FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(factory));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
				count.incrementAndGet();
			}
		};

		final AtomicInteger count2 = new AtomicInteger(0);

		final FilterChain filterChain2 = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IOException, ServletException {
				assertTrue(TransactionSynchronizationManager.hasResource(factory2));
				filter.doFilter(servletRequest, servletResponse, filterChain);
				count2.incrementAndGet();
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		AsyncWebRequest asyncWebRequest = mock(AsyncWebRequest.class);
		given(asyncWebRequest.isAsyncStarted()).willReturn(true);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(new SyncTaskExecutor());
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "anything";
			}
		});

		assertFalse(TransactionSynchronizationManager.hasResource(factory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		filter2.doFilter(this.request, this.response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		assertEquals(1, count.get());
		assertEquals(1, count2.get());
		assertNotNull(request.getAttribute("invoked"));
		verify(asyncWebRequest, times(2)).addCompletionHandler(any(Runnable.class));
		verify(asyncWebRequest).addTimeoutHandler(any(Runnable.class));
		verify(asyncWebRequest, times(2)).addCompletionHandler(any(Runnable.class));
		verify(asyncWebRequest).startAsync();

		// Async dispatch after concurrent handling produces result ...

		reset(asyncWebRequest);
		given(asyncWebRequest.isAsyncStarted()).willReturn(false);

		assertFalse(TransactionSynchronizationManager.hasResource(factory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		filter.doFilter(this.request, this.response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		assertEquals(2, count.get());
		assertEquals(2, count2.get());

		verify(this.manager).close();
		verify(manager2).close();

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
