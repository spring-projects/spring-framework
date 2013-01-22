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

package org.springframework.orm.jdo.support;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;
import org.springframework.mock.web.test.MockFilterConfig;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.mock.web.test.PassThroughFilterChain;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @since 15.06.2004
 */
public class OpenPersistenceManagerInViewTests {

	@Test
	public void testOpenPersistenceManagerInViewInterceptor() throws Exception {
		PersistenceManagerFactory pmf = mock(PersistenceManagerFactory.class);
		PersistenceManager pm = mock(PersistenceManager.class);

		OpenPersistenceManagerInViewInterceptor interceptor = new OpenPersistenceManagerInViewInterceptor();
		interceptor.setPersistenceManagerFactory(pmf);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);

		given(pmf.getPersistenceManager()).willReturn(pm);
		interceptor.preHandle(new ServletWebRequest(request));
		assertTrue(TransactionSynchronizationManager.hasResource(pmf));

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
		assertTrue(TransactionSynchronizationManager.hasResource(pmf));

		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertFalse(TransactionSynchronizationManager.hasResource(pmf));
	}

	@Test
	public void testOpenPersistenceManagerInViewFilter() throws Exception {
		final PersistenceManagerFactory pmf = mock(PersistenceManagerFactory.class);
		PersistenceManager pm = mock(PersistenceManager.class);

		given(pmf.getPersistenceManager()).willReturn(pm);
		final PersistenceManagerFactory pmf2 = mock(PersistenceManagerFactory.class);
		PersistenceManager pm2 = mock(PersistenceManager.class);

		given(pmf2.getPersistenceManager()).willReturn(pm2);

		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getDefaultListableBeanFactory().registerSingleton("persistenceManagerFactory", pmf);
		wac.getDefaultListableBeanFactory().registerSingleton("myPersistenceManagerFactory", pmf2);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		MockHttpServletRequest request = new MockHttpServletRequest(sc);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
		filterConfig2.addInitParameter("persistenceManagerFactoryBeanName", "myPersistenceManagerFactory");

		final OpenPersistenceManagerInViewFilter filter = new OpenPersistenceManagerInViewFilter();
		filter.init(filterConfig);
		final OpenPersistenceManagerInViewFilter filter2 = new OpenPersistenceManagerInViewFilter();
		filter2.init(filterConfig2);

		final FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
				assertTrue(TransactionSynchronizationManager.hasResource(pmf));
				servletRequest.setAttribute("invoked", Boolean.TRUE);
			}
		};

		final FilterChain filterChain2 = new FilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IOException, ServletException {
				assertTrue(TransactionSynchronizationManager.hasResource(pmf2));
				filter.doFilter(servletRequest, servletResponse, filterChain);
			}
		};

		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);

		assertFalse(TransactionSynchronizationManager.hasResource(pmf));
		assertFalse(TransactionSynchronizationManager.hasResource(pmf2));
		filter2.doFilter(request, response, filterChain3);
		assertFalse(TransactionSynchronizationManager.hasResource(pmf));
		assertFalse(TransactionSynchronizationManager.hasResource(pmf2));
		assertNotNull(request.getAttribute("invoked"));

		verify(pm).close();
		verify(pm2).close();

		wac.close();
	}

}
