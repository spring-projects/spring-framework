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

package org.springframework.web.servlet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.TestBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletConfig;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletConfigAwareBean;
import org.springframework.web.context.ServletContextAwareBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.BaseCommandController;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.theme.AbstractThemeResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.util.WebUtils;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 */
public class DispatcherServletTests extends TestCase {

	private static final String URL_KNOWN_ONLY_PARENT = "/knownOnlyToParent.do";

	private MockServletConfig servletConfig;

	private DispatcherServlet simpleDispatcherServlet;

	private DispatcherServlet complexDispatcherServlet;

	@Override
	protected void setUp() throws ServletException {
		servletConfig = new MockServletConfig(new MockServletContext(), "simple");
		MockServletConfig complexConfig = new MockServletConfig(servletConfig.getServletContext(), "complex");
		complexConfig.addInitParameter("publishContext", "false");
		complexConfig.addInitParameter("class", "notWritable");
		complexConfig.addInitParameter("unknownParam", "someValue");

		simpleDispatcherServlet = new DispatcherServlet();
		simpleDispatcherServlet.setContextClass(SimpleWebApplicationContext.class);
		simpleDispatcherServlet.init(servletConfig);

		complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.addRequiredProperty("publishContext");
		complexDispatcherServlet.init(complexConfig);
	}

	private ServletContext getServletContext() {
		return servletConfig.getServletContext();
	}

	public void testDispatcherServletGetServletNameDoesNotFailWithoutConfig() {
		DispatcherServlet ds = new DispatcherServlet();
		assertNull(ds.getServletConfig());
		assertNull(ds.getServletName());
		assertNull(ds.getServletContext());
	}

	public void testConfiguredDispatcherServlets() {
		assertTrue("Correct namespace",
				("simple" + FrameworkServlet.DEFAULT_NAMESPACE_SUFFIX).equals(simpleDispatcherServlet.getNamespace()));
		assertTrue("Correct attribute", (FrameworkServlet.SERVLET_CONTEXT_PREFIX + "simple").equals(
				simpleDispatcherServlet.getServletContextAttributeName()));
		assertTrue("Context published", simpleDispatcherServlet.getWebApplicationContext() ==
				getServletContext().getAttribute(FrameworkServlet.SERVLET_CONTEXT_PREFIX + "simple"));

		assertTrue("Correct namespace", "test".equals(complexDispatcherServlet.getNamespace()));
		assertTrue("Correct attribute", (FrameworkServlet.SERVLET_CONTEXT_PREFIX + "complex").equals(
				complexDispatcherServlet.getServletContextAttributeName()));
		assertTrue("Context not published",
				getServletContext().getAttribute(FrameworkServlet.SERVLET_CONTEXT_PREFIX + "complex") == null);

		simpleDispatcherServlet.destroy();
		complexDispatcherServlet.destroy();
	}

	public void testInvalidRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/invalid.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		simpleDispatcherServlet.service(request, response);
		assertTrue("Not forwarded", response.getForwardedUrl() == null);
		assertTrue("correct error code", response.getStatus() == HttpServletResponse.SC_NOT_FOUND);
	}

	public void testRequestHandledEvent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		ComplexWebApplicationContext.TestApplicationListener listener =
				(ComplexWebApplicationContext.TestApplicationListener) complexDispatcherServlet
						.getWebApplicationContext().getBean("testListener");
		assertEquals(1, listener.counter);
	}

	public void testPublishEventsOff() throws Exception {
		complexDispatcherServlet.setPublishEvents(false);
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		ComplexWebApplicationContext.TestApplicationListener listener =
				(ComplexWebApplicationContext.TestApplicationListener) complexDispatcherServlet
						.getWebApplicationContext().getBean("testListener");
		assertEquals(0, listener.counter);
	}

	public void testFormRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/form.do");
		request.addPreferredLocale(Locale.CANADA);
		MockHttpServletResponse response = new MockHttpServletResponse();

		simpleDispatcherServlet.service(request, response);
		assertTrue("forwarded to form", "form".equals(response.getForwardedUrl()));
		DefaultMessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(new String[]{"test"});
		RequestContext rc = new RequestContext(request);

		assertTrue("hasn't RequestContext attribute", request.getAttribute("rc") == null);
		assertTrue("Correct WebApplicationContext",
				RequestContextUtils.getWebApplicationContext(request) instanceof SimpleWebApplicationContext);
		assertTrue("Correct context path", rc.getContextPath().equals(request.getContextPath()));
		assertTrue("Correct locale", Locale.CANADA.equals(RequestContextUtils.getLocale(request)));
		assertTrue("Correct theme", AbstractThemeResolver.ORIGINAL_DEFAULT_THEME_NAME.equals(
				RequestContextUtils.getTheme(request).getName()));
		assertTrue("Correct message", "Canadian & test message".equals(rc.getMessage("test")));

		assertTrue("Correct WebApplicationContext",
				rc.getWebApplicationContext() == simpleDispatcherServlet.getWebApplicationContext());
		assertTrue("Correct Errors",
				!(rc.getErrors(BaseCommandController.DEFAULT_COMMAND_NAME) instanceof EscapedErrors));
		assertTrue("Correct Errors",
				!(rc.getErrors(BaseCommandController.DEFAULT_COMMAND_NAME, false) instanceof EscapedErrors));
		assertTrue("Correct Errors",
				rc.getErrors(BaseCommandController.DEFAULT_COMMAND_NAME, true) instanceof EscapedErrors);
		assertEquals("Correct message", "Canadian & test message", rc.getMessage("test"));
		assertEquals("Correct message", "Canadian & test message", rc.getMessage("test", null, false));
		assertEquals("Correct message", "Canadian &amp; test message", rc.getMessage("test", null, true));
		assertEquals("Correct message", "Canadian & test message", rc.getMessage(resolvable));
		assertEquals("Correct message", "Canadian & test message", rc.getMessage(resolvable, false));
		assertEquals("Correct message", "Canadian &amp; test message", rc.getMessage(resolvable, true));
		assertEquals("Correct message", "Canadian & test message", rc.getMessage("test", "default"));
		assertEquals("Correct message", "default", rc.getMessage("testa", "default"));
		assertEquals("Correct message", "default &amp;", rc.getMessage("testa", null, "default &", true));
	}

	public void testParameterizableViewController() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/view.do");
		request.addUserRole("role1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertTrue("forwarded to form", "myform.jsp".equals(response.getForwardedUrl()));
	}

	public void testHandlerInterceptorSuppressesView() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/view.do");
		request.addUserRole("role1");
		request.addParameter("noView", "true");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertTrue("Not forwarded", response.getForwardedUrl() == null);
	}

	public void testLocaleRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		MockHttpServletResponse response = new MockHttpServletResponse();
		simpleDispatcherServlet.service(request, response);
		assertTrue("Not forwarded", response.getForwardedUrl() == null);
		assertEquals("98", response.getHeader("Last-Modified"));
	}

	public void testUnknownRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/unknown.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals("forwarded to failed", "failed0.jsp", response.getForwardedUrl());
		assertTrue("Exception exposed", request.getAttribute("exception").getClass().equals(ServletException.class));
	}

	public void testAnotherFormRequest() throws Exception {
		MockHttpServletRequest request =
				new MockHttpServletRequest(getServletContext(), "GET", "/form.do;jsessionid=xxx");
		request.addPreferredLocale(Locale.CANADA);
		MockHttpServletResponse response = new MockHttpServletResponse();

		complexDispatcherServlet.service(request, response);
		assertTrue("forwarded to form", "myform.jsp".equals(response.getForwardedUrl()));
		assertTrue("has RequestContext attribute", request.getAttribute("rc") != null);
		DefaultMessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(new String[]{"test"});

		RequestContext rc = (RequestContext) request.getAttribute("rc");
		assertTrue("Not in HTML escaping mode", !rc.isDefaultHtmlEscape());
		assertTrue("Correct WebApplicationContext",
				rc.getWebApplicationContext() == complexDispatcherServlet.getWebApplicationContext());
		assertTrue("Correct context path", rc.getContextPath().equals(request.getContextPath()));
		assertTrue("Correct locale", Locale.CANADA.equals(rc.getLocale()));
		assertTrue("Correct Errors",
				!(rc.getErrors(BaseCommandController.DEFAULT_COMMAND_NAME) instanceof EscapedErrors));
		assertTrue("Correct Errors",
				!(rc.getErrors(BaseCommandController.DEFAULT_COMMAND_NAME, false) instanceof EscapedErrors));
		assertTrue("Correct Errors",
				rc.getErrors(BaseCommandController.DEFAULT_COMMAND_NAME, true) instanceof EscapedErrors);
		assertEquals("Correct message", "Canadian & test message", rc.getMessage("test"));
		assertEquals("Correct message", "Canadian & test message", rc.getMessage("test", null, false));
		assertEquals("Correct message", "Canadian &amp; test message", rc.getMessage("test", null, true));
		assertEquals("Correct message", "Canadian & test message", rc.getMessage(resolvable));
		assertEquals("Correct message", "Canadian & test message", rc.getMessage(resolvable, false));
		assertEquals("Correct message", "Canadian &amp; test message", rc.getMessage(resolvable, true));

		rc.setDefaultHtmlEscape(true);
		assertTrue("Is in HTML escaping mode", rc.isDefaultHtmlEscape());
		assertTrue("Correct Errors", rc.getErrors(BaseCommandController.DEFAULT_COMMAND_NAME) instanceof EscapedErrors);
		assertTrue("Correct Errors",
				!(rc.getErrors(BaseCommandController.DEFAULT_COMMAND_NAME, false) instanceof EscapedErrors));
		assertTrue("Correct Errors",
				rc.getErrors(BaseCommandController.DEFAULT_COMMAND_NAME, true) instanceof EscapedErrors);
		assertEquals("Correct message", "Canadian &amp; test message", rc.getMessage("test"));
		assertEquals("Correct message", "Canadian & test message", rc.getMessage("test", null, false));
		assertEquals("Correct message", "Canadian &amp; test message", rc.getMessage("test", null, true));
		assertEquals("Correct message", "Canadian &amp; test message", rc.getMessage(resolvable));
		assertEquals("Correct message", "Canadian & test message", rc.getMessage(resolvable, false));
		assertEquals("Correct message", "Canadian &amp; test message", rc.getMessage(resolvable, true));
	}

	public void testAnotherLocaleRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do;abc=def");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);

		assertTrue("Not forwarded", response.getForwardedUrl() == null);
		assertTrue(request.getAttribute("test1") != null);
		assertTrue(request.getAttribute("test1x") == null);
		assertTrue(request.getAttribute("test1y") == null);
		assertTrue(request.getAttribute("test2") != null);
		assertTrue(request.getAttribute("test2x") == null);
		assertTrue(request.getAttribute("test2y") == null);
		assertTrue(request.getAttribute("test3") != null);
		assertTrue(request.getAttribute("test3x") != null);
		assertTrue(request.getAttribute("test3y") != null);
		assertEquals("99", response.getHeader("Last-Modified"));
	}

	public void testExistingMultipartRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do;abc=def");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ComplexWebApplicationContext.MockMultipartResolver multipartResolver =
				(ComplexWebApplicationContext.MockMultipartResolver) complexDispatcherServlet.getWebApplicationContext()
						.getBean("multipartResolver");
		MultipartHttpServletRequest multipartRequest = multipartResolver.resolveMultipart(request);
		complexDispatcherServlet.service(multipartRequest, response);
		multipartResolver.cleanupMultipart(multipartRequest);
		assertNotNull(request.getAttribute("cleanedUp"));
	}

	public void testMultipartResolutionFailed() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do;abc=def");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.setAttribute("fail", Boolean.TRUE);
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertTrue("forwarded to failed", "failed0.jsp".equals(response.getForwardedUrl()));
		assertEquals(200, response.getStatus());
		assertTrue("correct exception", request.getAttribute(
				SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE) instanceof MaxUploadSizeExceededException);
	}

	public void testHandlerInterceptorAbort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addParameter("abort", "true");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertTrue("Not forwarded", response.getForwardedUrl() == null);
		assertTrue(request.getAttribute("test1") != null);
		assertTrue(request.getAttribute("test1x") != null);
		assertTrue(request.getAttribute("test1y") == null);
		assertTrue(request.getAttribute("test2") == null);
		assertTrue(request.getAttribute("test2x") == null);
		assertTrue(request.getAttribute("test2y") == null);
	}

	public void testModelAndViewDefiningException() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("fail", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		try {
			complexDispatcherServlet.service(request, response);
			assertEquals(200, response.getStatus());
			assertTrue("forwarded to failed", "failed1.jsp".equals(response.getForwardedUrl()));
		}
		catch (ServletException ex) {
			fail("Should not have thrown ServletException: " + ex.getMessage());
		}
	}

	public void testSimpleMappingExceptionResolverWithSpecificHandler1() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("access", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals(200, response.getStatus());
		assertEquals("forwarded to failed", "failed2.jsp", response.getForwardedUrl());
		assertTrue("Exception exposed", request.getAttribute("exception") instanceof IllegalAccessException);
	}

	public void testSimpleMappingExceptionResolverWithSpecificHandler2() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("servlet", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals(200, response.getStatus());
		assertEquals("forwarded to failed", "failed3.jsp", response.getForwardedUrl());
		assertTrue("Exception exposed", request.getAttribute("exception") instanceof ServletException);
	}

	public void testSimpleMappingExceptionResolverWithAllHandlers1() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/loc.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("access", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals(500, response.getStatus());
		assertEquals("forwarded to failed", "failed1.jsp", response.getForwardedUrl());
		assertTrue("Exception exposed", request.getAttribute("exception") instanceof IllegalAccessException);
	}

	public void testSimpleMappingExceptionResolverWithAllHandlers2() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/loc.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("servlet", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals(500, response.getStatus());
		assertEquals("forwarded to failed", "failed1.jsp", response.getForwardedUrl());
		assertTrue("Exception exposed", request.getAttribute("exception") instanceof ServletException);
	}

	public void testSimpleMappingExceptionResolverWithDefaultErrorView() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("exception", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals(200, response.getStatus());
		assertEquals("forwarded to failed", "failed0.jsp", response.getForwardedUrl());
		assertTrue("Exception exposed", request.getAttribute("exception").getClass().equals(RuntimeException.class));
	}

	public void testLocaleChangeInterceptor1() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.GERMAN);
		request.addUserRole("role2");
		request.addParameter("locale", "en");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals(200, response.getStatus());
		assertEquals("forwarded to failed", "failed0.jsp", response.getForwardedUrl());
		assertTrue("Exception exposed", request.getAttribute("exception").getClass().equals(ServletException.class));
	}

	public void testLocaleChangeInterceptor2() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.GERMAN);
		request.addUserRole("role2");
		request.addParameter("locale", "en");
		request.addParameter("locale2", "en_CA");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertTrue("Not forwarded", response.getForwardedUrl() == null);
	}

	public void testThemeChangeInterceptor1() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("theme", "mytheme");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals(200, response.getStatus());
		assertEquals("forwarded to failed", "failed0.jsp", response.getForwardedUrl());
		assertTrue("Exception exposed", request.getAttribute("exception").getClass().equals(ServletException.class));
	}

	public void testThemeChangeInterceptor2() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("theme", "mytheme");
		request.addParameter("theme2", "theme");
		MockHttpServletResponse response = new MockHttpServletResponse();
		try {
			complexDispatcherServlet.service(request, response);
			assertTrue("Not forwarded", response.getForwardedUrl() == null);
		}
		catch (ServletException ex) {
			fail("Should not have thrown ServletException: " + ex.getMessage());
		}
	}

	public void testNotAuthorized() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		MockHttpServletResponse response = new MockHttpServletResponse();
		try {
			complexDispatcherServlet.service(request, response);
			assertTrue("Correct response", response.getStatus() == HttpServletResponse.SC_FORBIDDEN);
		}
		catch (ServletException ex) {
			fail("Should not have thrown ServletException: " + ex.getMessage());
		}
	}

	public void testHeadMethodWithExplicitHandling() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "HEAD", "/head.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals(5, response.getContentLength());

		request = new MockHttpServletRequest(getServletContext(), "GET", "/head.do");
		response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals("", response.getContentAsString());
	}

	public void testHeadMethodWithNoBodyResponse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "HEAD", "/body.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals(4, response.getContentLength());

		request = new MockHttpServletRequest(getServletContext(), "GET", "/body.do");
		response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals("body", response.getContentAsString());
	}

	public void testNotDetectAllHandlerMappings() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.setDetectAllHandlerMappings(false);
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/unknown.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertTrue(response.getStatus() == HttpServletResponse.SC_NOT_FOUND);
	}

	public void testHandlerNotMappedWithAutodetect() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		// no parent
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", URL_KNOWN_ONLY_PARENT);
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
	}

	public void testDetectHandlerMappingFromParent() throws ServletException, IOException {
		// create a parent context that includes a mapping
		StaticWebApplicationContext parent = new StaticWebApplicationContext();
		parent.setServletContext(servletConfig.getServletContext());
		parent.registerSingleton("parentHandler", ControllerFromParent.class, new MutablePropertyValues());

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("mappings", URL_KNOWN_ONLY_PARENT + "=parentHandler"));

		parent.registerSingleton("parentMapping", SimpleUrlHandlerMapping.class, pvs);
		parent.refresh();

		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		// will have parent
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");

		ServletConfig config = new MockServletConfig(getServletContext(), "complex");
		config.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, parent);
		complexDispatcherServlet.init(config);

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", URL_KNOWN_ONLY_PARENT);
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);

		assertFalse("Matched through parent controller/handler pair: not response=" + response.getStatus(),
				response.getStatus() == HttpServletResponse.SC_NOT_FOUND);
	}

	public void testDetectAllHandlerAdapters() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/servlet.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals("body", response.getContentAsString());

		request = new MockHttpServletRequest(getServletContext(), "GET", "/form.do");
		response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertTrue("forwarded to form", "myform.jsp".equals(response.getForwardedUrl()));
	}

	public void testNotDetectAllHandlerAdapters() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.setDetectAllHandlerAdapters(false);
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		// only ServletHandlerAdapter with bean name "handlerAdapter" detected
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/servlet.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals("body", response.getContentAsString());

		// SimpleControllerHandlerAdapter not detected
		request = new MockHttpServletRequest(getServletContext(), "GET", "/form.do");
		response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals("forwarded to failed", "failed0.jsp", response.getForwardedUrl());
		assertTrue("Exception exposed", request.getAttribute("exception").getClass().equals(ServletException.class));
	}

	public void testNotDetectAllHandlerExceptionResolvers() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.setDetectAllHandlerExceptionResolvers(false);
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/unknown.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		try {
			complexDispatcherServlet.service(request, response);
			fail("Should have thrown ServletException");
		}
		catch (ServletException ex) {
			// expected
			assertTrue(ex.getMessage().indexOf("No adapter for handler") != -1);
		}
	}

	public void testNotDetectAllViewResolvers() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.setDetectAllViewResolvers(false);
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/unknown.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		try {
			complexDispatcherServlet.service(request, response);
			fail("Should have thrown ServletException");
		}
		catch (ServletException ex) {
			// expected
			assertTrue(ex.getMessage().indexOf("failed0") != -1);
		}
	}

	public void testCleanupAfterIncludeWithRemove() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/main.do");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setAttribute("test1", "value1");
		request.setAttribute("test2", "value2");
		WebApplicationContext wac = new StaticWebApplicationContext();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		request.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/form.do");
		simpleDispatcherServlet.service(request, response);
		assertTrue("forwarded to form", "form".equals(response.getIncludedUrl()));

		assertEquals("value1", request.getAttribute("test1"));
		assertEquals("value2", request.getAttribute("test2"));
		assertEquals(wac, request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE));
		assertNull(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE));
		assertNull(request.getAttribute("command"));
	}

	public void testCleanupAfterIncludeWithRestore() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/main.do");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setAttribute("test1", "value1");
		request.setAttribute("test2", "value2");
		WebApplicationContext wac = new StaticWebApplicationContext();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		TestBean command = new TestBean();
		request.setAttribute("command", command);

		request.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/form.do");
		simpleDispatcherServlet.service(request, response);
		assertTrue("forwarded to form", "form".equals(response.getIncludedUrl()));

		assertEquals("value1", request.getAttribute("test1"));
		assertEquals("value2", request.getAttribute("test2"));
		assertSame(wac, request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE));
		assertSame(command, request.getAttribute("command"));
	}

	public void testNoCleanupAfterInclude() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/main.do");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setAttribute("test1", "value1");
		request.setAttribute("test2", "value2");
		WebApplicationContext wac = new StaticWebApplicationContext();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		TestBean command = new TestBean();
		request.setAttribute("command", command);

		request.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/form.do");
		simpleDispatcherServlet.setCleanupAfterInclude(false);
		simpleDispatcherServlet.service(request, response);
		assertTrue("forwarded to form", "form".equals(response.getIncludedUrl()));

		assertEquals("value1", request.getAttribute("test1"));
		assertEquals("value2", request.getAttribute("test2"));
		assertSame(wac, request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE));
		assertNotSame(command, request.getAttribute("command"));
	}

	public void testServletHandlerAdapter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/servlet.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertEquals("body", response.getContentAsString());

		Servlet myServlet = (Servlet) complexDispatcherServlet.getWebApplicationContext().getBean("myServlet");
		assertEquals("complex", myServlet.getServletConfig().getServletName());
		assertEquals(getServletContext(), myServlet.getServletConfig().getServletContext());
		complexDispatcherServlet.destroy();
		assertNull(myServlet.getServletConfig());
	}

	public void testWebApplicationContextLookup() {
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/invalid.do");

		try {
			RequestContextUtils.getWebApplicationContext(request);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}

		try {
			RequestContextUtils.getWebApplicationContext(request, servletContext);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}

		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				new StaticWebApplicationContext());
		try {
			RequestContextUtils.getWebApplicationContext(request, servletContext);
		}
		catch (IllegalStateException ex) {
			fail("Should not have thrown IllegalStateException: " + ex.getMessage());
		}
	}

	public void testWithNoView() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/noview.do");
		MockHttpServletResponse response = new MockHttpServletResponse();

		complexDispatcherServlet.service(request, response);
		assertEquals("noview.jsp", response.getForwardedUrl());
	}

	public void testWithNoViewNested() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/noview/simple.do");
		MockHttpServletResponse response = new MockHttpServletResponse();

		complexDispatcherServlet.service(request, response);
		assertEquals("noview/simple.jsp", response.getForwardedUrl());
	}

	public void testWithNoViewAndSamePath() throws Exception {
		InternalResourceViewResolver vr = (InternalResourceViewResolver) complexDispatcherServlet
				.getWebApplicationContext().getBean("viewResolver2");
		vr.setSuffix("");

		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/noview");
		MockHttpServletResponse response = new MockHttpServletResponse();

		try {
			complexDispatcherServlet.service(request, response);
			fail("Should have thrown ServletException");
		}
		catch (ServletException ex) {
			ex.printStackTrace();
		}
	}

	public void testDispatcherServletRefresh() throws ServletException {
		MockServletContext servletContext = new MockServletContext("org/springframework/web/context");
		DispatcherServlet servlet = new DispatcherServlet();

		servlet.init(new MockServletConfig(servletContext, "empty"));
		ServletContextAwareBean contextBean =
				(ServletContextAwareBean) servlet.getWebApplicationContext().getBean("servletContextAwareBean");
		ServletConfigAwareBean configBean =
				(ServletConfigAwareBean) servlet.getWebApplicationContext().getBean("servletConfigAwareBean");
		assertSame(servletContext, contextBean.getServletContext());
		assertSame(servlet.getServletConfig(), configBean.getServletConfig());
		MultipartResolver multipartResolver = servlet.getMultipartResolver();
		assertNotNull(multipartResolver);

		servlet.refresh();

		ServletContextAwareBean contextBean2 =
				(ServletContextAwareBean) servlet.getWebApplicationContext().getBean("servletContextAwareBean");
		ServletConfigAwareBean configBean2 =
				(ServletConfigAwareBean) servlet.getWebApplicationContext().getBean("servletConfigAwareBean");
		assertSame(servletContext, contextBean2.getServletContext());
		assertSame(servlet.getServletConfig(), configBean2.getServletConfig());
		assertNotSame(contextBean, contextBean2);
		assertNotSame(configBean, configBean2);
		MultipartResolver multipartResolver2 = servlet.getMultipartResolver();
		assertNotSame(multipartResolver, multipartResolver2);

		servlet.destroy();
	}

	public void testDispatcherServletContextRefresh() throws ServletException {
		MockServletContext servletContext = new MockServletContext("org/springframework/web/context");
		DispatcherServlet servlet = new DispatcherServlet();

		servlet.init(new MockServletConfig(servletContext, "empty"));
		ServletContextAwareBean contextBean =
				(ServletContextAwareBean) servlet.getWebApplicationContext().getBean("servletContextAwareBean");
		ServletConfigAwareBean configBean =
				(ServletConfigAwareBean) servlet.getWebApplicationContext().getBean("servletConfigAwareBean");
		assertSame(servletContext, contextBean.getServletContext());
		assertSame(servlet.getServletConfig(), configBean.getServletConfig());
		MultipartResolver multipartResolver = servlet.getMultipartResolver();
		assertNotNull(multipartResolver);

		((ConfigurableApplicationContext) servlet.getWebApplicationContext()).refresh();

		ServletContextAwareBean contextBean2 =
				(ServletContextAwareBean) servlet.getWebApplicationContext().getBean("servletContextAwareBean");
		ServletConfigAwareBean configBean2 =
				(ServletConfigAwareBean) servlet.getWebApplicationContext().getBean("servletConfigAwareBean");
		assertSame(servletContext, contextBean2.getServletContext());
		assertSame(servlet.getServletConfig(), configBean2.getServletConfig());
		assertTrue(contextBean != contextBean2);
		assertTrue(configBean != configBean2);
		MultipartResolver multipartResolver2 = servlet.getMultipartResolver();
		assertTrue(multipartResolver != multipartResolver2);

		servlet.destroy();
	}

	public void testEnvironmentOperations() {
		DispatcherServlet servlet = new DispatcherServlet();
		ConfigurableWebEnvironment defaultEnv = servlet.getEnvironment();
		assertThat(defaultEnv, notNullValue());
		ConfigurableEnvironment env1 = new StandardServletEnvironment();
		servlet.setEnvironment(env1); // should succeed
		assertThat(servlet.getEnvironment(), sameInstance(env1));
		try {
			servlet.setEnvironment(new StandardEnvironment());
			fail("expected exception");
		}
		catch (IllegalArgumentException ex) {
		}
		class CustomServletEnvironment extends StandardServletEnvironment { }
		@SuppressWarnings("serial")
		DispatcherServlet custom = new DispatcherServlet() {
			@Override
			protected ConfigurableWebEnvironment createEnvironment() {
				return new CustomServletEnvironment();
			}
		};
		assertThat(custom.getEnvironment(), instanceOf(CustomServletEnvironment.class));
	}

	public void testAllowedOptionsIncludesPatchMethod() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "OPTIONS", "/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.service(request, response);
		assertThat(response.getHeader("Allow"), equalTo("GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, PATCH"));
	}


	public static class ControllerFromParent implements Controller {

		@Override
		public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
			return new ModelAndView(ControllerFromParent.class.getName());
		}
	}

}
