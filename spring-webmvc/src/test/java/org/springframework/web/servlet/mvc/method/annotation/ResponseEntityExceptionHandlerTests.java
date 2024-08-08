/*
 * Copyright 2002-2024 the original author or authors.
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

import java.beans.PropertyChangeEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.ErrorResponse;
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
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;

/**
 * Tests for {@link ResponseEntityExceptionHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Yanming Zhou
 */
class ResponseEntityExceptionHandlerTests {

	private final ResponseEntityExceptionHandler exceptionHandler = new ApplicationExceptionHandler();

	private final DefaultHandlerExceptionResolver exceptionResolver = new DefaultHandlerExceptionResolver();

	private MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");

	private final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

	private WebRequest request = new ServletWebRequest(this.servletRequest, this.servletResponse);


	@SuppressWarnings("unchecked")
	@Test
	void supportsAllDefaultHandlerExceptionResolverExceptionTypes() throws Exception {

		ExceptionHandler annotation = ResponseEntityExceptionHandler.class
				.getMethod("handleException", Exception.class, WebRequest.class)
				.getAnnotation(ExceptionHandler.class);

		Arrays.stream(DefaultHandlerExceptionResolver.class.getDeclaredMethods())
				.filter(method -> method.getName().startsWith("handle") && (method.getParameterCount() == 4))
				.filter(method -> !method.getName().equals("handleErrorResponse"))
				.map(method -> method.getParameterTypes()[0])
				.forEach(exceptionType -> assertThat(annotation.value())
						.as("@ExceptionHandler is missing declaration for " + exceptionType.getName())
						.contains((Class<Exception>) exceptionType));
	}

	@Test
	void httpRequestMethodNotSupported() {
		ResponseEntity<Object> entity =
				testException(new HttpRequestMethodNotSupportedException("GET", List.of("POST", "DELETE")));

		assertThat(entity.getHeaders().getFirst(HttpHeaders.ALLOW)).isEqualTo("POST, DELETE");
	}

	@Test
	void httpMediaTypeNotSupported() {
		ResponseEntity<Object> entity = testException(new HttpMediaTypeNotSupportedException(
				MediaType.APPLICATION_JSON, List.of(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML)));

		assertThat(entity.getHeaders().getFirst(HttpHeaders.ACCEPT)).isEqualTo("application/atom+xml, application/xml");
		assertThat(entity.getHeaders().getAcceptPatch()).isEmpty();
	}

	@Test
	void patchHttpMediaTypeNotSupported() {
		this.servletRequest = new MockHttpServletRequest("PATCH", "/");
		this.request = new ServletWebRequest(this.servletRequest, this.servletResponse);

		ResponseEntity<Object> entity = testException(
				new HttpMediaTypeNotSupportedException(
						MediaType.APPLICATION_JSON,
						List.of(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML),
						HttpMethod.PATCH));

		HttpHeaders headers = entity.getHeaders();
		assertThat(headers.getFirst(HttpHeaders.ACCEPT)).isEqualTo("application/atom+xml, application/xml");
		assertThat(headers.getFirst(HttpHeaders.ACCEPT)).isEqualTo("application/atom+xml, application/xml");
		assertThat(headers.getFirst(HttpHeaders.ACCEPT_PATCH)).isEqualTo("application/atom+xml, application/xml");
	}

	@Test
	void httpMediaTypeNotAcceptable() {
		testException(new HttpMediaTypeNotAcceptableException(""));
	}

	@Test
	void missingPathVariable() throws NoSuchMethodException {
		testException(new MissingPathVariableException("param",
				new MethodParameter(getClass().getDeclaredMethod("handle", String.class), 0)));
	}

	@Test
	void missingServletRequestParameter() {
		testException(new MissingServletRequestParameterException("param", "type"));
	}

	@Test
	void servletRequestBindingException() {
		testException(new ServletRequestBindingException("message"));
	}

	@Test
	void errorResponseProblemDetailViaMessageSource() {
		try {
			Locale locale = Locale.UK;
			LocaleContextHolder.setLocale(locale);

			String type = "https://example.com/probs/unsupported-content";
			String title = "Media type is not valid or not supported";
			Class<HttpMediaTypeNotSupportedException> exceptionType = HttpMediaTypeNotSupportedException.class;

			StaticMessageSource source = new StaticMessageSource();
			source.addMessage(ErrorResponse.getDefaultTypeMessageCode(exceptionType), locale, type);
			source.addMessage(ErrorResponse.getDefaultTitleMessageCode(exceptionType), locale, title);
			source.addMessage(ErrorResponse.getDefaultDetailMessageCode(exceptionType, null), locale,
					"Content-Type {0} not supported. Supported: {1}");

			this.exceptionHandler.setMessageSource(source);

			ResponseEntity<?> entity = testException(new HttpMediaTypeNotSupportedException(
					MediaType.APPLICATION_JSON, List.of(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML)));

			ProblemDetail problem = (ProblemDetail) entity.getBody();
			assertThat(problem).isNotNull();
			assertThat(problem.getType()).isEqualTo(URI.create(type));
			assertThat(problem.getTitle()).isEqualTo(title);
			assertThat(problem.getDetail()).isEqualTo(
					"Content-Type application/json not supported. Supported: [application/atom+xml, application/xml]");
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	@Test // gh-30300
	public void reasonAsDetailShouldBeUpdatedViaMessageSource() {

		Locale locale = Locale.UK;
		LocaleContextHolder.setLocale(locale);

		String reason = "bad.request";
		String message = "Breaking Bad Request";
		try {
			StaticMessageSource messageSource = new StaticMessageSource();
			messageSource.addMessage(reason, locale, message);

			this.exceptionHandler.setMessageSource(messageSource);

			ResponseEntity<?> entity = testException(new ResponseStatusException(HttpStatus.BAD_REQUEST, reason));

			ProblemDetail body = (ProblemDetail) entity.getBody();
			assertThat(body.getDetail()).isEqualTo(message);
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	@Test
	void conversionNotSupported() {
		testException(new ConversionNotSupportedException(new Object(), Object.class, null));
	}

	@Test
	void typeMismatch() {
		testException(new TypeMismatchException("foo", String.class));
	}

	@Test
	void typeMismatchWithProblemDetailViaMessageSource() {
		Locale locale = Locale.UK;
		LocaleContextHolder.setLocale(locale);

		try {
			StaticMessageSource messageSource = new StaticMessageSource();
			messageSource.addMessage(
					ErrorResponse.getDefaultDetailMessageCode(TypeMismatchException.class, null), locale,
					"Failed to set {0} to value: {1}");

			this.exceptionHandler.setMessageSource(messageSource);

			ResponseEntity<?> entity = testException(
					new TypeMismatchException(new PropertyChangeEvent(this, "name", "John", "James"), String.class));

			ProblemDetail body = (ProblemDetail) entity.getBody();
			assertThat(body.getDetail()).isEqualTo("Failed to set name to value: James");
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	@Test
	@SuppressWarnings("deprecation")
	public void httpMessageNotReadable() {
		testException(new HttpMessageNotReadableException("message"));
	}

	@Test
	void httpMessageNotWritable() {
		testException(new HttpMessageNotWritableException(""));
	}

	@Test
	void methodArgumentNotValid() throws Exception {
		testException(new MethodArgumentNotValidException(
				new MethodParameter(getClass().getDeclaredMethod("handle", String.class), 0),
				new MapBindingResult(Collections.emptyMap(), "name")));
	}

	@Test
	void handlerMethodValidationException() {
		testException(new HandlerMethodValidationException(mock(MethodValidationResult.class)));
	}

	@Test
	void methodValidationException() {
		testException(new MethodValidationException(mock(MethodValidationResult.class)));
	}

	@Test
	void missingServletRequestPart() {
		testException(new MissingServletRequestPartException("partName"));
	}

	@Test
	void noHandlerFoundException() {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // gh-29626

		ResponseEntity<Object> responseEntity =
				testException(new NoHandlerFoundException("GET", "/resource", requestHeaders));

		assertThat(responseEntity.getHeaders()).isEmpty();
	}

	@Test
	void noResourceFoundException() {
		testException(new NoResourceFoundException(HttpMethod.GET, "/resource"));
	}

	@Test
	void asyncRequestTimeoutException() {
		testException(new AsyncRequestTimeoutException());
	}

	@Test
	void asyncRequestNotUsableException() throws Exception {
		AsyncRequestNotUsableException ex = new AsyncRequestNotUsableException("simulated failure");
		ResponseEntity<Object> entity = this.exceptionHandler.handleException(ex, this.request);
		assertThat(entity).isNull();
	}

	@Test
	void maxUploadSizeExceededException() {
		testException(new MaxUploadSizeExceededException(1000));
	}

	@Test // gh-14287, gh-31541
	void serverErrorWithoutBody() {
		HttpStatusCode code = HttpStatusCode.valueOf(500);
		Exception ex = new IllegalStateException("internal error");
		this.exceptionHandler.handleExceptionInternal(ex, null, new HttpHeaders(), code, this.request);

		assertThat(this.servletRequest.getAttribute("jakarta.servlet.error.exception")).isSameAs(ex);
	}

	@Test
	void controllerAdvice() throws Exception {
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
	void controllerAdviceWithNestedException() {
		StaticWebApplicationContext ctx = new StaticWebApplicationContext();
		ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
		ctx.refresh();

		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.setApplicationContext(ctx);
		resolver.afterPropertiesSet();

		ModelAndView mav = resolver.resolveException(
				this.servletRequest, this.servletResponse, null,
				new IllegalStateException(new ServletRequestBindingException("message")));

		assertThat(mav).isNull();
	}

	@Test
	void controllerAdviceWithinDispatcherServlet() throws Exception {
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
	void controllerAdviceWithNestedExceptionWithinDispatcherServlet() throws Exception {
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
			ResponseEntity<Object> entity = this.exceptionHandler.handleException(ex, this.request);
			assertThat(entity).isNotNull();

			// Verify DefaultHandlerExceptionResolver would set the same status
			this.exceptionResolver.resolveException(this.servletRequest, this.servletResponse, null, ex);
			assertThat(entity.getStatusCode().value()).isEqualTo(this.servletResponse.getStatus());

			return entity;
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
		public void handleRequest() {
			throw new IllegalStateException(new ServletRequestBindingException("message"));
		}
	}


	@ControllerAdvice
	private static class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

		@Override
		protected ResponseEntity<Object> handleServletRequestBindingException(
				ServletRequestBindingException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

			headers = new HttpHeaders();
			headers.set("someHeader", "someHeaderValue");
			return handleExceptionInternal(ex, "error content", headers, status, request);
		}
	}


	@SuppressWarnings("unused")
	void handle(String arg) {
	}

}
