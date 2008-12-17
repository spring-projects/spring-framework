/*
 * Copyright 2002-2007 the original author or authors.
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

import org.junit.Ignore;


/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 15.06.2004
 */
@Ignore // dependency issues after moving from .testsuite -> .test
public class OpenPersistenceManagerInViewTests {
    
//	public void testOpenPersistenceManagerInViewInterceptor() throws Exception {
//		MockControl pmfControl = MockControl.createControl(PersistenceManagerFactory.class);
//		PersistenceManagerFactory pmf = (PersistenceManagerFactory) pmfControl.getMock();
//		MockControl pmControl = MockControl.createControl(PersistenceManager.class);
//		PersistenceManager pm = (PersistenceManager) pmControl.getMock();
//
//		OpenPersistenceManagerInViewInterceptor rawInterceptor = new OpenPersistenceManagerInViewInterceptor();
//		rawInterceptor.setPersistenceManagerFactory(pmf);
//		HandlerInterceptor interceptor = new WebRequestHandlerInterceptorAdapter(rawInterceptor);
//
//		MockServletContext sc = new MockServletContext();
//		MockHttpServletRequest request = new MockHttpServletRequest(sc);
//		MockHttpServletResponse response = new MockHttpServletResponse();
//
//		pmf.getPersistenceManager();
//		pmfControl.setReturnValue(pm, 1);
//		pmfControl.replay();
//		pmControl.replay();
//		interceptor.preHandle(request, response, "handler");
//		assertTrue(TransactionSynchronizationManager.hasResource(pmf));
//
//		// check that further invocations simply participate
//		interceptor.preHandle(request, response, "handler");
//
//		interceptor.preHandle(request, response, "handler");
//		interceptor.postHandle(request, response, "handler", null);
//		interceptor.afterCompletion(request, response, "handler", null);
//
//		interceptor.postHandle(request, response, "handler", null);
//		interceptor.afterCompletion(request, response, "handler", null);
//
//		interceptor.preHandle(request, response, "handler");
//		interceptor.postHandle(request, response, "handler", null);
//		interceptor.afterCompletion(request, response, "handler", null);
//
//		pmfControl.verify();
//		pmControl.verify();
//
//		pmfControl.reset();
//		pmControl.reset();
//		pmfControl.replay();
//		pmControl.replay();
//		interceptor.postHandle(request, response, "handler", null);
//		assertTrue(TransactionSynchronizationManager.hasResource(pmf));
//		pmfControl.verify();
//		pmControl.verify();
//
//		pmfControl.reset();
//		pmControl.reset();
//		pm.close();
//		pmControl.setVoidCallable(1);
//		pmfControl.replay();
//		pmControl.replay();
//		interceptor.afterCompletion(request, response, "handler", null);
//		assertFalse(TransactionSynchronizationManager.hasResource(pmf));
//		pmfControl.verify();
//		pmControl.verify();
//	}
//
//	public void testOpenPersistenceManagerInViewFilter() throws Exception {
//		MockControl pmfControl = MockControl.createControl(PersistenceManagerFactory.class);
//		final PersistenceManagerFactory pmf = (PersistenceManagerFactory) pmfControl.getMock();
//		MockControl pmControl = MockControl.createControl(PersistenceManager.class);
//		PersistenceManager pm = (PersistenceManager) pmControl.getMock();
//
//		pmf.getPersistenceManager();
//		pmfControl.setReturnValue(pm, 1);
//		pm.close();
//		pmControl.setVoidCallable(1);
//		pmfControl.replay();
//		pmControl.replay();
//
//		MockControl pmf2Control = MockControl.createControl(PersistenceManagerFactory.class);
//		final PersistenceManagerFactory pmf2 = (PersistenceManagerFactory) pmf2Control.getMock();
//		MockControl pm2Control = MockControl.createControl(PersistenceManager.class);
//		PersistenceManager pm2 = (PersistenceManager) pm2Control.getMock();
//
//		pmf2.getPersistenceManager();
//		pmf2Control.setReturnValue(pm2, 1);
//		pm2.close();
//		pm2Control.setVoidCallable(1);
//		pmf2Control.replay();
//		pm2Control.replay();
//
//		MockServletContext sc = new MockServletContext();
//		StaticWebApplicationContext wac = new StaticWebApplicationContext();
//		wac.setServletContext(sc);
//		wac.getDefaultListableBeanFactory().registerSingleton("persistenceManagerFactory", pmf);
//		wac.getDefaultListableBeanFactory().registerSingleton("myPersistenceManagerFactory", pmf2);
//		wac.refresh();
//		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
//		MockHttpServletRequest request = new MockHttpServletRequest(sc);
//		MockHttpServletResponse response = new MockHttpServletResponse();
//
//		MockFilterConfig filterConfig = new MockFilterConfig(wac.getServletContext(), "filter");
//		MockFilterConfig filterConfig2 = new MockFilterConfig(wac.getServletContext(), "filter2");
//		filterConfig2.addInitParameter("persistenceManagerFactoryBeanName", "myPersistenceManagerFactory");
//
//		final OpenPersistenceManagerInViewFilter filter = new OpenPersistenceManagerInViewFilter();
//		filter.init(filterConfig);
//		final OpenPersistenceManagerInViewFilter filter2 = new OpenPersistenceManagerInViewFilter();
//		filter2.init(filterConfig2);
//
//		final FilterChain filterChain = new FilterChain() {
//			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
//				assertTrue(TransactionSynchronizationManager.hasResource(pmf));
//				servletRequest.setAttribute("invoked", Boolean.TRUE);
//			}
//		};
//
//		final FilterChain filterChain2 = new FilterChain() {
//			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
//			    throws IOException, ServletException {
//				assertTrue(TransactionSynchronizationManager.hasResource(pmf2));
//				filter.doFilter(servletRequest, servletResponse, filterChain);
//			}
//		};
//
//		FilterChain filterChain3 = new PassThroughFilterChain(filter2, filterChain2);
//
//		assertFalse(TransactionSynchronizationManager.hasResource(pmf));
//		assertFalse(TransactionSynchronizationManager.hasResource(pmf2));
//		filter2.doFilter(request, response, filterChain3);
//		assertFalse(TransactionSynchronizationManager.hasResource(pmf));
//		assertFalse(TransactionSynchronizationManager.hasResource(pmf2));
//		assertNotNull(request.getAttribute("invoked"));
//
//		pmfControl.verify();
//		pmControl.verify();
//		pmf2Control.verify();
//		pm2Control.verify();
//
//		wac.close();
//	}

}
