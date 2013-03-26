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

package org.springframework.web.servlet.mvc;

import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import static org.mockito.BDDMockito.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class ControllerTests extends TestCase {

	public void testParameterizableViewController() throws Exception {
		String viewName = "viewName";
		ParameterizableViewController pvc = new ParameterizableViewController();
		pvc.setViewName(viewName);
		// We don't care about the params.
		ModelAndView mv = pvc.handleRequest(new MockHttpServletRequest("GET", "foo.html"), null);
		assertTrue("model has no data", mv.getModel().size() == 0);
		assertTrue("model has correct viewname", mv.getViewName().equals(viewName));
		assertTrue("getViewName matches", pvc.getViewName().equals(viewName));
	}

	public void testServletForwardingController() throws Exception {
		ServletForwardingController sfc = new ServletForwardingController();
		sfc.setServletName("action");
		doTestServletForwardingController(sfc, false);
	}

	public void testServletForwardingControllerWithInclude() throws Exception {
		ServletForwardingController sfc = new ServletForwardingController();
		sfc.setServletName("action");
		doTestServletForwardingController(sfc, true);
	}

	public void testServletForwardingControllerWithBeanName() throws Exception {
		ServletForwardingController sfc = new ServletForwardingController();
		sfc.setBeanName("action");
		doTestServletForwardingController(sfc, false);
	}

	private void doTestServletForwardingController(ServletForwardingController sfc, boolean include)
			throws Exception {

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		ServletContext context = mock(ServletContext.class);
		RequestDispatcher dispatcher = mock(RequestDispatcher.class);

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
		assertNull(sfc.handleRequest(request, response));

		if (include) {
			verify(dispatcher).include(request, response);
		}
		else {
			verify(dispatcher).forward(request, response);
		}
	}

	public void testServletWrappingController() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/somePath");
		HttpServletResponse response = new MockHttpServletResponse();

		ServletWrappingController swc = new ServletWrappingController();
		swc.setServletClass(TestServlet.class);
		swc.setServletName("action");
		Properties props = new Properties();
		props.setProperty("config", "myValue");
		swc.setInitParameters(props);

		swc.afterPropertiesSet();
		assertNotNull(TestServlet.config);
		assertEquals("action", TestServlet.config.getServletName());
		assertEquals("myValue", TestServlet.config.getInitParameter("config"));
		assertNull(TestServlet.request);
		assertFalse(TestServlet.destroyed);

		assertNull(swc.handleRequest(request, response));
		assertEquals(request, TestServlet.request);
		assertEquals(response, TestServlet.response);
		assertFalse(TestServlet.destroyed);

		swc.destroy();
		assertTrue(TestServlet.destroyed);
	}

	public void testServletWrappingControllerWithBeanName() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/somePath");
		HttpServletResponse response = new MockHttpServletResponse();

		ServletWrappingController swc = new ServletWrappingController();
		swc.setServletClass(TestServlet.class);
		swc.setBeanName("action");

		swc.afterPropertiesSet();
		assertNotNull(TestServlet.config);
		assertEquals("action", TestServlet.config.getServletName());
		assertNull(TestServlet.request);
		assertFalse(TestServlet.destroyed);

		assertNull(swc.handleRequest(request, response));
		assertEquals(request, TestServlet.request);
		assertEquals(response, TestServlet.response);
		assertFalse(TestServlet.destroyed);

		swc.destroy();
		assertTrue(TestServlet.destroyed);
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
