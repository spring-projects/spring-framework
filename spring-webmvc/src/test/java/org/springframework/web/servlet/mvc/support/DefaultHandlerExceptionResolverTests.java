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

package org.springframework.web.servlet.mvc.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

/** @author Arjen Poutsma */
public class DefaultHandlerExceptionResolverTests {

	private DefaultHandlerExceptionResolver exceptionResolver;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setUp() {
		exceptionResolver = new DefaultHandlerExceptionResolver();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		request.setMethod("GET");
	}

	@Test
	public void handleNoSuchRequestHandlingMethod() {
		NoSuchRequestHandlingMethodException ex = new NoSuchRequestHandlingMethodException(request);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 404, response.getStatus());
	}

	@Test
	public void handleHttpRequestMethodNotSupported() {
		HttpRequestMethodNotSupportedException ex =
				new HttpRequestMethodNotSupportedException("GET", new String[]{"POST", "PUT"});
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 405, response.getStatus());
		assertEquals("Invalid Allow header", "POST, PUT", response.getHeader("Allow"));
	}

	@Test
	public void handleHttpMediaTypeNotSupported() {
		HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(new MediaType("text", "plain"),
				Collections.singletonList(new MediaType("application", "pdf")));
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 415, response.getStatus());
		assertEquals("Invalid Accept header", "application/pdf", response.getHeader("Accept"));
	}

	@Test
	public void handleMissingServletRequestParameter() {
		MissingServletRequestParameterException ex = new MissingServletRequestParameterException("foo", "bar");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 400, response.getStatus());
	}

	@Test
	public void handleServletRequestBindingException() {
		String message = "Missing required value - header, cookie, or pathvar";
		ServletRequestBindingException ex = new ServletRequestBindingException(message);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 400, response.getStatus());
	}

	@Test
	public void handleTypeMismatch() {
		TypeMismatchException ex = new TypeMismatchException("foo", String.class);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 400, response.getStatus());
	}

	@Test
	public void handleHttpMessageNotReadable() {
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException("foo");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 400, response.getStatus());
	}

	@Test
	public void handleHttpMessageNotWritable() {
		HttpMessageNotWritableException ex = new HttpMessageNotWritableException("foo");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 500, response.getStatus());
	}

	@Test
	public void handleMethodArgumentNotValid() throws Exception {
		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new TestBean(), "testBean");
		errors.rejectValue("name", "invalid");
		MethodParameter parameter = new MethodParameter(this.getClass().getMethod("handle", String.class), 0);
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, errors);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 400, response.getStatus());
	}

	@Test
	public void handleMissingServletRequestPartException() throws Exception {
		MissingServletRequestPartException ex = new MissingServletRequestPartException("name");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 400, response.getStatus());
	}

	@Test
	public void handleBindException() throws Exception {
		BindException ex = new BindException(new Object(), "name");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 400, response.getStatus());
	}

	public void handle(String arg) {
	}

}
