/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.orm.jpa.support;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockAsyncContext;
import org.springframework.web.testfixture.servlet.MockFilterConfig;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.testfixture.servlet.PassThroughFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

	private final TestTaskExecutor taskExecutor = new TestTaskExecutor();


	@BeforeEach
	public void setUp() {
		factory = mock(EntityManagerFactory.class);
		manager = mock(EntityManager.class);

		given(factory.createEntityManager()).willReturn(manager);

		this.request = new MockHttpServletRequest();
		this.request.setAsyncSupported(true);
		this.response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.request);
	}

	@AfterEach
	public void tearDown() {
		assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}

	@Test
	public void testOpenEntityManagerInViewInterceptor() {
		OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
		interceptor.setEntityManagerFactory(this.factory);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);

		interceptor.preHandle(new ServletWebRequest(request));
		assertThat(TransactionSynchronizationManager.hasResource(this.factory)).isTrue();

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
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

		given(manager.isOpen()).willReturn(true);

		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();

		verify(manager).close();
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorAsyncScenario() throws Exception {

		// Initial request thread

		OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
		interceptor.setEntityManagerFactory(factory);

		given(factory.createEntityManager()).willReturn(this.manager);

		interceptor.preHandle(this.webRequest);
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.webRequest);
		asyncManager.setTaskExecutor(this.taskExecutor);
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing((Callable<String>) () -> "anything");

		this.taskExecutor.await();
		assertThat(asyncManager.getConcurrentResult()).as("Concurrent result ").isEqualTo("anything");

		interceptor.afterConcurrentHandlingStarted(this.webRequest);
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();

		// Async dispatch thread

		interceptor.preHandle(this.webRequest);
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

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
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

		given(this.manager.isOpen()).willReturn(true);

		interceptor.afterCompletion(this.webRequest, null);
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();

		verify(this.manager).close();
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorAsyncTimeoutScenario() throws Exception {

		// Initial request thread

		OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
		interceptor.setEntityManagerFactory(factory);

		given(this.factory.createEntityManager()).willReturn(this.manager);

		interceptor.preHandle(this.webRequest);
		assertThat(TransactionSynchronizationManager.hasResource(this.factory)).isTrue();

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(this.taskExecutor);
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing((Callable<String>) () -> "anything");

		this.taskExecutor.await();
		assertThat(asyncManager.getConcurrentResult()).as("Concurrent result ").isEqualTo("anything");

		interceptor.afterConcurrentHandlingStarted(this.webRequest);
		assertThat(TransactionSynchronizationManager.hasResource(this.factory)).isFalse();

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
	public void testOpenEntityManagerInViewInterceptorAsyncErrorScenario() throws Exception {

		// Initial request thread

		OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
		interceptor.setEntityManagerFactory(factory);

		given(this.factory.createEntityManager()).willReturn(this.manager);

		interceptor.preHandle(this.webRequest);
		assertThat(TransactionSynchronizationManager.hasResource(this.factory)).isTrue();

		AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(this.taskExecutor);
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing((Callable<String>) () -> "anything");

		this.taskExecutor.await();
		assertThat(asyncManager.getConcurrentResult()).as("Concurrent result ").isEqualTo("anything");

		interceptor.afterConcurrentHandlingStarted(this.webRequest);
		assertThat(TransactionSynchronizationManager.hasResource(this.factory)).isFalse();

		// Async request error

		given(this.manager.isOpen()).willReturn(true);

		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		for (AsyncListener listener : asyncContext.getListeners()) {
			listener.onError(new AsyncEvent(asyncContext, new Exception()));
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

		final FilterChain filterChain = (servletRequest, servletResponse) -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			servletRequest.setAttribute("invoked", Boolean.TRUE);
		};

		final FilterChain filterChain2 = (servletRequest, servletResponse) -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory2)).isTrue();
			filter.doFilter(servletRequest, servletResponse, filterChain);
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(factory2)).isFalse();
		filter2.doFilter(request, response, filterChain3);
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(factory2)).isFalse();
		assertThat(request.getAttribute("invoked")).isNotNull();

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

		final AtomicInteger count = new AtomicInteger();

		final FilterChain filterChain = (servletRequest, servletResponse) -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			servletRequest.setAttribute("invoked", Boolean.TRUE);
			count.incrementAndGet();
		};

		final AtomicInteger count2 = new AtomicInteger();

		final FilterChain filterChain2 = (servletRequest, servletResponse) -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory2)).isTrue();
			filter.doFilter(servletRequest, servletResponse, filterChain);
			count2.incrementAndGet();
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		AsyncWebRequest asyncWebRequest = mock(AsyncWebRequest.class);
		given(asyncWebRequest.isAsyncStarted()).willReturn(true);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setTaskExecutor(this.taskExecutor);
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing((Callable<String>) () -> "anything");

		this.taskExecutor.await();
		assertThat(asyncManager.getConcurrentResult()).as("Concurrent result ").isEqualTo("anything");

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(factory2)).isFalse();
		filter2.doFilter(this.request, this.response, filterChain3);
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(factory2)).isFalse();
		assertThat(count.get()).isEqualTo(1);
		assertThat(count2.get()).isEqualTo(1);
		assertThat(request.getAttribute("invoked")).isNotNull();
		verify(asyncWebRequest, times(2)).addCompletionHandler(any(Runnable.class));
		verify(asyncWebRequest).addTimeoutHandler(any(Runnable.class));
		verify(asyncWebRequest, times(2)).addCompletionHandler(any(Runnable.class));
		verify(asyncWebRequest).startAsync();

		// Async dispatch after concurrent handling produces result ...

		reset(asyncWebRequest);
		given(asyncWebRequest.isAsyncStarted()).willReturn(false);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(factory2)).isFalse();
		filter.doFilter(this.request, this.response, filterChain3);
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(factory2)).isFalse();
		assertThat(count.get()).isEqualTo(2);
		assertThat(count2.get()).isEqualTo(2);

		verify(this.manager).close();
		verify(manager2).close();

		wac.close();
	}


	@SuppressWarnings("serial")
	private static class TestTaskExecutor extends SimpleAsyncTaskExecutor {

		private final CountDownLatch latch = new CountDownLatch(1);

		@Override
		public void execute(Runnable task, long startTimeout) {
			Runnable decoratedTask = () -> {
				try {
					task.run();
				}
				finally {
					latch.countDown();
				}
			};
			super.execute(decoratedTask, startTimeout);
		}

		void await() throws InterruptedException {
			this.latch.await(5, TimeUnit.SECONDS);
		}
	}

}
