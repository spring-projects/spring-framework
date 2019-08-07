/*
 * Copyright 2002-2018 the original author or authors.
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
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;

/**
 * @author Seth Ladd
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 */
public class SimpleMappingExceptionResolverTests {

	private SimpleMappingExceptionResolver exceptionResolver;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private Object handler1;
	private Object handler2;
	private Exception genericException;

	@Before
	public void setUp() throws Exception {
		exceptionResolver = new SimpleMappingExceptionResolver();
		handler1 = new String();
		handler2 = new Object();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		request.setMethod("GET");
		genericException = new Exception();
	}

	@Test
	public void setOrder() {
		exceptionResolver.setOrder(2);
		assertEquals(2, exceptionResolver.getOrder());
	}

	@Test
	public void defaultErrorView() {
		exceptionResolver.setDefaultErrorView("default-view");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("default-view", mav.getViewName());
		assertEquals(genericException, mav.getModel().get(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE));
	}

	@Test
	public void defaultErrorViewDifferentHandler() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertNull(mav);
	}

	@Test
	public void defaultErrorViewDifferentHandlerClass() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setMappedHandlerClasses(String.class);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertNull(mav);
	}

	@Test
	public void nullExceptionAttribute() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setExceptionAttribute(null);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("default-view", mav.getViewName());
		assertNull(mav.getModel().get(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE));
	}

	@Test
	public void nullExceptionMappings() {
		exceptionResolver.setExceptionMappings(null);
		exceptionResolver.setDefaultErrorView("default-view");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("default-view", mav.getViewName());
	}

	@Test
	public void noDefaultStatusCode() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void setDefaultStatusCode() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setDefaultStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
	}

	@Test
	public void noDefaultStatusCodeInInclude() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setDefaultStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		request.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "some path");
		exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void specificStatusCode() {
		exceptionResolver.setDefaultErrorView("default-view");
		exceptionResolver.setDefaultStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		Properties statusCodes = new Properties();
		statusCodes.setProperty("default-view", "406");
		exceptionResolver.setStatusCodes(statusCodes);
		exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals(HttpServletResponse.SC_NOT_ACCEPTABLE, response.getStatus());
	}

	@Test
	public void simpleExceptionMapping() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setWarnLogCategory("HANDLER_EXCEPTION");
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void exactExceptionMappingWithHandlerSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void exactExceptionMappingWithHandlerClassSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		exceptionResolver.setMappedHandlerClasses(String.class);
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void exactExceptionMappingWithHandlerInterfaceSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		exceptionResolver.setMappedHandlerClasses(Comparable.class);
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void simpleExceptionMappingWithHandlerSpecifiedButWrongHandler() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertNull(mav);
	}

	@Test
	public void simpleExceptionMappingWithHandlerClassSpecifiedButWrongHandler() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setMappedHandlerClasses(String.class);
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertNull(mav);
	}

	@Test
	public void simpleExceptionMappingWithExclusion() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setExceptionMappings(props);
		exceptionResolver.setExcludedExceptions(IllegalArgumentException.class);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, new IllegalArgumentException());
		assertNull(mav);
	}

	@Test
	public void missingExceptionInMapping() {
		Properties props = new Properties();
		props.setProperty("SomeFooThrowable", "error");
		exceptionResolver.setWarnLogCategory("HANDLER_EXCEPTION");
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertNull(mav);
	}

	@Test
	public void twoMappings() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("AnotherException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void twoMappingsOneShortOneLong() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		props.setProperty("AnotherException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void twoMappingsOneShortOneLongThrowOddException() {
		Exception oddException = new SomeOddException();
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertEquals("another-error", mav.getViewName());
	}

	@Test
	public void twoMappingsThrowOddExceptionUseLongExceptionMapping() {
		Exception oddException = new SomeOddException();
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertEquals("another-error", mav.getViewName());
	}

	@Test
	public void threeMappings() {
		Exception oddException = new AnotherOddException();
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		props.setProperty("AnotherOddException", "another-some-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertEquals("another-some-error", mav.getViewName());
	}


	@SuppressWarnings("serial")
	private static class SomeOddException extends Exception {

	}


	@SuppressWarnings("serial")
	private static class AnotherOddException extends Exception {

	}

}
