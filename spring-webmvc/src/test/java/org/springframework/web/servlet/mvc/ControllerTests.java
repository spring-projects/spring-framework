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

package org.springframework.web.servlet.mvc;

import java.util.Properties;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
class ControllerTests {

	@Test
	void parameterizableViewController() throws Exception {
		String viewName = "viewName";
		ParameterizableViewController pvc = new ParameterizableViewController();
		pvc.setViewName(viewName);
		// We don't care about the params.
		ModelAndView mv = pvc.handleRequest(new MockHttpServletRequest("GET", "foo.html"), new MockHttpServletResponse());
		assertThat(mv.getModel()).as("model has no data").isEmpty();
		assertThat(mv.getViewName()).as("model has correct viewname").isEqualTo(viewName);
		assertThat(pvc.getViewName()).as("getViewName matches").isEqualTo(viewName);
	}

	@Test
	void servletForwardingController() throws Exception {
		ServletForwardingController sfc = new ServletForwardingController();
		sfc.setServletName("action");
		doTestServletForwardingController(sfc, false);
	}

	@Test
	void servletForwardingControllerWithInclude() throws Exception {
		ServletForwardingController sfc = new ServletForwardingController();
		sfc.setServletName("action");
		doTestServletForwardingController(sfc, true);
	}

	@Test
	void servletForwardingControllerWithBeanName() throws Exception {
		ServletForwardingController sfc = new ServletForwardingController();
		sfc.setBeanName("action");
		doTestServletForwardingController(sfc, false);
	}

	private void doTestServletForwardingController(ServletForwardingController sfc, boolean include)
			throws Exception {

		HttpServletRequest request = mock();
		HttpServletResponse response = mock();
		ServletContext context = mock();
		RequestDispatcher dispatcher = mock();

		given(request.getMethod()).willReturn("GET");
		given(context.getNamedDispatcher("action")).willReturn(dispatcher);
		if (include) {
			given(request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE)).willReturn("somePath");
		}
		else {
			given(request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		}

		StaticWebApplicationContext sac = new StaticWebApplicationContext();
		sac.setServletContext(context);
		sfc.setApplicationContext(sac);
		assertThat(sfc.handleRequest(request, response)).isNull();

		if (include) {
			verify(dispatcher).include(request, response);
		}
		else {
			verify(dispatcher).forward(request, response);
		}
	}

	@Test
	void servletWrappingController() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/somePath");
		HttpServletResponse response = new MockHttpServletResponse();

		ServletWrappingController swc = new ServletWrappingController();
		swc.setServletClass(TestServlet.class);
		swc.setServletName("action");
		Properties props = new Properties();
		props.setProperty("config", "myValue");
		swc.setInitParameters(props);

		swc.afterPropertiesSet();
		assertThat(TestServlet.config).isNotNull();
		assertThat(TestServlet.config.getServletName()).isEqualTo("action");
		assertThat(TestServlet.config.getInitParameter("config")).isEqualTo("myValue");
		assertThat(TestServlet.request).isNull();
		assertThat(TestServlet.destroyed).isFalse();

		assertThat(swc.handleRequest(request, response)).isNull();
		assertThat(TestServlet.request).isEqualTo(request);
		assertThat(TestServlet.response).isEqualTo(response);
		assertThat(TestServlet.destroyed).isFalse();

		swc.destroy();
		assertThat(TestServlet.destroyed).isTrue();
	}

	@Test
	void servletWrappingControllerWithBeanName() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/somePath");
		HttpServletResponse response = new MockHttpServletResponse();

		ServletWrappingController swc = new ServletWrappingController();
		swc.setServletClass(TestServlet.class);
		swc.setBeanName("action");

		swc.afterPropertiesSet();
		assertThat(TestServlet.config).isNotNull();
		assertThat(TestServlet.config.getServletName()).isEqualTo("action");
		assertThat(TestServlet.request).isNull();
		assertThat(TestServlet.destroyed).isFalse();

		assertThat(swc.handleRequest(request, response)).isNull();
		assertThat(TestServlet.request).isEqualTo(request);
		assertThat(TestServlet.response).isEqualTo(response);
		assertThat(TestServlet.destroyed).isFalse();

		swc.destroy();
		assertThat(TestServlet.destroyed).isTrue();
	}


	public static class TestServlet implements Servlet {

		private static ServletConfig config;
		private static ServletRequest request;
		private static ServletResponse response;
		private static boolean destroyed;

		public TestServlet() {
			config = null;
			request = null;
			response = null;
			destroyed = false;
		}

		@Override
		public void init(ServletConfig servletConfig) {
			config = servletConfig;
		}

		@Override
		public ServletConfig getServletConfig() {
			return config;
		}

		@Override
		public void service(ServletRequest servletRequest, ServletResponse servletResponse) {
			request = servletRequest;
			response = servletResponse;
		}

		@Override
		public String getServletInfo() {
			return "TestServlet";
		}

		@Override
		public void destroy() {
			destroyed = true;
		}
	}

}
