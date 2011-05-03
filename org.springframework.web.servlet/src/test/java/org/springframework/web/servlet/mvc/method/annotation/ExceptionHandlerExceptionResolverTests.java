/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.BindException;
import java.net.SocketException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

/**
 * Test fixture with {@link ExceptionHandlerExceptionResolver}.
 * 
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.1
 */
public class ExceptionHandlerExceptionResolverTests {

	private ExceptionHandlerExceptionResolver exceptionResolver;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setUp() throws Exception {
		exceptionResolver = new ExceptionHandlerExceptionResolver();
		exceptionResolver.afterPropertiesSet();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		request.setMethod("GET");
	}

	@Test
	public void simpleWithIOException() throws NoSuchMethodException {
		IOException ex = new IOException();
		HandlerMethod handlerMethod = new InvocableHandlerMethod(new SimpleController(), "handle");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handlerMethod, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "X:IOException", mav.getViewName());
		assertEquals("Invalid status code returned", 500, response.getStatus());
	}

	@Test
	public void simpleWithSocketException() throws NoSuchMethodException {
		SocketException ex = new SocketException();
		HandlerMethod handlerMethod = new InvocableHandlerMethod(new SimpleController(), "handle");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handlerMethod, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "Y:SocketException", mav.getViewName());
		assertEquals("Invalid status code returned", 406, response.getStatus());
		assertEquals("Invalid status reason returned", "This is simply unacceptable!", response.getErrorMessage());
	}

	@Test
	public void simpleWithFileNotFoundException() throws NoSuchMethodException {
		FileNotFoundException ex = new FileNotFoundException();
		HandlerMethod handlerMethod = new InvocableHandlerMethod(new SimpleController(), "handle");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handlerMethod, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "X:FileNotFoundException", mav.getViewName());
		assertEquals("Invalid status code returned", 500, response.getStatus());
	}

	@Test
	public void simpleWithBindException() throws NoSuchMethodException {
		BindException ex = new BindException();
		HandlerMethod handlerMethod = new InvocableHandlerMethod(new SimpleController(), "handle");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handlerMethod, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "Y:BindException", mav.getViewName());
		assertEquals("Invalid status code returned", 406, response.getStatus());
	}

	@Test
	public void inherited() throws NoSuchMethodException	{
		IOException ex = new IOException();
		HandlerMethod handlerMethod = new InvocableHandlerMethod(new InheritedController(), "handle");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handlerMethod, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "GenericError", mav.getViewName());
		assertEquals("Invalid status code returned", 500, response.getStatus());
	}
	
	@Test(expected = IllegalStateException.class)
	public void ambiguous() throws NoSuchMethodException {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new InvocableHandlerMethod(new AmbiguousController(), "handle");
		exceptionResolver.resolveException(request, response, handlerMethod, ex);
	}

	@Test
	public void noModelAndView() throws UnsupportedEncodingException, NoSuchMethodException {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new InvocableHandlerMethod(new NoMAVReturningController(), "handle");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handlerMethod, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("ModelAndView not empty", mav.isEmpty());
		assertEquals("Invalid response written", "IllegalArgumentException", response.getContentAsString());
	}
	
	@Test
	public void responseBody() throws UnsupportedEncodingException, NoSuchMethodException {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new InvocableHandlerMethod(new ResponseBodyController(), "handle");
		request.addHeader("Accept", "text/plain");
		ModelAndView mav = exceptionResolver.resolveException(request, response, handlerMethod, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("ModelAndView not empty", mav.isEmpty());
		assertEquals("Invalid response written", "IllegalArgumentException", response.getContentAsString());
	}


	@Controller
	private static class SimpleController {
		
		@SuppressWarnings("unused")
		public void handle() {}

		@SuppressWarnings("unused")
		@ExceptionHandler(IOException.class)
		@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
		public String handleIOException(IOException ex, HttpServletRequest request) {
			return "X:" + ClassUtils.getShortName(ex.getClass());
		}

		@SuppressWarnings("unused")
		@ExceptionHandler(SocketException.class)
		@ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE, reason = "This is simply unacceptable!")
		public String handleSocketException(Exception ex, HttpServletResponse response) {
			return "Y:" + ClassUtils.getShortName(ex.getClass());
		}

		@SuppressWarnings("unused")
		@ExceptionHandler(IllegalArgumentException.class)
		public String handleIllegalArgumentException(Exception ex) {
			return ClassUtils.getShortName(ex.getClass());
		}
	}


	@Controller
	private static class InheritedController extends SimpleController {

		@Override
		public String handleIOException(IOException ex, HttpServletRequest request)	{
			return "GenericError";
		}
	}


	@Controller
	private static class AmbiguousController {

		@SuppressWarnings("unused")
		public void handle() {}

		@SuppressWarnings("unused")
		@ExceptionHandler({BindException.class, IllegalArgumentException.class})
		public String handle1(Exception ex, HttpServletRequest request, HttpServletResponse response)
				throws IOException {
			return ClassUtils.getShortName(ex.getClass());
		}

		@SuppressWarnings("unused")
		@ExceptionHandler
		public String handle2(IllegalArgumentException ex) {
			return ClassUtils.getShortName(ex.getClass());
		}
	}


	@Controller
	private static class NoMAVReturningController {

		@SuppressWarnings("unused")
		public void handle() {}

		@SuppressWarnings("unused")
		@ExceptionHandler(Exception.class)
		public void handle(Exception ex, Writer writer) throws IOException {
			writer.write(ClassUtils.getShortName(ex.getClass()));
		}
	}


	@Controller
	private static class ResponseBodyController {

		@SuppressWarnings("unused")
		public void handle() {}

		@SuppressWarnings("unused")
		@ExceptionHandler(Exception.class)
		@ResponseBody
		public String handle(Exception ex) {
			return ClassUtils.getShortName(ex.getClass());
		}
	}
	
}