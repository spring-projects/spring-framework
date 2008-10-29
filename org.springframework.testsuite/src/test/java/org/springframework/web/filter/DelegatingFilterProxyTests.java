/* Copyright 2004, 2005 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * @author Juergen Hoeller
 * @since 08.05.2005
 */
public class DelegatingFilterProxyTests extends TestCase {

	public void testDelegatingFilterProxy() throws ServletException, IOException {
		ServletContext sc = new MockServletContext();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.registerSingleton("targetFilter", MockFilter.class);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilter targetFilter = (MockFilter) wac.getBean("targetFilter");

		MockFilterConfig proxyConfig = new MockFilterConfig(sc);
		proxyConfig.addInitParameter("targetBeanName", "targetFilter");
		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy();
		filterProxy.init(proxyConfig);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertNull(targetFilter.filterConfig);
		assertEquals(Boolean.TRUE, request.getAttribute("called"));

		filterProxy.destroy();
		assertNull(targetFilter.filterConfig);
	}

	public void testDelegatingFilterProxyWithFilterName() throws ServletException, IOException {
		ServletContext sc = new MockServletContext();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.registerSingleton("targetFilter", MockFilter.class);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilter targetFilter = (MockFilter) wac.getBean("targetFilter");

		MockFilterConfig proxyConfig = new MockFilterConfig(sc, "targetFilter");
		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy();
		filterProxy.init(proxyConfig);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertNull(targetFilter.filterConfig);
		assertEquals(Boolean.TRUE, request.getAttribute("called"));

		filterProxy.destroy();
		assertNull(targetFilter.filterConfig);
	}

	public void testDelegatingFilterProxyWithLazyContextStartup() throws ServletException, IOException {
		ServletContext sc = new MockServletContext();

		MockFilterConfig proxyConfig = new MockFilterConfig(sc);
		proxyConfig.addInitParameter("targetBeanName", "targetFilter");
		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy();
		filterProxy.init(proxyConfig);

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.registerSingleton("targetFilter", MockFilter.class);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilter targetFilter = (MockFilter) wac.getBean("targetFilter");

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertNull(targetFilter.filterConfig);
		assertEquals(Boolean.TRUE, request.getAttribute("called"));

		filterProxy.destroy();
		assertNull(targetFilter.filterConfig);
	}

	public void testDelegatingFilterProxyWithTargetFilterLifecycle() throws ServletException, IOException {
		ServletContext sc = new MockServletContext();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.registerSingleton("targetFilter", MockFilter.class);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilter targetFilter = (MockFilter) wac.getBean("targetFilter");

		MockFilterConfig proxyConfig = new MockFilterConfig(sc);
		proxyConfig.addInitParameter("targetBeanName", "targetFilter");
		proxyConfig.addInitParameter("targetFilterLifecycle", "true");
		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy();
		filterProxy.init(proxyConfig);
		assertEquals(proxyConfig, targetFilter.filterConfig);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertEquals(proxyConfig, targetFilter.filterConfig);
		assertEquals(Boolean.TRUE, request.getAttribute("called"));

		filterProxy.destroy();
		assertNull(targetFilter.filterConfig);
	}


	public static class MockFilter implements Filter {

		public FilterConfig filterConfig;

		public void init(FilterConfig filterConfig) throws ServletException {
			this.filterConfig = filterConfig;
		}

		public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
			request.setAttribute("called", Boolean.TRUE);
		}

		public void destroy() {
			this.filterConfig = null;
		}
	}

}
