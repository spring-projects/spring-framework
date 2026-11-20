/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;

import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link ServletResponseMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 */
class ServletResponseMethodArgumentResolverTests {

	private ServletResponseMethodArgumentResolver resolver;

	private ModelAndViewContainer mavContainer;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest webRequest;

	private Method method;


	@BeforeEach
	void setup() throws Exception {
		resolver = new ServletResponseMethodArgumentResolver();
		mavContainer = new ModelAndViewContainer();
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(new MockHttpServletRequest(), servletResponse);

		method = getClass().getMethod("supportedParams", ServletResponse.class, OutputStream.class, Writer.class);
	}


	@Test
	void servletResponse() throws Exception {
		MethodParameter servletResponseParameter = new MethodParameter(method, 0);
		assertThat(resolver.supportsParameter(servletResponseParameter)).as("ServletResponse not supported").isTrue();

		Object result = resolver.resolveArgument(servletResponseParameter, mavContainer, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(servletResponse);
		assertThat(mavContainer.isRequestHandled()).isTrue();
	}

	@Test  // SPR-8983
	void servletResponseNoMavContainer() throws Exception {
		MethodParameter servletResponseParameter = new MethodParameter(method, 0);
		assertThat(resolver.supportsParameter(servletResponseParameter)).as("ServletResponse not supported").isTrue();

		Object result = resolver.resolveArgument(servletResponseParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(servletResponse);
	}

	@Test
	void outputStream() throws Exception {
		MethodParameter outputStreamParameter = new MethodParameter(method, 1);
		assertThat(resolver.supportsParameter(outputStreamParameter)).as("OutputStream not supported").isTrue();

		Object result = resolver.resolveArgument(outputStreamParameter, mavContainer, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(servletResponse.getOutputStream());
		assertThat(mavContainer.isRequestHandled()).isTrue();
	}

	@Test
	void writer() throws Exception {
		MethodParameter writerParameter = new MethodParameter(method, 2);
		assertThat(resolver.supportsParameter(writerParameter)).as("Writer not supported").isTrue();

		Object result = resolver.resolveArgument(writerParameter, mavContainer, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(servletResponse.getWriter());
		assertThat(mavContainer.isRequestHandled()).isTrue();
	}


	@SuppressWarnings("unused")
	public void supportedParams(ServletResponse p0, OutputStream p1, Writer p2) {
	}

}
