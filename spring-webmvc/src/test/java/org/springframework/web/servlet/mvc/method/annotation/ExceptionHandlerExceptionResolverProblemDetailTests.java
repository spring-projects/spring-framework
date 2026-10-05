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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExceptionHandlerExceptionResolver} with
 * {@link ExceptionHandlerExceptionResolver#setRenderUnhandledExceptionsAsProblemDetails(boolean)}.
 *
 * @author Brian Clozel
 */
class ExceptionHandlerExceptionResolverProblemDetailTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/42");

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final ServerRequestObservationContext observationContext =
			new ServerRequestObservationContext(this.request, this.response);


	ExceptionHandlerExceptionResolverProblemDetailTests() {
		this.request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		this.request.setAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, this.observationContext);
	}


	@Test
	void disabledByDefault() throws Exception {
		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		StaticApplicationContext context = new StaticApplicationContext();
		context.refresh();
		resolver.setApplicationContext(context);
		resolver.afterPropertiesSet();

		assertThat(resolver.isRenderUnhandledExceptionsAsProblemDetails()).isFalse();
		assertThat(resolve(resolver, handlerMethod(), new IllegalStateException("Secret internal state"))).isNull();
	}

	@Test
	void unknownException() throws Exception {
		IllegalStateException ex = new IllegalStateException("Secret internal state");

		ModelAndView mav = resolve(createResolver(), handlerMethod(), ex);
		assertThat(mav).isNotNull();
		assertThat(this.response.getStatus()).isEqualTo(500);
		assertThat(this.response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		assertThat(this.response.getContentAsString())
				.contains("\"status\":500", "\"instance\":\"/orders/42\"")
				.doesNotContain("Secret");
		assertThat(this.observationContext.getError()).isSameAs(ex);
	}

	@Test
	void clientErrorIsNotRecorded() throws Exception {
		HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

		resolve(createResolver(), handlerMethod(), ex);
		assertThat(this.response.getStatus()).isEqualTo(405);
		assertThat(this.response.getHeader(HttpHeaders.ALLOW)).isEqualTo("GET");
		assertThat(this.response.getContentAsString()).contains("\"status\":405");
		assertThat(this.observationContext.getError()).isNull();
	}

	@Test
	void errorResponseInterceptorIsInvoked() throws Exception {
		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.setRenderUnhandledExceptionsAsProblemDetails(true);
		resolver.setMessageConverters(List.of(new JacksonJsonHttpMessageConverter()));
		resolver.setErrorResponseInterceptors(List.of((detail, errorResponse) -> detail.setProperty("traceId", "123")));
		StaticApplicationContext context = new StaticApplicationContext();
		context.refresh();
		resolver.setApplicationContext(context);
		resolver.afterPropertiesSet();

		resolve(resolver, handlerMethod(), new IllegalStateException());
		assertThat(this.response.getContentAsString()).contains("\"traceId\":\"123\"");
	}

	@Test
	void messageSourceFromApplicationContext() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.addMessage(ErrorResponse.getDefaultDetailMessageCode(IllegalStateException.class, null),
				Locale.FRENCH, "Erreur interne");
		this.request.addPreferredLocale(Locale.FRENCH);

		context.refresh();
		resolve(createResolver(context), handlerMethod(), new IllegalStateException());
		assertThat(this.response.getContentAsString()).contains("\"detail\":\"Erreur interne\"");
	}

	@Test
	void controllerAdviceHasPrecedence() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConflictAdvice.class);

		resolve(createResolver(context), handlerMethod(), new IllegalStateException());
		assertThat(this.response.getStatus()).isEqualTo(409);
		assertThat(this.observationContext.getError()).isNull();
	}

	@Test
	void localExceptionHandlerThatRethrows() throws Exception {
		IllegalStateException ex = new IllegalStateException();

		resolve(createResolver(), new HandlerMethod(new RethrowingController(), "handle"), ex);
		assertThat(this.response.getStatus()).isEqualTo(500);
		assertThat(this.response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		assertThat(this.observationContext.getError()).isSameAs(ex);
	}

	@Test
	void localExceptionHandlerThatFails() throws Exception {
		resolve(createResolver(), new HandlerMethod(new FailingController(), "handle"), new IllegalStateException());
		assertThat(this.response.getStatus()).isEqualTo(500);
		assertThat(this.response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
	}

	@Test
	void responseEntityExceptionHandlerThatRethrowsWrappedException() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ProblemDetailsAdvice.class);
		Exception ex = new IllegalStateException(new MissingServletRequestParameterException("id", "int"));

		ModelAndView mav = resolve(createResolver(context), handlerMethod(), ex);
		assertThat(mav).isNotNull();
		assertThat(this.response.getStatus()).isEqualTo(500);
		assertThat(this.response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
	}

	@Test
	void responseEntityExceptionHandlerCoexists() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ProblemDetailsAdvice.class);

		resolve(createResolver(context), handlerMethod(), new MissingServletRequestParameterException("id", "int"));
		assertThat(this.response.getStatus()).isEqualTo(400);
		assertThat(this.response.getContentAsString()).contains("\"detail\":\"Required parameter 'id' is not present.\"");
	}

	@Test
	void anyHandlerType() throws Exception {
		HttpRequestHandler handler = (request, response) -> {};

		ModelAndView mav = resolve(createResolver(), handler, new IllegalStateException());
		assertThat(mav).isNotNull();
		assertThat(this.response.getStatus()).isEqualTo(500);
	}

	@Test
	void nullHandler() throws Exception {
		ModelAndView mav = resolve(createResolver(), null, new IllegalStateException());
		assertThat(mav).isNotNull();
		assertThat(this.response.getStatus()).isEqualTo(500);
	}

	@Test
	void notAppliedToUnmappedHandler() throws Exception {
		ExceptionHandlerExceptionResolver resolver = createResolver();
		resolver.setMappedHandlerClasses(RethrowingController.class);

		assertThat(resolve(resolver, handlerMethod(), new IllegalStateException())).isNull();
	}

	@Test
	void asyncRequestNotUsable() throws Exception {
		ModelAndView mav = resolve(createResolver(), handlerMethod(), new AsyncRequestNotUsableException("Not usable"));
		assertThat(mav).isNotNull();
		assertThat(this.response.getContentAsString()).isEmpty();
		assertThat(this.observationContext.getError()).isNull();
	}

	@Test
	void disconnectedClient() throws Exception {
		IOException ex = new IOException("Broken pipe");

		ModelAndView mav = resolve(createResolver(), handlerMethod(), ex);
		assertThat(mav).isNotNull();
		assertThat(this.response.getStatus()).isEqualTo(500);
		assertThat(this.observationContext.getError()).isSameAs(ex);
	}

	@Test
	void committedResponse() throws Exception {
		this.response.setCommitted(true);

		assertThat(resolve(createResolver(), handlerMethod(), new IllegalStateException())).isNull();
		assertThat(this.observationContext.getError()).isNull();
	}


	private ExceptionHandlerExceptionResolver createResolver() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.refresh();
		return createResolver(context);
	}

	private ExceptionHandlerExceptionResolver createResolver(
			ApplicationContext applicationContext) {
		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.setRenderUnhandledExceptionsAsProblemDetails(true);
		resolver.setMessageConverters(List.of(new JacksonJsonHttpMessageConverter()));
		resolver.setApplicationContext(applicationContext);
		resolver.afterPropertiesSet();
		return resolver;
	}

	private ModelAndView resolve(ExceptionHandlerExceptionResolver resolver, Object handler, Exception ex) {
		return resolver.resolveException(this.request, this.response, handler, ex);
	}

	private static HandlerMethod handlerMethod() throws NoSuchMethodException {
		return new HandlerMethod(new PlainController(), "handle");
	}


	@Controller
	static class PlainController {

		public void handle() {
		}
	}

	@Controller
	static class RethrowingController {

		public void handle() {
		}

		@ExceptionHandler
		public ResponseEntity<String> handleException(IllegalStateException ex) {
			throw ex;
		}
	}

	@Controller
	static class FailingController {

		public void handle() {
		}

		@ExceptionHandler
		public ResponseEntity<String> handleException(IllegalStateException ex) {
			throw new IllegalArgumentException("Failure in exception handler");
		}
	}

	@ControllerAdvice
	static class ConflictAdvice {

		@ExceptionHandler
		public ResponseEntity<String> handleException(IllegalStateException ex) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("conflict");
		}
	}

	@ControllerAdvice
	static class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {
	}

}
