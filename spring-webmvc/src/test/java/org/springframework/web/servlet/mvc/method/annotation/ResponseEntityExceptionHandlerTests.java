/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link ResponseEntityExceptionHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseEntityExceptionHandlerTests {

	private ResponseEntityExceptionHandler exceptionHandlerSupport = new ApplicationExceptionHandler();

	private DefaultHandlerExceptionResolver defaultExceptionResolver = new DefaultHandlerExceptionResolver();

	private MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");

	private MockHttpServletResponse servletResponse = new MockHttpServletResponse();

	private WebRequest request = new ServletWebRequest(this.servletRequest, this.servletResponse);


	@Test
	public void supportsAllDefaultHandlerExceptionResolverExceptionTypes() throws Exception {
		Class<ResponseEntityExceptionHandler> clazz = ResponseEntityExceptionHandler.class;
		Method handleExceptionMethod = clazz.getMethod("handleException", Exception.class, WebRequest.class);
		ExceptionHandler annotation = handleExceptionMethod.getAnnotation(ExceptionHandler.class);
		List<Class<?>> exceptionTypes = Arrays.asList(annotation.value());

		for (Method method : DefaultHandlerExceptionResolver.class.getDeclaredMethods()) {
			Class<?>[] paramTypes = method.getParameterTypes();
			if (method.getName().startsWith("handle") && (paramTypes.length == 4)) {
				String name = paramTypes[0].getSimpleName();
				assertThat(exceptionTypes.contains(paramTypes[0])).as("@ExceptionHandler is missing " + name).isTrue();
			}
		}
	}

	@Test
	public void httpRequestMethodNotSupported() {
		List<String> supported = Arrays.asList("POST", "DELETE");
		Exception ex = new HttpRequestMethodNotSupportedException("GET", supported);

		ResponseEntity<Object> responseEntity = testException(ex);
		assertThat(responseEntity.getHeaders().getAllow()).isEqualTo(EnumSet.of(HttpMethod.POST, HttpMethod.DELETE));
	}

	@Test
	public void handleHttpMediaTypeNotSupported() {
		List<MediaType> acceptable = Arrays.asList(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML);
		Exception ex = new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_JSON, acceptable);

		ResponseEntity<Object> responseEntity = testException(ex);
		assertThat(responseEntity.getHeaders().getAccept()).isEqualTo(acceptable);
		assertThat(responseEntity.getHeaders().getAcceptPatch()).isEmpty();
	}

	@Test
	public void patchHttpMediaTypeNotSupported() {
		this.servletRequest = new MockHttpServletRequest("PATCH", "/");
		this.request = new ServletWebRequest(this.servletRequest, this.servletResponse);

		List<MediaType> acceptable = Arrays.asList(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML);
		Exception ex = new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_JSON, acceptable);

		ResponseEntity<Object> responseEntity = testException(ex);
		assertThat(responseEntity.getHeaders().getAccept()).isEqualTo(acceptable);
		assertThat(responseEntity.getHeaders().getAcceptPatch()).isEqualTo(acceptable);
	}

	@Test
	public void httpMediaTypeNotAcceptable() {
		Exception ex = new HttpMediaTypeNotAcceptableException("");
		testException(ex);
	}

	@Test
	public void missingPathVariable() throws NoSuchMethodException {
		Method method = getClass().getDeclaredMethod("handle", String.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		Exception ex = new MissingPathVariableException("param", parameter);
		testException(ex);
	}

	@Test
	public void missingServletRequestParameter() {
		Exception ex = new MissingServletRequestParameterException("param", "type");
		testException(ex);
	}

	@Test
	public void servletRequestBindingException() {
		Exception ex = new ServletRequestBindingException("message");
		testException(ex);
	}

	@Test
	public void conversionNotSupported() {
		Exception ex = new ConversionNotSupportedException(new Object(), Object.class, null);
		testException(ex);
	}

	@Test
	public void typeMismatch() {
		Exception ex = new TypeMismatchException("foo", String.class);
		testException(ex);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void httpMessageNotReadable() {
		Exception ex = new HttpMessageNotReadableException("message");
		testException(ex);
	}

	@Test
	public void httpMessageNotWritable() {
		Exception ex = new HttpMessageNotWritableException("");
		testException(ex);
	}

	@Test
	public void methodArgumentNotValid() {
		Exception ex = mock(MethodArgumentNotValidException.class);
		testException(ex);
	}

	@Test
	public void missingServletRequestPart() {
		Exception ex = new MissingServletRequestPartException("partName");
		testException(ex);
	}

	@Test
	public void bindException() {
		Exception ex = new BindException(new Object(), "name");
		testException(ex);
	}

	@Test
	public void noHandlerFoundException() {
		ServletServerHttpRequest req = new ServletServerHttpRequest(
				new MockHttpServletRequest("GET","/resource"));
		Exception ex = new NoHandlerFoundException(req.getMethod().toString(),
				req.getServletRequest().getRequestURI(),req.getHeaders());
		testException(ex);
	}

	@Test
	public void asyncRequestTimeoutException() {
		testException(new AsyncRequestTimeoutException());
	}

	@Test
	public void controllerAdvice() throws Exception {
		StaticWebApplicationContext ctx = new StaticWebApplicationContext();
		ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
		ctx.refresh();

		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.setApplicationContext(ctx);
		resolver.afterPropertiesSet();

		ServletRequestBindingException ex = new ServletRequestBindingException("message");
		assertThat(resolver.resolveException(this.servletRequest, this.servletResponse, null, ex)).isNotNull();

		assertThat(this.servletResponse.getStatus()).isEqualTo(400);
		assertThat(this.servletResponse.getContentAsString()).isEqualTo("error content");
		assertThat(this.servletResponse.getHeader("someHeader")).isEqualTo("someHeaderValue");
	}

	@Test
	public void controllerAdviceWithNestedException() {
		StaticWebApplicationContext ctx = new StaticWebApplicationContext();
		ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
		ctx.refresh();

		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.setApplicationContext(ctx);
		resolver.afterPropertiesSet();

		IllegalStateException ex = new IllegalStateException(new ServletRequestBindingException("message"));
		assertThat(resolver.resolveException(this.servletRequest, this.servletResponse, null, ex)).isNull();
	}

	@Test
	public void controllerAdviceWithinDispatcherServlet() throws Exception {
		StaticWebApplicationContext ctx = new StaticWebApplicationContext();
		ctx.registerSingleton("controller", ExceptionThrowingController.class);
		ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
		ctx.refresh();

		DispatcherServlet servlet = new DispatcherServlet(ctx);
		servlet.init(new MockServletConfig());
		servlet.service(this.servletRequest, this.servletResponse);

		assertThat(this.servletResponse.getStatus()).isEqualTo(400);
		assertThat(this.servletResponse.getContentAsString()).isEqualTo("error content");
		assertThat(this.servletResponse.getHeader("someHeader")).isEqualTo("someHeaderValue");
	}

	@Test
	public void controllerAdviceWithNestedExceptionWithinDispatcherServlet() throws Exception {
		StaticWebApplicationContext ctx = new StaticWebApplicationContext();
		ctx.registerSingleton("controller", NestedExceptionThrowingController.class);
		ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
		ctx.refresh();

		DispatcherServlet servlet = new DispatcherServlet(ctx);
		servlet.init(new MockServletConfig());
		try {
			servlet.service(this.servletRequest, this.servletResponse);
		}
		catch (ServletException ex) {
			boolean condition1 = ex.getCause() instanceof IllegalStateException;
			assertThat(condition1).isTrue();
			boolean condition = ex.getCause().getCause() instanceof ServletRequestBindingException;
			assertThat(condition).isTrue();
		}
	}


	private ResponseEntity<Object> testException(Exception ex) {
		try {
			ResponseEntity<Object> responseEntity = this.exceptionHandlerSupport.handleException(ex, this.request);

			// SPR-9653
			if (HttpStatus.INTERNAL_SERVER_ERROR.equals(responseEntity.getStatusCode())) {
				assertThat(this.servletRequest.getAttribute("javax.servlet.error.exception")).isSameAs(ex);
			}

			this.defaultExceptionResolver.resolveException(this.servletRequest, this.servletResponse, null, ex);

			assertThat(responseEntity.getStatusCode().value()).isEqualTo(this.servletResponse.getStatus());

			return responseEntity;
		}
		catch (Exception ex2) {
			throw new IllegalStateException("handleException threw exception", ex2);
		}
	}


	@Controller
	private static class ExceptionThrowingController {

		@RequestMapping("/")
		public void handleRequest() throws Exception {
			throw new ServletRequestBindingException("message");
		}
	}


	@Controller
	private static class NestedExceptionThrowingController {

		@RequestMapping("/")
		public void handleRequest() throws Exception {
			throw new IllegalStateException(new ServletRequestBindingException("message"));
		}
	}


	@ControllerAdvice
	private static class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

		@Override
		protected ResponseEntity<Object> handleServletRequestBindingException(
				ServletRequestBindingException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

			headers.set("someHeader", "someHeaderValue");
			return handleExceptionInternal(ex, "error content", headers, status, request);
		}
	}


	@SuppressWarnings("unused")
	void handle(String arg) {
	}

}
