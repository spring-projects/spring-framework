/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockFilterConfig;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Rob Winch
 * @since 08.05.2005
 */
class DelegatingFilterProxyTests {

	@Test
	void testDelegatingFilterProxy() throws ServletException, IOException {
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

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyAndCustomContextAttribute() throws ServletException, IOException {
		ServletContext sc = new MockServletContext();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.registerSingleton("targetFilter", MockFilter.class);
		wac.refresh();
		sc.setAttribute("CUSTOM_ATTR", wac);

		MockFilter targetFilter = (MockFilter) wac.getBean("targetFilter");

		MockFilterConfig proxyConfig = new MockFilterConfig(sc);
		proxyConfig.addInitParameter("targetBeanName", "targetFilter");
		proxyConfig.addInitParameter("contextAttribute", "CUSTOM_ATTR");
		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy();
		filterProxy.init(proxyConfig);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyWithFilterDelegateInstance() throws ServletException, IOException {
		MockFilter targetFilter = new MockFilter();

		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy(targetFilter);
		filterProxy.init(new MockFilterConfig(new MockServletContext()));

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyWithTargetBeanName() throws ServletException, IOException {
		MockServletContext sc = new MockServletContext();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.registerSingleton("targetFilter", MockFilter.class);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		MockFilter targetFilter = (MockFilter) wac.getBean("targetFilter");

		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy("targetFilter");
		filterProxy.init(new MockFilterConfig(sc));

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyWithTargetBeanNameAndNotYetRefreshedApplicationContext()
			throws ServletException, IOException {

		MockServletContext sc = new MockServletContext();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.registerSingleton("targetFilter", MockFilter.class);
		// wac.refresh();
		// note that the context is not set as the ROOT attribute in the ServletContext!

		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy("targetFilter", wac);
		filterProxy.init(new MockFilterConfig(sc));

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		MockFilter targetFilter = (MockFilter) wac.getBean("targetFilter");

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyWithTargetBeanNameAndNoApplicationContext()
			throws ServletException {

		MockServletContext sc = new MockServletContext();

		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy("targetFilter", null);
		filterProxy.init(new MockFilterConfig(sc));

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertThatIllegalStateException().isThrownBy(() ->
				filterProxy.doFilter(request, response, null));
	}

	@Test
	void testDelegatingFilterProxyWithFilterName() throws ServletException, IOException {
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

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyWithLazyContextStartup() throws ServletException, IOException {
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

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyWithTargetFilterLifecycle() throws ServletException, IOException {
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
		assertThat(targetFilter.filterConfig).isEqualTo(proxyConfig);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertThat(targetFilter.filterConfig).isEqualTo(proxyConfig);
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyWithFrameworkServletContext() throws ServletException, IOException {
		ServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.registerSingleton("targetFilter", MockFilter.class);
		wac.refresh();
		sc.setAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.dispatcher", wac);

		MockFilter targetFilter = (MockFilter) wac.getBean("targetFilter");

		MockFilterConfig proxyConfig = new MockFilterConfig(sc);
		proxyConfig.addInitParameter("targetBeanName", "targetFilter");
		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy();
		filterProxy.init(proxyConfig);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyInjectedPreferred() throws ServletException, IOException {
		ServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.refresh();
		sc.setAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.dispatcher", wac);

		StaticWebApplicationContext injectedWac = new StaticWebApplicationContext();
		injectedWac.setServletContext(sc);
		String beanName = "targetFilter";
		injectedWac.registerSingleton(beanName, MockFilter.class);
		injectedWac.refresh();

		MockFilter targetFilter = (MockFilter) injectedWac.getBean(beanName);

		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy(beanName, injectedWac);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyNotInjectedWacServletAttrPreferred()
			throws ServletException, IOException {

		ServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		sc.setAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.dispatcher", wac);

		StaticWebApplicationContext wacToUse = new StaticWebApplicationContext();
		wacToUse.setServletContext(sc);
		String beanName = "targetFilter";
		String attrName = "customAttrName";
		wacToUse.registerSingleton(beanName, MockFilter.class);
		wacToUse.refresh();
		sc.setAttribute(attrName, wacToUse);

		MockFilter targetFilter = (MockFilter) wacToUse.getBean(beanName);

		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy(beanName);
		filterProxy.setContextAttribute(attrName);
		filterProxy.setServletContext(sc);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}

	@Test
	void testDelegatingFilterProxyNotInjectedWithRootPreferred() throws ServletException, IOException {
		ServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.refresh();
		sc.setAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.dispatcher", wac);
		sc.setAttribute("another", wac);

		StaticWebApplicationContext wacToUse = new StaticWebApplicationContext();
		wacToUse.setServletContext(sc);
		String beanName = "targetFilter";
		wacToUse.registerSingleton(beanName, MockFilter.class);
		wacToUse.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wacToUse);

		MockFilter targetFilter = (MockFilter) wacToUse.getBean(beanName);

		DelegatingFilterProxy filterProxy = new DelegatingFilterProxy(beanName);
		filterProxy.setServletContext(sc);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertThat(targetFilter.filterConfig).isNull();
		assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

		filterProxy.destroy();
		assertThat(targetFilter.filterConfig).isNull();
	}


	public static class MockFilter implements Filter {

		public FilterConfig filterConfig;

		@Override
		public void init(FilterConfig filterConfig) {
			this.filterConfig = filterConfig;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
				throws IOException {

			request.setAttribute("called", Boolean.TRUE);
		}

		@Override
		public void destroy() {
			this.filterConfig = null;
		}
	}

}
