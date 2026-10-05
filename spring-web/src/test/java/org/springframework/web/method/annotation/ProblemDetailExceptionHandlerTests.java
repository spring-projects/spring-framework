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

package org.springframework.web.method.annotation;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.testfixture.http.MockHttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ProblemDetailExceptionHandler}.
 *
 * @author Brian Clozel
 */
class ProblemDetailExceptionHandlerTests {

	private final StaticMessageSource messageSource = new StaticMessageSource();

	private final ProblemDetailExceptionHandler handler = new ProblemDetailExceptionHandler(this.messageSource);


	@Test
	void handlerMethodIsAnnotated() throws Exception {
		Method method = ProblemDetailExceptionHandler.class.getMethod("handleException", Exception.class, Locale.class);
		assertThat(AnnotatedElementUtils.hasAnnotation(method, ExceptionHandler.class)).isTrue();
	}

	@Test
	void errorResponseIsUsedAsIs() {
		ErrorResponseException ex = new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE);
		ex.getHeaders().set(HttpHeaders.RETRY_AFTER, "60");

		ErrorResponse response = handle(ex);
		assertThat(response).isSameAs(ex);
		assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
	}

	@Test
	void errorResponseWithLocalizedDetail() {
		this.messageSource.addMessage(
				ErrorResponse.getDefaultDetailMessageCode(ErrorResponseException.class, null), Locale.FRENCH, "Indisponible");

		ProblemDetail body = handle(new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE), Locale.FRENCH).getBody();
		assertThat(body.getDetail()).isEqualTo("Indisponible");
	}

	@Test
	void responseStatusException() {
		ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "No such order");

		ErrorResponse response = handle(ex);
		assertThat(response).isSameAs(ex);
		assertThat(response.getBody().getDetail()).isEqualTo("No such order");
	}

	@Test
	void responseStatusExceptionAsCause() {
		ResponseStatusException cause = new ResponseStatusException(HttpStatus.CONFLICT, "Order already placed");

		ErrorResponse response = handle(new IllegalStateException("wrapper", cause));
		assertThat(response).isSameAs(cause);
	}

	@Test
	void responseStatusAnnotation() {
		ProblemDetail body = handle(new GoneException()).getBody();
		assertThat(body.getStatus()).isEqualTo(410);
		assertThat(body.getDetail()).isEqualTo("Resource is gone");
	}

	@Test
	void responseStatusAnnotationWithLocalizedReason() {
		this.messageSource.addMessage("gone.reason", Locale.FRENCH, "La ressource n'existe plus");

		ProblemDetail body = handle(new LocalizedGoneException(), Locale.FRENCH).getBody();
		assertThat(body.getStatus()).isEqualTo(410);
		assertThat(body.getDetail()).isEqualTo("La ressource n'existe plus");
	}

	@Test
	void responseStatusAnnotationWithoutReason() {
		ProblemDetail body = handle(new ServerErrorException()).getBody();
		assertThat(body.getStatus()).isEqualTo(500);
		assertThat(body.getDetail()).isNull();
	}

	@Test
	void responseStatusAnnotationOnCause() {
		ProblemDetail body = handle(new IllegalStateException("wrapper", new GoneException())).getBody();
		assertThat(body.getStatus()).isEqualTo(410);
		assertThat(body.getDetail()).isEqualTo("Resource is gone");
	}

	@Test
	void responseStatusAnnotationOnCauseHasPrecedenceOverTypeMismatch() {
		TypeMismatchException ex = new TypeMismatchException("value", Integer.class, new GoneException());

		ProblemDetail body = handle(ex).getBody();
		assertThat(body.getStatus()).isEqualTo(410);
	}

	@Test
	void typeMismatch() {
		MethodArgumentTypeMismatchException ex =
				new MethodArgumentTypeMismatchException("abc", Integer.class, "id", null, null);

		ProblemDetail body = handle(ex).getBody();
		assertThat(body.getStatus()).isEqualTo(400);
		assertThat(body.getDetail()).isEqualTo("Failed to convert 'id' with value: 'abc'");
	}

	@Test
	void typeMismatchWithLocalizedDetail() {
		this.messageSource.addMessage(ErrorResponse.getDefaultDetailMessageCode(TypeMismatchException.class, null),
				Locale.FRENCH, "Conversion de ''{0}'' en {2} impossible");
		MethodArgumentTypeMismatchException ex =
				new MethodArgumentTypeMismatchException("abc", Integer.class, "id", null, null);

		ProblemDetail body = handle(ex, Locale.FRENCH).getBody();
		assertThat(body.getDetail()).isEqualTo("Conversion de 'id' en Integer impossible");
	}

	@Test
	void conversionNotSupported() {
		ProblemDetail body = handle(new ConversionNotSupportedException(new Object(), String.class, null)).getBody();
		assertThat(body.getStatus()).isEqualTo(500);
		assertThat(body.getDetail()).startsWith("Failed to convert");
	}

	@Test
	void httpMessageNotReadable() {
		HttpMessageNotReadableException ex =
				new HttpMessageNotReadableException("Invalid JSON", new MockHttpInputMessage(new byte[0]));

		ProblemDetail body = handle(ex).getBody();
		assertThat(body.getStatus()).isEqualTo(400);
		assertThat(body.getDetail()).isEqualTo("Failed to read request");
	}

	@Test
	void httpMessageNotWritable() {
		ProblemDetail body = handle(new HttpMessageNotWritableException("Serialization failed")).getBody();
		assertThat(body.getStatus()).isEqualTo(500);
		assertThat(body.getDetail()).isEqualTo("Failed to write request");
	}

	@Test
	void methodValidation() {
		ProblemDetail body = handle(new MethodValidationException(mock(MethodValidationResult.class))).getBody();
		assertThat(body.getStatus()).isEqualTo(500);
		assertThat(body.getDetail()).isEqualTo("Validation failed");
	}

	@Test
	void asyncRequestNotUsable() {
		assertThat(this.handler.handleException(new AsyncRequestNotUsableException("Response not usable"), Locale.ENGLISH))
				.isNull();
	}

	@Test
	void disconnectedClient() {
		ProblemDetail body = handle(new IOException("Broken pipe")).getBody();
		assertThat(body.getStatus()).isEqualTo(500);
		assertThat(body.getDetail()).isNull();
	}

	@Test
	void unknownExceptionDoesNotExposeMessage() {
		ProblemDetail body = handle(new IllegalStateException("Secret internal state")).getBody();
		assertThat(body.getStatus()).isEqualTo(500);
		assertThat(body.getDetail()).isNull();
		assertThat(body.getTitle()).isEqualTo("Internal Server Error");
	}

	@Test
	void unknownExceptionWithLocalizedDetail() {
		this.messageSource.addMessage(ErrorResponse.getDefaultDetailMessageCode(IllegalStateException.class, null),
				Locale.FRENCH, "Erreur interne");

		ProblemDetail body = handle(new IllegalStateException("Secret internal state"), Locale.FRENCH).getBody();
		assertThat(body.getDetail()).isEqualTo("Erreur interne");
	}

	@Test
	void withoutMessageSource() {
		ProblemDetailExceptionHandler handler = new ProblemDetailExceptionHandler(null);

		ErrorResponse response = handler.handleException(new LocalizedGoneException(), Locale.ENGLISH);
		assertThat(response).isNotNull();
		assertThat(response.getBody().getDetail()).isEqualTo("gone.reason");
	}


	private ErrorResponse handle(Exception ex) {
		return handle(ex, Locale.ENGLISH);
	}

	private ErrorResponse handle(Exception ex, Locale locale) {
		ErrorResponse response = this.handler.handleException(ex, locale);
		assertThat(response).isNotNull();
		assertThat(response.getBody().getStatus()).isEqualTo(response.getStatusCode().value());
		return response;
	}


	@ResponseStatus(code = HttpStatus.GONE, reason = "Resource is gone")
	@SuppressWarnings("serial")
	private static class GoneException extends RuntimeException {
	}

	@ResponseStatus(code = HttpStatus.GONE, reason = "gone.reason")
	@SuppressWarnings("serial")
	private static class LocalizedGoneException extends RuntimeException {
	}

	@ResponseStatus
	@SuppressWarnings("serial")
	private static class ServerErrorException extends RuntimeException {
	}

}
