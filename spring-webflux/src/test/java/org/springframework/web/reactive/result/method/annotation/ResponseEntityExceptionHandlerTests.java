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

package org.springframework.web.reactive.result.method.annotation;


import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsatisfiedRequestParameterException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;

/**
 * Tests for {@link ResponseEntityExceptionHandler}.
 *
 * @author Rossen Stoyanchev
 */
class ResponseEntityExceptionHandlerTests {

	private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());


	@Test
	void handleMethodNotAllowedException() {
		ResponseEntity<ProblemDetail> entity = testException(
				new MethodNotAllowedException(HttpMethod.PATCH, List.of(HttpMethod.GET, HttpMethod.POST)));

		assertThat(entity.getHeaders().getFirst(HttpHeaders.ALLOW)).isEqualTo("GET,POST");
	}

	@Test
	void handleNotAcceptableStatusException() {
		ResponseEntity<ProblemDetail> entity = testException(
				new NotAcceptableStatusException(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)));

		assertThat(entity.getHeaders().getFirst(HttpHeaders.ACCEPT)).isEqualTo("application/json, application/xml");
	}

	@Test
	void handleUnsupportedMediaTypeStatusException() {
		ResponseEntity<ProblemDetail> entity = testException(
				new UnsupportedMediaTypeStatusException(MediaType.APPLICATION_JSON, List.of(MediaType.APPLICATION_XML)));

		assertThat(entity.getHeaders().getFirst(HttpHeaders.ACCEPT)).isEqualTo("application/xml");
	}

	@Test
	void handleMissingRequestValueException() {
		testException(new MissingRequestValueException("id", String.class, "cookie", null));
	}

	@Test
	void handleUnsatisfiedRequestParameterException() {
		testException(new UnsatisfiedRequestParameterException(Collections.emptyList(), new LinkedMultiValueMap<>()));
	}

	@Test
	void handleWebExchangeBindException() {
		testException(new WebExchangeBindException(null, new BeanPropertyBindingResult(new Object(), "foo")));
	}

	@Test
	void handlerMethodValidationException() {
		testException(new HandlerMethodValidationException(mock(MethodValidationResult.class)));
	}

	@Test
	void methodValidationException() {
		MethodValidationException ex = new MethodValidationException(mock(MethodValidationResult.class));
		ResponseEntity<?> entity = this.exceptionHandler.handleException(ex, this.exchange).block();

		assertThat(entity).isNotNull();
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(entity.getBody()).isInstanceOf(ProblemDetail.class);
	}

	@Test
	void handleServerWebInputException() {
		testException(new ServerWebInputException(""));
	}

	@Test
	void handleServerErrorException() {
		testException(new ServerErrorException("", (Method) null, null));
	}

	@Test
	void handleResponseStatusException() {
		testException(new ResponseStatusException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED));
	}

	@Test
	void handleErrorResponseException() {
		testException(new ErrorResponseException(HttpStatus.CONFLICT));
	}

	@Test
	void errorResponseProblemDetailViaMessageSource() {
		Locale locale = Locale.UK;

		String type = "https://example.com/probs/unsupported-content";
		String title = "Media type is not valid or not supported";
		Class<UnsupportedMediaTypeStatusException> exceptionType = UnsupportedMediaTypeStatusException.class;

		StaticMessageSource source = new StaticMessageSource();
		source.addMessage(ErrorResponse.getDefaultTypeMessageCode(exceptionType), locale, type);
		source.addMessage(ErrorResponse.getDefaultTitleMessageCode(exceptionType), locale, title);
		source.addMessage(ErrorResponse.getDefaultDetailMessageCode(exceptionType, null), locale,
				"Content-Type {0} not supported. Supported: {1}");

		this.exceptionHandler.setMessageSource(source);

		Exception ex = new UnsupportedMediaTypeStatusException(
				MediaType.APPLICATION_JSON, List.of(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML));

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/").acceptLanguageAsLocales(locale).build());

		ResponseEntity<?> responseEntity = this.exceptionHandler.handleException(ex, exchange).block();
		assertThat(responseEntity).isNotNull();

		ProblemDetail problem = (ProblemDetail) responseEntity.getBody();
		assertThat(problem).isNotNull();
		assertThat(problem.getType()).isEqualTo(URI.create(type));
		assertThat(problem.getTitle()).isEqualTo(title);
		assertThat(problem.getDetail()).isEqualTo(
				"Content-Type application/json not supported. Supported: [application/atom+xml, application/xml]");
	}

	@Test
	void customExceptionToProblemDetailViaMessageSource() {

		Locale locale = Locale.UK;
		LocaleContextHolder.setLocale(locale);

		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage(
				"problemDetail." + IllegalStateException.class.getName(), locale,
				"Invalid state: {0}");

		this.exceptionHandler.setMessageSource(messageSource);

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
				.acceptLanguageAsLocales(locale).build());

		ResponseEntity<?> responseEntity =
				this.exceptionHandler.handleException(new IllegalStateException("test"), exchange).block();

		ProblemDetail body = (ProblemDetail) responseEntity.getBody();
		assertThat(body.getDetail()).isEqualTo("Invalid state: A");
	}


	@SuppressWarnings("unchecked")
	private ResponseEntity<ProblemDetail> testException(ErrorResponseException exception) {
		ResponseEntity<?> entity = this.exceptionHandler.handleException(exception, this.exchange).block();
		assertThat(entity).isNotNull();
		assertThat(entity.getStatusCode()).isEqualTo(exception.getStatusCode());
		assertThat(entity.getBody()).isInstanceOf(ProblemDetail.class);
		return (ResponseEntity<ProblemDetail>) entity;
	}


	private static class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

		private Mono<ResponseEntity<Object>> handleAndSetTypeToExceptionName(
				ErrorResponseException ex, HttpHeaders headers, HttpStatusCode status, ServerWebExchange exchange) {

			return handleExceptionInternal(ex, null, headers, status, exchange);
		}

		@Override
		protected Mono<ResponseEntity<Object>> handleMethodNotAllowedException(
				MethodNotAllowedException ex, HttpHeaders headers, HttpStatusCode status,
				ServerWebExchange exchange) {

			return handleAndSetTypeToExceptionName(ex, headers, status, exchange);
		}

		@Override
		protected Mono<ResponseEntity<Object>> handleNotAcceptableStatusException(
				NotAcceptableStatusException ex, HttpHeaders headers, HttpStatusCode status,
				ServerWebExchange exchange) {

			return handleAndSetTypeToExceptionName(ex, headers, status, exchange);
		}

		@Override
		protected Mono<ResponseEntity<Object>> handleUnsupportedMediaTypeStatusException(
				UnsupportedMediaTypeStatusException ex, HttpHeaders headers, HttpStatusCode status,
				ServerWebExchange exchange) {

			return handleAndSetTypeToExceptionName(ex, headers, status, exchange);
		}

		@Override
		protected Mono<ResponseEntity<Object>> handleMissingRequestValueException(
				MissingRequestValueException ex, HttpHeaders headers, HttpStatusCode status,
				ServerWebExchange exchange) {

			return handleAndSetTypeToExceptionName(ex, headers, status, exchange);
		}

		@Override
		protected Mono<ResponseEntity<Object>> handleUnsatisfiedRequestParameterException(
				UnsatisfiedRequestParameterException ex, HttpHeaders headers, HttpStatusCode status,
				ServerWebExchange exchange) {

			return handleAndSetTypeToExceptionName(ex, headers, status, exchange);
		}

		@Override
		protected Mono<ResponseEntity<Object>> handleWebExchangeBindException(
				WebExchangeBindException ex, HttpHeaders headers, HttpStatusCode status,
				ServerWebExchange exchange) {

			return handleAndSetTypeToExceptionName(ex, headers, status, exchange);
		}

		@Override
		protected Mono<ResponseEntity<Object>> handleServerWebInputException(
				ServerWebInputException ex, HttpHeaders headers, HttpStatusCode status,
				ServerWebExchange exchange) {

			return handleAndSetTypeToExceptionName(ex, headers, status, exchange);
		}

		@Override
		protected Mono<ResponseEntity<Object>> handleResponseStatusException(
				ResponseStatusException ex, HttpHeaders headers, HttpStatusCode status,
				ServerWebExchange exchange) {

			return handleAndSetTypeToExceptionName(ex, headers, status, exchange);
		}

		@Override
		protected Mono<ResponseEntity<Object>> handleServerErrorException(
				ServerErrorException ex, HttpHeaders headers, HttpStatusCode status,
				ServerWebExchange exchange) {

			return handleAndSetTypeToExceptionName(ex, headers, status, exchange);
		}

		@Override
		protected Mono<ResponseEntity<Object>> handleErrorResponseException(
				ErrorResponseException ex, HttpHeaders headers, HttpStatusCode status,
				ServerWebExchange exchange) {

			return handleAndSetTypeToExceptionName(ex, headers, status, exchange);
		}

		public Mono<ResponseEntity<Object>> handleException(IllegalStateException ex, ServerWebExchange exchange) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			ProblemDetail body = createProblemDetail(ex, status, ex.getMessage(), null, new Object[] {"A"}, exchange);
			return handleExceptionInternal(ex, body, null, status, exchange);
		}
	}

}
