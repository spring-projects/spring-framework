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

package org.springframework.orm.jdo.support;

import java.io.IOException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;
import org.easymock.MockControl;

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
 * @since 15.06.2004
 */
public class OpenPersistenceManagerInViewTests extends TestCase {

	public void testOpenPersistenceManagerInViewInterceptor() throws Exception {
		MockControl pmfControl = MockControl.createControl(PersistenceManagerFactory.class);
		PersistenceManagerFactory pmf = (PersistenceManagerFactory) pmfControl.getMock();
		MockControl pmControl = MockControl.createControl(PersistenceManager.class);
		PersistenceManager pm = (PersistenceManager) pmControl.getMock();

		OpenPersistenceManagerInViewInterceptor interceptor = new OpenPersistenceManagerInViewInterceptor();
		interceptor.setPersistenceManagerFactory(pmf);

		MockServletContext sc = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(sc);

		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pmfControl.replay();
		pmControl.replay();
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

		pmfControl.verify();
		pmControl.verify();

		pmfControl.reset();
		pmControl.reset();
		pmfControl.replay();
		pmControl.replay();
		interceptor.postHandle(new ServletWebRequest(request), null);
		assertTrue(TransactionSynchronizationManager.hasResource(pmf));
		pmfControl.verify();
		pmControl.verify();

		pmfControl.reset();
		pmControl.reset();
		pm.close();
		pmControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();
		interceptor.afterCompletion(new ServletWebRequest(request), null);
		assertFalse(TransactionSynchronizationManager.hasResource(pmf));
		pmfControl.verify();
		pmControl.verify();
	}

	public void testOpenPersistenceManagerInViewFilter() throws Exception {
		MockControl pmfControl = MockControl.createControl(PersistenceManagerFactory.class);
		final PersistenceManagerFactory pmf = (PersistenceManagerFactory) pmfControl.getMock();
		MockControl pmControl = MockControl.createControl(PersistenceManager.class);
		PersistenceManager pm = (PersistenceManager) pmControl.getMock();

		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.close();
		pmControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();

		MockControl pmf2Control = MockControl.createControl(PersistenceManagerFactory.class);
		final PersistenceManagerFactory pmf2 = (PersistenceManagerFactory) pmf2Control.getMock();
		MockControl pm2Control = MockControl.createControl(PersistenceManager.class);
		PersistenceManager pm2 = (PersistenceManager) pm2Control.getMock();

		pmf2.getPersistenceManager();
		pmf2Control.setReturnValue(pm2, 1);
		pm2.close();
		pm2Control.setVoidCallable(1);
		pmf2Control.replay();
		pm2Control.replay();

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

		pmfControl.verify();
		pmControl.verify();
		pmf2Control.verify();
		pm2Control.verify();

		wac.close();
	}

}
