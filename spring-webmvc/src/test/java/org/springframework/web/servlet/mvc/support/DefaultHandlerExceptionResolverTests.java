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

package org.springframework.web.servlet.mvc.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultHandlerExceptionResolver}.
 *
 * @author Arjen Poutsma
 */
public class DefaultHandlerExceptionResolverTests {

	private final DefaultHandlerExceptionResolver exceptionResolver = new DefaultHandlerExceptionResolver();

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@BeforeEach
	public void setup() {
		exceptionResolver.setWarnLogCategory(exceptionResolver.getClass().getName());
	}


	@Test
	public void handleHttpRequestMethodNotSupported() {
		HttpRequestMethodNotSupportedException ex =
				new HttpRequestMethodNotSupportedException("GET", Arrays.asList("POST", "PUT"));
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(405);
		assertThat(response.getHeader("Allow")).as("Invalid Allow header").isEqualTo("POST, PUT");
	}

	@Test
	public void handleHttpMediaTypeNotSupported() {
		HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(new MediaType("text", "plain"),
				Collections.singletonList(new MediaType("application", "pdf")));
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(415);
		assertThat(response.getHeader("Accept")).as("Invalid Accept header").isEqualTo("application/pdf");
	}

	@Test
	public void patchHttpMediaTypeNotSupported() {
		HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
				new MediaType("text", "plain"),
				Collections.singletonList(new MediaType("application", "pdf")),
				HttpMethod.PATCH);
		MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(415);
		assertThat(response.getHeader("Accept-Patch")).as("Invalid Accept header").isEqualTo("application/pdf");
	}

	@Test
	public void handleMissingPathVariable() throws NoSuchMethodException {
		Method method = getClass().getMethod("handle", String.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		MissingPathVariableException ex = new MissingPathVariableException("foo", parameter);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(500);
		assertThat(response.getErrorMessage()).isEqualTo("Required path variable 'foo' is not present.");
	}

	@Test
	public void handleMissingServletRequestParameter() {
		MissingServletRequestParameterException ex = new MissingServletRequestParameterException("foo", "bar");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
		assertThat(response.getErrorMessage()).isEqualTo("Required parameter 'foo' is not present.");
	}

	@Test
	public void handleServletRequestBindingException() {
		String message = "Missing required value - header, cookie, or pathvar";
		ServletRequestBindingException ex = new ServletRequestBindingException(message);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
	}

	@Test
	public void handleTypeMismatch() {
		TypeMismatchException ex = new TypeMismatchException("foo", String.class);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void handleHttpMessageNotReadable() {
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException("foo");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
	}

	@Test
	public void handleHttpMessageNotWritable() {
		HttpMessageNotWritableException ex = new HttpMessageNotWritableException("foo");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(500);
	}

	@Test
	public void handleMethodArgumentNotValid() throws Exception {
		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new TestBean(), "testBean");
		errors.rejectValue("name", "invalid");
		MethodParameter parameter = new MethodParameter(this.getClass().getMethod("handle", String.class), 0);
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, errors);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
	}

	@Test
	public void handleMissingServletRequestPartException() {
		MissingServletRequestPartException ex = new MissingServletRequestPartException("name");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
		assertThat(response.getErrorMessage()).contains("part");
		assertThat(response.getErrorMessage()).contains("name");
		assertThat(response.getErrorMessage()).contains("not present");
	}

	@Test
	public void handleBindException() {
		BindException ex = new BindException(new Object(), "name");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
	}

	@Test
	public void handleNoHandlerFoundException() {
		ServletServerHttpRequest req = new ServletServerHttpRequest(
				new MockHttpServletRequest("GET","/resource"));
		NoHandlerFoundException ex = new NoHandlerFoundException(req.getMethod().name(),
				req.getServletRequest().getRequestURI(),req.getHeaders());
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(404);
	}

	@Test
	public void handleNoResourceFoundException() {
		NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/resource");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(404);
	}

	@Test
	public void handleConversionNotSupportedException() {
		ConversionNotSupportedException ex =
				new ConversionNotSupportedException(new Object(), String.class, new Exception());
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(500);

		// SPR-9653
		assertThat(request.getAttribute("jakarta.servlet.error.exception")).isSameAs(ex);
	}

	@Test  // SPR-14669
	public void handleAsyncRequestTimeoutException() {
		Exception ex = new AsyncRequestTimeoutException();
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertThat(mav).as("No ModelAndView returned").isNotNull();
		assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
		assertThat(response.getStatus()).as("Invalid status code").isEqualTo(503);
	}

	@Test
	public void customModelAndView() {
		ModelAndView expected = new ModelAndView();

		HandlerExceptionResolver resolver = new DefaultHandlerExceptionResolver() {

			@Override
			protected ModelAndView handleHttpRequestMethodNotSupported(
					HttpRequestMethodNotSupportedException ex, HttpServletRequest request,
					HttpServletResponse response, @Nullable Object handler) {

				return expected;
			}
		};

		HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET");

		ModelAndView actual = resolver.resolveException(request, response, null, ex);
		assertThat(actual).isSameAs(expected);
	}


	@SuppressWarnings("unused")
	public void handle(String arg) {
	}

}
