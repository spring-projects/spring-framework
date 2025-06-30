/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.util.Collections;
import java.util.Properties;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Seth Ladd
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 */
class SimpleMappingExceptionResolverTests {

	private SimpleMappingExceptionResolver exceptionResolver;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private Object handler1;
	private Object handler2;
	private Exception genericException;

	@BeforeEach
	void setUp() {
		exceptionResolver = new SimpleMappingExceptionResolver();
		handler1 = "";
		handler2 = new Object();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		request.setMethod("GET");
		genericException = new Exception();
	}

	@Test
	void setOrder() {
		exceptionResolver.setOrder(2);
		assertThat(exceptionResolver.getOrder()).isEqualTo(2);
	}

	@Test
	void defaultErrorView() {
		exceptionResolver.setDefaultErrorView("default-view");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(mav.getViewName()).isEqualTo("default-view");
		assertThat(mav.getModel().get(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE)).isEqualTo(genericException);
	}

	@Test
	void defaultErrorViewDifferentHandler() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertThat(mav).isNull();
	}

	@Test
	void defaultErrorViewDifferentHandlerClass() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setMappedHandlerClasses(String.class);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertThat(mav).isNull();
	}

	@Test
	void nullExceptionAttribute() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setExceptionAttribute(null);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(mav.getViewName()).isEqualTo("default-view");
		assertThat(mav.getModel().get(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE)).isNull();
	}

	@Test
	void nullExceptionMappings() {
		exceptionResolver.setExceptionMappings(null);
		exceptionResolver.setDefaultErrorView("default-view");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(mav.getViewName()).isEqualTo("default-view");
	}

	@Test
	void noDefaultStatusCode() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	void setDefaultStatusCode() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setDefaultStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
	}

	@Test
	void noDefaultStatusCodeInInclude() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setDefaultStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		request.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "some path");
		exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	void specificStatusCode() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setDefaultStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		Properties statusCodes = new Properties();
		statusCodes.setProperty("default-view", "406");
		exceptionResolver.setStatusCodes(statusCodes);
		exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_ACCEPTABLE);
	}

	@Test
	void simpleExceptionMapping() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setWarnLogCategory("HANDLER_EXCEPTION");
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(mav.getViewName()).isEqualTo("error");
	}

	@Test
	void exactExceptionMappingWithHandlerSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(mav.getViewName()).isEqualTo("error");
	}

	@Test
	void exactExceptionMappingWithHandlerClassSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		exceptionResolver.setMappedHandlerClasses(String.class);
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(mav.getViewName()).isEqualTo("error");
	}

	@Test
	void exactExceptionMappingWithHandlerInterfaceSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		exceptionResolver.setMappedHandlerClasses(Comparable.class);
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(mav.getViewName()).isEqualTo("error");
	}

	@Test
	void simpleExceptionMappingWithHandlerSpecifiedButWrongHandler() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertThat(mav).isNull();
	}

	@Test
	void simpleExceptionMappingWithHandlerClassSpecifiedButWrongHandler() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setMappedHandlerClasses(String.class);
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertThat(mav).isNull();
	}

	@Test
	void simpleExceptionMappingWithExclusion() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setExceptionMappings(props);
		exceptionResolver.setExcludedExceptions(IllegalArgumentException.class);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, new IllegalArgumentException());
		assertThat(mav).isNull();
	}

	@Test
	void missingExceptionInMapping() {
		Properties props = new Properties();
		props.setProperty("SomeFooThrowable", "error");
		exceptionResolver.setWarnLogCategory("HANDLER_EXCEPTION");
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(mav).isNull();
	}

	@Test
	void twoMappings() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("AnotherException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(mav.getViewName()).isEqualTo("error");
	}

	@Test
	void twoMappingsOneShortOneLong() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		props.setProperty("AnotherException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertThat(mav.getViewName()).isEqualTo("error");
	}

	@Test
	void twoMappingsOneShortOneLongThrowOddException() {
		Exception oddException = new SomeOddException();
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertThat(mav.getViewName()).isEqualTo("another-error");
	}

	@Test
	void twoMappingsThrowOddExceptionUseLongExceptionMapping() {
		Exception oddException = new SomeOddException();
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertThat(mav.getViewName()).isEqualTo("another-error");
	}

	@Test
	void threeMappings() {
		Exception oddException = new AnotherOddException();
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		props.setProperty("AnotherOddException", "another-some-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertThat(mav.getViewName()).isEqualTo("another-some-error");
	}


	@SuppressWarnings("serial")
	private static class SomeOddException extends Exception {

	}


	@SuppressWarnings("serial")
	private static class AnotherOddException extends Exception {

	}

}
