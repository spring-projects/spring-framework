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

package org.springframework.orm.jpa.support;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.PassThroughFilterChain;
import org.springframework.orm.jpa.JpaTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class OpenEntityManagerInViewTests extends TestCase {

	private EntityManager manager;

	private EntityManagerFactory factory;

	private JpaTemplate template;


	@Override
	protected void setUp() throws Exception {
		factory = createMock(EntityManagerFactory.class);
		manager = createMock(EntityManager.class);

		template = new JpaTemplate(factory);
		template.afterPropertiesSet();

		expect(factory.createEntityManager()).andReturn(manager);
	}

	@Override
	protected void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	public void testOpenEntityManagerInViewInterceptor() throws Exception {
		OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
		interceptor.setEntityManagerFactory(factory);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);

		replay(manager, factory);

		interceptor.preHandle(new ServletWebRequest(request));
		assertTrue(TransactionSynchronizationManager.hasResource(factory));

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

		verify(manager, factory);

		reset(manager, factory);
		replay(manager, factory);

		interceptor.postHandle(new ServletWebRequest(request), null);
		assertTrue(TransactionSynchronizationManager.hasResource(factory));

		verify(manager, factory);

		reset(manager, factory);

		expect(manager.isOpen()).andReturn(true);
		manager.close();

		replay(manager, factory);

		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));

		verify(manager, factory);
	}

	public void testOpenEntityManagerInViewInterceptorAsyncScenario() throws Exception {

		// Initial request thread

		OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
		interceptor.setEntityManagerFactory(factory);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		ServletWebRequest webRequest = new ServletWebRequest(request);

		replay(manager, factory);

		interceptor.preHandle(webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(factory));

		verify(manager, factory);

		AsyncWebRequest asyncWebRequest = createStrictMock(AsyncWebRequest.class);
		asyncWebRequest.addCompletionHandler((Runnable) anyObject());
		asyncWebRequest.startAsync();
		replay(asyncWebRequest);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(webRequest);
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			public String call() throws Exception {
				return "anything";
			}
		});

		verify(asyncWebRequest);

		interceptor.afterConcurrentHandlingStarted(webRequest);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));

		// Async dispatch thread

		reset(asyncWebRequest);
		expect(asyncWebRequest.isDispatched()).andReturn(true);
		replay(asyncWebRequest);

		reset(manager, factory);
		replay(manager, factory);

		interceptor.preHandle(webRequest);
		assertTrue(TransactionSynchronizationManager.hasResource(factory));

		verify(manager, factory);
		reset(manager, factory);
		replay(manager, factory);

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

		verify(manager, factory);
		reset(manager, factory);
		replay(manager, factory);

		interceptor.postHandle(webRequest, null);
		assertTrue(TransactionSynchronizationManager.hasResource(factory));

		verify(manager, factory);
		reset(manager, factory);

		expect(manager.isOpen()).andReturn(true);
		manager.close();

		replay(manager, factory);

		interceptor.afterCompletion(webRequest, null);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));

		verify(manager, factory);
	}

	public void testOpenEntityManagerInViewFilter() throws Exception {
		expect(manager.isOpen()).andReturn(true);
		manager.close();

		replay(manager, factory);

		final EntityManagerFactory factory2 = (EntityManagerFactory) createMock(EntityManagerFactory.class);
		final EntityManager manager2 = (EntityManager) createMock(EntityManager.class);

		expect(factory2.createEntityManager()).andReturn(manager2);
		expect(manager2.isOpen()).andReturn(true);
		manager2.close();

		replay(factory2, manager2);

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
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(factory));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
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

		verify(manager, factory);
		verify(factory2, manager2);

		wac.close();
	}

	public void testOpenEntityManagerInViewFilterAsyncScenario() throws Exception {
		expect(manager.isOpen()).andReturn(true);
		manager.close();

		replay(manager, factory);

		final EntityManagerFactory factory2 = (EntityManagerFactory) createMock(EntityManagerFactory.class);
		final EntityManager manager2 = (EntityManager) createMock(EntityManager.class);

		expect(factory2.createEntityManager()).andReturn(manager2);
		expect(manager2.isOpen()).andReturn(true);
		manager2.close();

		replay(factory2, manager2);

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

		final AtomicInteger count = new AtomicInteger(0);

		final FilterChain filterChain = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(factory));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
				count.incrementAndGet();
			}
		};

		final AtomicInteger count2 = new AtomicInteger(0);

		final FilterChain filterChain2 = new FilterChain() {
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
			    throws IOException, ServletException {
				assertTrue(TransactionSynchronizationManager.hasResource(factory2));
				filter.doFilter(servletRequest, servletResponse, filterChain);
				count2.incrementAndGet();
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		AsyncWebRequest asyncWebRequest = createMock(AsyncWebRequest.class);
		asyncWebRequest.addCompletionHandler((Runnable) anyObject());
		asyncWebRequest.startAsync();
		expect(asyncWebRequest.isAsyncStarted()).andReturn(true).anyTimes();
		expect(asyncWebRequest.isDispatched()).andReturn(false).anyTimes();
		replay(asyncWebRequest);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		asyncManager.startCallableProcessing(new Callable<String>() {
			public String call() throws Exception {
				return "anything";
			}
		});

		assertFalse(TransactionSynchronizationManager.hasResource(factory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		filter2.doFilter(request, response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		assertEquals(1, count.get());
		assertEquals(1, count2.get());
		assertNotNull(request.getAttribute("invoked"));

		// Async dispatch after concurrent handling produces result ...

		reset(asyncWebRequest);
		expect(asyncWebRequest.isAsyncStarted()).andReturn(false).anyTimes();
		expect(asyncWebRequest.isDispatched()).andReturn(true).anyTimes();
		replay(asyncWebRequest);

		assertFalse(TransactionSynchronizationManager.hasResource(factory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		filter.doFilter(request, response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(factory));
		assertFalse(TransactionSynchronizationManager.hasResource(factory2));
		assertEquals(2, count.get());
		assertEquals(2, count2.get());

		verify(manager, factory);
		verify(factory2, manager2);

		wac.close();
	}

}
