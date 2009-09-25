/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.servlet.mvc.annotation;

import java.io.IOException;
import java.io.Writer;
import java.io.UnsupportedEncodingException;
import java.net.BindException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Arjen Poutsma
 */
public class AnnotationMethodHandlerExceptionResolverTests {

	private AnnotationMethodHandlerExceptionResolver exceptionResolver;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setUp() {
		exceptionResolver = new AnnotationMethodHandlerExceptionResolver();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		request.setMethod("GET");
	}

	@Test
	public void simple() {
		BindException ex = new BindException();
		SimpleController controller = new SimpleController();
		ModelAndView mav = exceptionResolver.resolveException(request, response, controller, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "BindException", mav.getViewName());
		assertEquals("Invalid status code returned", 406, response.getStatus());
	}
	
	@Test
	public void inherited()	{
		IOException ex = new IOException();
		InheritedController controller = new InheritedController();
		ModelAndView mav = exceptionResolver.resolveException(request, response, controller, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "GenericError", mav.getViewName());
		assertEquals("Invalid status code returned", 500, response.getStatus());
	}
	
	@Test(expected = IllegalStateException.class)
	public void ambiguous() {
		IllegalArgumentException ex = new IllegalArgumentException();
		AmbiguousController controller = new AmbiguousController();
		exceptionResolver.resolveException(request, response, controller, ex);
	}

	@Test
	public void noModelAndView() throws UnsupportedEncodingException {
		IllegalArgumentException ex = new IllegalArgumentException();
		NoMAVReturningController controller = new NoMAVReturningController();
		ModelAndView mav = exceptionResolver.resolveException(request, response, controller, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("ModelAndView not empty", mav.isEmpty());
		assertEquals("Invalid response written", "IllegalArgumentException", response.getContentAsString());
	}

	@Controller
	private static class SimpleController {

		@ExceptionHandler(IOException.class)
		@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
		public String handleIOException(IOException ex, HttpServletRequest request) {
			return ClassUtils.getShortName(ex.getClass());
		}

		@ExceptionHandler(BindException.class)
		@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
		public String handleBindException(Exception ex, HttpServletResponse response) {
			return ClassUtils.getShortName(ex.getClass());
		}

		@ExceptionHandler(IllegalArgumentException.class)
		public String handleIllegalArgumentException(Exception ex) {
			return ClassUtils.getShortName(ex.getClass());
		}

	}

	@Controller
	private static class InheritedController extends SimpleController
	{
		@Override
		public String handleIOException(IOException ex, HttpServletRequest request)	{
			return "GenericError";
		}
	}

	@Controller
	private static class AmbiguousController {

		@ExceptionHandler({BindException.class, IllegalArgumentException.class})
		public String handle1(Exception ex, HttpServletRequest request, HttpServletResponse response)
				throws IOException {
			return ClassUtils.getShortName(ex.getClass());
		}

		@ExceptionHandler
		public String handle2(IllegalArgumentException ex) {
			return ClassUtils.getShortName(ex.getClass());
		}

	}

	@Controller
	private static class NoMAVReturningController {

		@ExceptionHandler(Exception.class)
		public void handle(Exception ex, Writer writer) throws IOException {
			writer.write(ClassUtils.getShortName(ex.getClass()));
		}
	}
}
