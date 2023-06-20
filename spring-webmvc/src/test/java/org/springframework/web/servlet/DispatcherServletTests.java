/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ServletConfigAwareBean;
import org.springframework.web.context.ServletContextAwareBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.WebUtils;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 */
public class DispatcherServletTests {

	private static final String URL_KNOWN_ONLY_PARENT = "/knownOnlyToParent.do";

	private final MockServletConfig servletConfig = new MockServletConfig(new MockServletContext(), "simple");

	private DispatcherServlet simpleDispatcherServlet;

	private DispatcherServlet complexDispatcherServlet;


	@BeforeEach
	public void setup() throws ServletException {
		MockServletConfig complexConfig = new MockServletConfig(getServletContext(), "complex");
		complexConfig.addInitParameter("publishContext", "false");
		complexConfig.addInitParameter("class", "notWritable");
		complexConfig.addInitParameter("unknownParam", "someValue");
		complexConfig.addInitParameter("jakarta.servlet.http.legacyDoHead", "true");

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


	@Test
	public void configuredDispatcherServlets() {
		assertThat((simpleDispatcherServlet.getNamespace())).as("Correct namespace")
				.isEqualTo("simple" + FrameworkServlet.DEFAULT_NAMESPACE_SUFFIX);
		assertThat((FrameworkServlet.SERVLET_CONTEXT_PREFIX + "simple")).as("Correct attribute")
				.isEqualTo(simpleDispatcherServlet.getServletContextAttributeName());
		assertThat(simpleDispatcherServlet.getWebApplicationContext()).as("Context published")
				.isSameAs(getServletContext().getAttribute(FrameworkServlet.SERVLET_CONTEXT_PREFIX + "simple"));

		assertThat(complexDispatcherServlet.getNamespace()).as("Correct namespace").isEqualTo("test");
		assertThat((FrameworkServlet.SERVLET_CONTEXT_PREFIX + "complex")).as("Correct attribute")
				.isEqualTo(complexDispatcherServlet.getServletContextAttributeName());
		assertThat(getServletContext().getAttribute(FrameworkServlet.SERVLET_CONTEXT_PREFIX + "complex")).as("Context not published")
				.isNull();

		simpleDispatcherServlet.destroy();
		complexDispatcherServlet.destroy();
	}

	@Test
	public void invalidRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/invalid.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		simpleDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("Not forwarded").isNull();
		assertThat(response.getStatus()).as("correct error code").isEqualTo(HttpServletResponse.SC_NOT_FOUND);
	}

	@Test
	public void requestHandledEvent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		ComplexWebApplicationContext.TestApplicationListener listener =
				(ComplexWebApplicationContext.TestApplicationListener) complexDispatcherServlet
						.getWebApplicationContext().getBean("testListener");
		assertThat(listener.counter).isOne();
	}

	@Test
	public void publishEventsOff() throws Exception {
		complexDispatcherServlet.setPublishEvents(false);
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		ComplexWebApplicationContext.TestApplicationListener listener =
				(ComplexWebApplicationContext.TestApplicationListener) complexDispatcherServlet
						.getWebApplicationContext().getBean("testListener");
		assertThat(listener.counter).isEqualTo(0);
	}

	@Test
	public void parameterizableViewController() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/view.do");
		request.addUserRole("role1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("myform.jsp");
	}

	@Test
	public void handlerInterceptorSuppressesView() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/view.do");
		request.addUserRole("role1");
		request.addParameter("noView", "true");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("Not forwarded").isNull();
	}

	@Test
	public void localeRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		MockHttpServletResponse response = new MockHttpServletResponse();
		simpleDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("Not forwarded").isNull();
		assertThat(response.getHeader("Last-Modified")).isEqualTo("Wed, 01 Apr 2015 00:00:00 GMT");
	}

	@Test
	public void unknownRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/unknown.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed0.jsp");
		assertThat(request.getAttribute("exception").getClass().equals(ServletException.class)).as("Exception exposed").isTrue();
	}

	@Test
	public void anotherLocaleRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do;abc=def");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);

		assertThat(response.getForwardedUrl()).as("Not forwarded").isNull();
		assertThat(request.getAttribute("test1")).isNotNull();
		assertThat(request.getAttribute("test1x")).isNull();
		assertThat(request.getAttribute("test1y")).isNull();
		assertThat(request.getAttribute("test2")).isNotNull();
		assertThat(request.getAttribute("test2x")).isNull();
		assertThat(request.getAttribute("test2y")).isNull();
		assertThat(request.getAttribute("test3")).isNotNull();
		assertThat(request.getAttribute("test3x")).isNotNull();
		assertThat(request.getAttribute("test3y")).isNotNull();
		assertThat(response.getHeader("Last-Modified")).isEqualTo("Wed, 01 Apr 2015 00:00:01 GMT");
	}

	@Test
	public void existingMultipartRequest() throws Exception {
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
		assertThat(request.getAttribute(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE)).isNull();
		assertThat(request.getAttribute("cleanedUp")).isNotNull();
	}

	@Test
	public void existingMultipartRequestButWrapped() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do;abc=def");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ComplexWebApplicationContext.MockMultipartResolver multipartResolver =
				(ComplexWebApplicationContext.MockMultipartResolver) complexDispatcherServlet.getWebApplicationContext()
						.getBean("multipartResolver");
		MultipartHttpServletRequest multipartRequest = multipartResolver.resolveMultipart(request);
		complexDispatcherServlet.service(new HttpServletRequestWrapper(multipartRequest), response);
		multipartResolver.cleanupMultipart(multipartRequest);
		assertThat(request.getAttribute(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE)).isNull();
		assertThat(request.getAttribute("cleanedUp")).isNotNull();
	}

	@Test
	public void multipartResolutionFailed() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do;abc=def");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.setAttribute("fail", Boolean.TRUE);
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed0.jsp");
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(request.getAttribute(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE))
			.isInstanceOf(MaxUploadSizeExceededException.class);
	}

	@Test
	public void handlerInterceptorAbort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addParameter("abort", "true");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("Not forwarded").isNull();
		assertThat(request.getAttribute("test1")).isNotNull();
		assertThat(request.getAttribute("test1x")).isNotNull();
		assertThat(request.getAttribute("test1y")).isNull();
		assertThat(request.getAttribute("test2")).isNull();
		assertThat(request.getAttribute("test2x")).isNull();
		assertThat(request.getAttribute("test2y")).isNull();
	}

	@Test
	public void modelAndViewDefiningException() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("fail", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed1.jsp");
	}

	@Test
	public void simpleMappingExceptionResolverWithSpecificHandler1() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("access", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed2.jsp");
		assertThat(request.getAttribute("exception")).as("Exception exposed")
				.isInstanceOf(IllegalAccessException.class);
	}

	@Test
	public void simpleMappingExceptionResolverWithSpecificHandler2() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("servlet", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed3.jsp");
		assertThat(request.getAttribute("exception")).as("Exception exposed").isInstanceOf(ServletException.class);
	}

	@Test
	public void simpleMappingExceptionResolverWithAllHandlers1() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/loc.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("access", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).isEqualTo(500);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed1.jsp");
		assertThat(request.getAttribute("exception")).as("Exception exposed")
				.isInstanceOf(IllegalAccessException.class);
	}

	@Test
	public void simpleMappingExceptionResolverWithAllHandlers2() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/loc.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("servlet", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).isEqualTo(500);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed1.jsp");
		assertThat(request.getAttribute("exception")).as("Exception exposed").isInstanceOf(ServletException.class);
	}

	@Test
	public void simpleMappingExceptionResolverWithDefaultErrorView() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("exception", "yes");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed0.jsp");
		assertThat(request.getAttribute("exception").getClass().equals(RuntimeException.class)).as("Exception exposed").isTrue();
	}

	@Test
	public void localeChangeInterceptor1() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.GERMAN);
		request.addUserRole("role2");
		request.addParameter("locale", "en");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed0.jsp");
		assertThat(request.getAttribute("exception").getClass().equals(ServletException.class)).as("Exception exposed").isTrue();
	}

	@Test
	public void localeChangeInterceptor2() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.GERMAN);
		request.addUserRole("role2");
		request.addParameter("locale", "en");
		request.addParameter("locale2", "en_CA");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("Not forwarded").isNull();
	}

	@Test
	public void themeChangeInterceptor1() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("theme", "mytheme");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed0.jsp");
		assertThat(request.getAttribute("exception").getClass().equals(ServletException.class)).as("Exception exposed").isTrue();
	}

	@Test
	public void themeChangeInterceptor2() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		request.addUserRole("role1");
		request.addParameter("theme", "mytheme");
		request.addParameter("theme2", "theme");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("Not forwarded").isNull();
	}

	@Test
	public void notAuthorized() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/locale.do");
		request.addPreferredLocale(Locale.CANADA);
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).as("Correct response").isEqualTo(HttpServletResponse.SC_FORBIDDEN);
	}

	@Test
	public void headMethodWithExplicitHandling() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "HEAD", "/head.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getContentLength()).isEqualTo(5);

		request = new MockHttpServletRequest(getServletContext(), "GET", "/head.do");
		response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getContentAsString()).isEmpty();
	}

	@Test
	public void headMethodWithNoBodyResponse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "HEAD", "/body.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getContentLength()).isEqualTo(4);

		request = new MockHttpServletRequest(getServletContext(), "GET", "/body.do");
		response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("body");
	}

	@Test
	public void notDetectAllHandlerMappings() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.setDetectAllHandlerMappings(false);
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/unknown.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
	}

	@Test
	public void handlerNotMappedWithAutodetect() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		// no parent
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", URL_KNOWN_ONLY_PARENT);
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
	}

	@Test
	public void detectHandlerMappingFromParent() throws ServletException, IOException {
		// create a parent context that includes a mapping
		StaticWebApplicationContext parent = new StaticWebApplicationContext();
		parent.setServletContext(getServletContext());
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

		assertThat(response.getStatus()).as("Matched through parent controller/handler pair: not response=" + response.getStatus())
				.isNotEqualTo(HttpServletResponse.SC_NOT_FOUND);
	}

	@Test
	public void detectAllHandlerAdapters() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/servlet.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("body");

		request = new MockHttpServletRequest(getServletContext(), "GET", "/form.do");
		response = new MockHttpServletResponse();
		request.addParameter("fail", "yes");
		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed1.jsp");
		assertThat(request.getAttribute("exception")).isNull();
	}

	@Test
	public void notDetectAllHandlerAdapters() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.setDetectAllHandlerAdapters(false);
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		// only ServletHandlerAdapter with bean name "handlerAdapter" detected
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/servlet.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("body");

		// MyHandlerAdapter not detected
		request = new MockHttpServletRequest(getServletContext(), "GET", "/form.do");
		response = new MockHttpServletResponse();
		request.addParameter("fail", "yes");
		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).as("forwarded URL").isEqualTo("failed0.jsp");
		assertThat(request.getAttribute("exception"))
			.asInstanceOf(InstanceOfAssertFactories.type(ServletException.class))
			.extracting(Throwable::getMessage).asString().startsWith("No adapter for handler");
	}

	@Test
	public void notDetectAllHandlerExceptionResolvers() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.setDetectAllHandlerExceptionResolvers(false);
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/unknown.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertThatExceptionOfType(ServletException.class).isThrownBy(() ->
				complexDispatcherServlet.service(request, response))
			.withMessageContaining("No adapter for handler");
	}

	@Test
	public void notDetectAllViewResolvers() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(ComplexWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.setDetectAllViewResolvers(false);
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/unknown.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertThatExceptionOfType(ServletException.class).isThrownBy(() ->
				complexDispatcherServlet.service(request, response))
			.withMessageContaining("failed0");
	}

	@Test
	public void throwExceptionIfNoHandlerFound() throws ServletException, IOException {
		DispatcherServlet complexDispatcherServlet = new DispatcherServlet();
		complexDispatcherServlet.setContextClass(SimpleWebApplicationContext.class);
		complexDispatcherServlet.setNamespace("test");
		complexDispatcherServlet.init(new MockServletConfig(getServletContext(), "complex"));

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/unknown");
		MockHttpServletResponse response = new MockHttpServletResponse();

		complexDispatcherServlet.service(request, response);
		assertThat(response.getStatus()).as("correct error code").isEqualTo(HttpServletResponse.SC_NOT_FOUND);
	}

	// SPR-12984

	@Test
	public void noHandlerFoundExceptionMessage() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/foo", headers);
		assertThat(ex.getMessage()).doesNotContain("bar");
		assertThat(ex.toString()).doesNotContain("bar");
	}

	@Test
	public void cleanupAfterIncludeWithRemove() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/main.do");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setAttribute("test1", "value1");
		request.setAttribute("test2", "value2");
		WebApplicationContext wac = new StaticWebApplicationContext();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		request.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "/form.do");
		simpleDispatcherServlet.service(request, response);

		assertThat(request.getAttribute("test1")).isEqualTo("value1");
		assertThat(request.getAttribute("test2")).isEqualTo("value2");
		assertThat(request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isEqualTo(wac);
		assertThat(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isNull();
		assertThat(request.getAttribute("command")).isNull();
	}

	@Test
	public void cleanupAfterIncludeWithRestore() throws ServletException, IOException {
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

		assertThat(request.getAttribute("test1")).isEqualTo("value1");
		assertThat(request.getAttribute("test2")).isEqualTo("value2");
		assertThat(request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(wac);
	}

	@Test
	public void noCleanupAfterInclude() throws ServletException, IOException {
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

		assertThat(request.getAttribute("test1")).isEqualTo("value1");
		assertThat(request.getAttribute("test2")).isEqualTo("value2");
		assertThat(request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(wac);
	}

	@Test
	public void servletHandlerAdapter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/servlet.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("body");

		Servlet myServlet = (Servlet) complexDispatcherServlet.getWebApplicationContext().getBean("myServlet");
		assertThat(myServlet.getServletConfig().getServletName()).isEqualTo("complex");
		assertThat(myServlet.getServletConfig().getServletContext()).isEqualTo(getServletContext());
		complexDispatcherServlet.destroy();
		assertThat((Object) myServlet.getServletConfig()).isNull();
	}

	@Test
	public void withNoView() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/noview.do");
		MockHttpServletResponse response = new MockHttpServletResponse();

		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).isEqualTo("noview.jsp");
	}

	@Test
	public void withNoViewNested() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/noview/simple.do");
		MockHttpServletResponse response = new MockHttpServletResponse();

		complexDispatcherServlet.service(request, response);
		assertThat(response.getForwardedUrl()).isEqualTo("noview/simple.jsp");
	}

	@Test
	public void withNoViewAndSamePath() throws Exception {
		InternalResourceViewResolver vr = (InternalResourceViewResolver) complexDispatcherServlet
				.getWebApplicationContext().getBean("viewResolver2");
		vr.setSuffix("");

		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/noview");
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThatExceptionOfType(ServletException.class).isThrownBy(() ->
				complexDispatcherServlet.service(request, response));
	}

	@Test // gh-26318
	public void parsedRequestPathIsRestoredOnForward() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(PathPatternParserConfig.class);
		DispatcherServlet servlet = new DispatcherServlet(context);
		servlet.init(servletConfig);

		RequestPath previousRequestPath = RequestPath.parse("/", null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.setDispatcherType(DispatcherType.FORWARD);
		request.setAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE, previousRequestPath);

		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo("test-body");
		assertThat(request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE)).isSameAs(previousRequestPath);
	}

	@Test
	public void dispatcherServletRefresh() throws ServletException {
		MockServletContext servletContext = new MockServletContext("org/springframework/web/context");
		DispatcherServlet servlet = new DispatcherServlet();

		servlet.init(new MockServletConfig(servletContext, "empty"));
		ServletContextAwareBean contextBean =
				(ServletContextAwareBean) servlet.getWebApplicationContext().getBean("servletContextAwareBean");
		ServletConfigAwareBean configBean =
				(ServletConfigAwareBean) servlet.getWebApplicationContext().getBean("servletConfigAwareBean");
		assertThat(contextBean.getServletContext()).isSameAs(servletContext);
		assertThat(configBean.getServletConfig()).isSameAs(servlet.getServletConfig());
		MultipartResolver multipartResolver = servlet.getMultipartResolver();
		assertThat(multipartResolver).isNotNull();

		servlet.refresh();

		ServletContextAwareBean contextBean2 =
				(ServletContextAwareBean) servlet.getWebApplicationContext().getBean("servletContextAwareBean");
		ServletConfigAwareBean configBean2 =
				(ServletConfigAwareBean) servlet.getWebApplicationContext().getBean("servletConfigAwareBean");
		assertThat(contextBean2.getServletContext()).isSameAs(servletContext);
		assertThat(configBean2.getServletConfig()).isSameAs(servlet.getServletConfig());
		assertThat(contextBean2).isNotSameAs(contextBean);
		assertThat(configBean2).isNotSameAs(configBean);
		MultipartResolver multipartResolver2 = servlet.getMultipartResolver();
		assertThat(multipartResolver2).isNotSameAs(multipartResolver);

		servlet.destroy();
	}

	@Test
	public void dispatcherServletContextRefresh() throws ServletException {
		MockServletContext servletContext = new MockServletContext("org/springframework/web/context");
		DispatcherServlet servlet = new DispatcherServlet();

		servlet.init(new MockServletConfig(servletContext, "empty"));
		ServletContextAwareBean contextBean =
				(ServletContextAwareBean) servlet.getWebApplicationContext().getBean("servletContextAwareBean");
		ServletConfigAwareBean configBean =
				(ServletConfigAwareBean) servlet.getWebApplicationContext().getBean("servletConfigAwareBean");
		assertThat(contextBean.getServletContext()).isSameAs(servletContext);
		assertThat(configBean.getServletConfig()).isSameAs(servlet.getServletConfig());
		MultipartResolver multipartResolver = servlet.getMultipartResolver();
		assertThat(multipartResolver).isNotNull();

		((ConfigurableApplicationContext) servlet.getWebApplicationContext()).refresh();

		ServletContextAwareBean contextBean2 =
				(ServletContextAwareBean) servlet.getWebApplicationContext().getBean("servletContextAwareBean");
		ServletConfigAwareBean configBean2 =
				(ServletConfigAwareBean) servlet.getWebApplicationContext().getBean("servletConfigAwareBean");
		assertThat(contextBean2.getServletContext()).isSameAs(servletContext);
		assertThat(configBean2.getServletConfig()).isSameAs(servlet.getServletConfig());
		assertThat(contextBean).isNotSameAs(contextBean2);
		assertThat(configBean).isNotSameAs(configBean2);
		MultipartResolver multipartResolver2 = servlet.getMultipartResolver();
		assertThat(multipartResolver).isNotSameAs(multipartResolver2);

		servlet.destroy();
	}

	@Test
	public void environmentOperations() {
		DispatcherServlet servlet = new DispatcherServlet();
		ConfigurableEnvironment defaultEnv = servlet.getEnvironment();
		assertThat(defaultEnv).isNotNull();
		ConfigurableEnvironment env1 = new StandardServletEnvironment();
		servlet.setEnvironment(env1); // should succeed
		assertThat(servlet.getEnvironment()).isSameAs(env1);
		assertThatIllegalArgumentException().as("non-configurable Environment").isThrownBy(() ->
				servlet.setEnvironment(new DummyEnvironment()));
		class CustomServletEnvironment extends StandardServletEnvironment { }
		@SuppressWarnings("serial")
		DispatcherServlet custom = new DispatcherServlet() {
			@Override
			protected ConfigurableWebEnvironment createEnvironment() {
				return new CustomServletEnvironment();
			}
		};
		assertThat(custom.getEnvironment()).isInstanceOf(CustomServletEnvironment.class);
	}

	@Test
	public void allowedOptionsIncludesPatchMethod() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "OPTIONS", "/foo");
		MockHttpServletResponse response = spy(new MockHttpServletResponse());
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setDispatchOptionsRequest(false);
		servlet.service(request, response);
		verify(response, never()).getHeader(anyString()); // SPR-10341
		assertThat(response.getHeader("Allow")).isEqualTo("GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, PATCH");
	}

	@Test
	public void contextInitializers() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setContextClass(SimpleWebApplicationContext.class);
		servlet.setContextInitializers(new TestWebContextInitializer(), new OtherWebContextInitializer());
		servlet.init(servletConfig);
		assertThat(getServletContext().getAttribute("initialized")).isEqualTo("true");
		assertThat(getServletContext().getAttribute("otherInitialized")).isEqualTo("true");
	}

	@Test
	public void contextInitializerClasses() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setContextClass(SimpleWebApplicationContext.class);
		servlet.setContextInitializerClasses(
				TestWebContextInitializer.class.getName() + "," + OtherWebContextInitializer.class.getName());
		servlet.init(servletConfig);
		assertThat(getServletContext().getAttribute("initialized")).isEqualTo("true");
		assertThat(getServletContext().getAttribute("otherInitialized")).isEqualTo("true");
	}

	@Test
	public void globalInitializerClasses() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setContextClass(SimpleWebApplicationContext.class);
		getServletContext().setInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM,
				TestWebContextInitializer.class.getName() + "," + OtherWebContextInitializer.class.getName());
		servlet.init(servletConfig);
		assertThat(getServletContext().getAttribute("initialized")).isEqualTo("true");
		assertThat(getServletContext().getAttribute("otherInitialized")).isEqualTo("true");
	}

	@Test
	public void mixedInitializerClasses() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setContextClass(SimpleWebApplicationContext.class);
		getServletContext().setInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM,
				TestWebContextInitializer.class.getName());
		servlet.setContextInitializerClasses(OtherWebContextInitializer.class.getName());
		servlet.init(servletConfig);
		assertThat(getServletContext().getAttribute("initialized")).isEqualTo("true");
		assertThat(getServletContext().getAttribute("otherInitialized")).isEqualTo("true");
	}

	@Test
	public void webDavMethod() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "PROPFIND", "/body.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		complexDispatcherServlet.service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("body");
	}


	public static class ControllerFromParent implements Controller {

		@Override
		public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
			return new ModelAndView(ControllerFromParent.class.getName());
		}
	}


	private static class TestWebContextInitializer
			implements ApplicationContextInitializer<ConfigurableWebApplicationContext> {

		@Override
		public void initialize(ConfigurableWebApplicationContext applicationContext) {
			applicationContext.getServletContext().setAttribute("initialized", "true");
		}
	}


	private static class OtherWebContextInitializer
			implements ApplicationContextInitializer<ConfigurableWebApplicationContext> {

		@Override
		public void initialize(ConfigurableWebApplicationContext applicationContext) {
			applicationContext.getServletContext().setAttribute("otherInitialized", "true");
		}
	}


	private static class PathPatternParserConfig {

		@Bean
		public SimpleUrlHandlerMapping handlerMapping() {
			Map<String, Object> urlMap = Collections.singletonMap("/test",
					(HttpRequestHandler) (request, response) -> {
						response.setStatus(200);
						response.getWriter().print("test-body");
					});

			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setPatternParser(new PathPatternParser());
			mapping.setUrlMap(urlMap);
			return mapping;
		}
	}

}
