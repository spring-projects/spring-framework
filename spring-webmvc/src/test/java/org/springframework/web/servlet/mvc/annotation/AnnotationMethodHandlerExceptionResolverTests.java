/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.BindException;
import java.net.SocketException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@Deprecated
public class AnnotationMethodHandlerExceptionResolverTests {

	private final AnnotationMethodHandlerExceptionResolver exceptionResolver = new AnnotationMethodHandlerExceptionResolver();

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "");

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	public void simpleWithIOException() {
		IOException ex = new IOException();
		SimpleController controller = new SimpleController();
		ModelAndView mav = exceptionResolver.resolveException(request, response, controller, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "X:IOException", mav.getViewName());
		assertEquals("Invalid status code returned", 500, response.getStatus());
	}

	@Test
	public void simpleWithSocketException() {
		SocketException ex = new SocketException();
		SimpleController controller = new SimpleController();
		ModelAndView mav = exceptionResolver.resolveException(request, response, controller, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "Y:SocketException", mav.getViewName());
		assertEquals("Invalid status code returned", 406, response.getStatus());
		assertEquals("Invalid status reason returned", "This is simply unacceptable!", response.getErrorMessage());
	}

	@Test
	public void simpleWithFileNotFoundException() {
		FileNotFoundException ex = new FileNotFoundException();
		SimpleController controller = new SimpleController();
		ModelAndView mav = exceptionResolver.resolveException(request, response, controller, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "X:FileNotFoundException", mav.getViewName());
		assertEquals("Invalid status code returned", 500, response.getStatus());
	}

	@Test
	public void simpleWithBindException() {
		BindException ex = new BindException();
		SimpleController controller = new SimpleController();
		ModelAndView mav = exceptionResolver.resolveException(request, response, controller, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "Y:BindException", mav.getViewName());
		assertEquals("Invalid status code returned", 406, response.getStatus());
	}

	@Test
	public void simpleWithNumberFormatExceptionAndComposedResponseStatusAnnotation() {
		NumberFormatException ex = new NumberFormatException();
		SimpleController controller = new SimpleController();
		ModelAndView mav = exceptionResolver.resolveException(request, response, controller, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertEquals("Invalid view name returned", "X:NumberFormatException", mav.getViewName());
		assertEquals("Invalid status code returned", 400, response.getStatus());
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

	@Test
	public void responseBody() throws UnsupportedEncodingException {
		IllegalArgumentException ex = new IllegalArgumentException();
		ResponseBodyController controller = new ResponseBodyController();
		request.addHeader("Accept", "text/plain");
		ModelAndView mav = exceptionResolver.resolveException(request, response, controller, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("ModelAndView not empty", mav.isEmpty());
		assertEquals("Invalid response written", "IllegalArgumentException", response.getContentAsString());
	}

	// SPR-9209

	@Test
	public void cachingSideEffect() {
		IllegalArgumentException ex = new IllegalArgumentException();
		SimpleController controller = new SimpleController();

		ModelAndView mav = exceptionResolver.resolveException(request, response, controller, ex);
		assertNotNull("No ModelAndView returned", mav);

		mav = exceptionResolver.resolveException(request, response, controller, new NullPointerException());
		assertNull(mav);
	}

	@ResponseStatus
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComposedResponseStatus {

		@AliasFor(annotation = ResponseStatus.class, attribute = "code")
		HttpStatus responseStatus() default HttpStatus.INTERNAL_SERVER_ERROR;
	}

	@Controller
	private static class SimpleController {

		@ExceptionHandler(IOException.class)
		@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
		public String handleIOException(IOException ex, HttpServletRequest request) {
			return "X:" + ex.getClass().getSimpleName();
		}

		@ExceptionHandler(SocketException.class)
		@ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE, reason = "This is simply unacceptable!")
		public String handleSocketException(Exception ex, HttpServletResponse response) {
			return "Y:" + ex.getClass().getSimpleName();
		}

		@ExceptionHandler(IllegalArgumentException.class)
		public String handleIllegalArgumentException(Exception ex) {
			return ex.getClass().getSimpleName();
		}

		@ExceptionHandler(NumberFormatException.class)
		@ComposedResponseStatus(responseStatus = HttpStatus.BAD_REQUEST)
		public String handleNumberFormatException(NumberFormatException ex) {
			return "X:" + ex.getClass().getSimpleName();
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

		@ExceptionHandler({BindException.class, IllegalArgumentException.class})
		public String handle1(Exception ex, HttpServletRequest request, HttpServletResponse response)
				throws IOException {
			return ex.getClass().getSimpleName();
		}

		@ExceptionHandler
		public String handle2(IllegalArgumentException ex) {
			return ex.getClass().getSimpleName();
		}
	}


	@Controller
	private static class NoMAVReturningController {

		@ExceptionHandler(Exception.class)
		public void handle(Exception ex, Writer writer) throws IOException {
			writer.write(ex.getClass().getSimpleName());
		}
	}


	@Controller
	private static class ResponseBodyController {

		@ExceptionHandler(Exception.class)
		@ResponseBody
		public String handle(Exception ex) {
			return ex.getClass().getSimpleName();
		}
	}

}
