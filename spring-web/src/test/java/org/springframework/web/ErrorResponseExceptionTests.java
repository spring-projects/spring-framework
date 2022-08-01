/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingMatrixVariableException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.UnsatisfiedRequestParameterException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.testfixture.method.ResolvableMethod;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests that verify the HTTP response details exposed by exceptions in the
 * {@link ErrorResponse} hierarchy.
 *
 * @author Rossen Stoyanchev
 */
public class ErrorResponseExceptionTests {

	private final MethodParameter methodParameter =
			new MethodParameter(ResolvableMethod.on(getClass()).resolveMethod("handle"), 0);


	@Test
	void httpMediaTypeNotSupportedException() {

		List<MediaType> mediaTypes =
				Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_CBOR);

		ErrorResponse ex = new HttpMediaTypeNotSupportedException(
				MediaType.APPLICATION_XML, mediaTypes, HttpMethod.PATCH, "Custom message");


		assertStatus(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		assertDetail(ex, "Content-Type 'application/xml' is not supported.");

		HttpHeaders headers = ex.getHeaders();
		assertThat(headers.getAccept()).isEqualTo(mediaTypes);
		assertThat(headers.getAcceptPatch()).isEqualTo(mediaTypes);
	}

	@Test
	void httpMediaTypeNotSupportedExceptionWithParseError() {

		ErrorResponse ex = new HttpMediaTypeNotSupportedException(
				"Could not parse Accept header: Invalid mime type \"foo\": does not contain '/'");


		assertStatus(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		assertDetail(ex, "Could not parse Content-Type.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void httpMediaTypeNotAcceptableException() {

		List<MediaType> mediaTypes = Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_CBOR);
		ErrorResponse ex = new HttpMediaTypeNotAcceptableException(mediaTypes);


		assertStatus(ex, HttpStatus.NOT_ACCEPTABLE);
		assertDetail(ex, "Acceptable representations: 'application/json, application/cbor'.");

		assertThat(ex.getHeaders()).hasSize(1);
		assertThat(ex.getHeaders().getAccept()).isEqualTo(mediaTypes);
	}

	@Test
	void httpMediaTypeNotAcceptableExceptionWithParseError() {

		ErrorResponse ex = new HttpMediaTypeNotAcceptableException(
				"Could not parse Accept header: Invalid mime type \"foo\": does not contain '/'");


		assertStatus(ex, HttpStatus.NOT_ACCEPTABLE);
		assertDetail(ex, "Could not parse Accept header.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void asyncRequestTimeoutException() {

		ErrorResponse ex = new AsyncRequestTimeoutException();


		assertStatus(ex, HttpStatus.SERVICE_UNAVAILABLE);
		assertDetail(ex, null);
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void httpRequestMethodNotSupportedException() {

		String[] supportedMethods = new String[] { "GET", "POST" };
		ErrorResponse ex = new HttpRequestMethodNotSupportedException("PUT", supportedMethods, "Custom message");


		assertStatus(ex, HttpStatus.METHOD_NOT_ALLOWED);
		assertDetail(ex, "Method 'PUT' is not supported.");

		assertThat(ex.getHeaders()).hasSize(1);
		assertThat(ex.getHeaders().getAllow()).containsExactly(HttpMethod.GET, HttpMethod.POST);
	}

	@Test
	void missingRequestHeaderException() {

		ErrorResponse ex = new MissingRequestHeaderException("Authorization", this.methodParameter);


		assertStatus(ex, HttpStatus.BAD_REQUEST);
		assertDetail(ex, "Required header 'Authorization' is not present.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void missingServletRequestParameterException() {

		ErrorResponse ex = new MissingServletRequestParameterException("query", "String");


		assertStatus(ex, HttpStatus.BAD_REQUEST);
		assertDetail(ex, "Required parameter 'query' is not present.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void missingMatrixVariableException() {

		ErrorResponse ex = new MissingMatrixVariableException("region", this.methodParameter);


		assertStatus(ex, HttpStatus.BAD_REQUEST);
		assertDetail(ex, "Required path parameter 'region' is not present.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void missingPathVariableException() {

		ErrorResponse ex = new MissingPathVariableException("id", this.methodParameter);


		assertStatus(ex, HttpStatus.INTERNAL_SERVER_ERROR);
		assertDetail(ex, "Required path variable 'id' is not present.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void missingRequestCookieException() {

		ErrorResponse ex = new MissingRequestCookieException("oreo", this.methodParameter);


		assertStatus(ex, HttpStatus.BAD_REQUEST);
		assertDetail(ex, "Required cookie 'oreo' is not present.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void unsatisfiedServletRequestParameterException() {

		ErrorResponse ex = new UnsatisfiedServletRequestParameterException(
				new String[] { "foo=bar", "bar=baz" }, Collections.singletonMap("q", new String[] {"1"}));


		assertStatus(ex, HttpStatus.BAD_REQUEST);
		assertDetail(ex, "Invalid request parameters.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void missingServletRequestPartException() {

		ErrorResponse ex = new MissingServletRequestPartException("file");

		assertStatus(ex, HttpStatus.BAD_REQUEST);
		assertDetail(ex, "Required part 'file' is not present.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void methodArgumentNotValidException() {

		BindingResult bindingResult = new BindException(new Object(), "object");
		bindingResult.addError(new FieldError("object", "field", "message"));

		ErrorResponse ex = new MethodArgumentNotValidException(this.methodParameter, bindingResult);

		assertStatus(ex, HttpStatus.BAD_REQUEST);
		assertDetail(ex, "Invalid request content.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void unsupportedMediaTypeStatusException() {

		List<MediaType> mediaTypes =
				Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_CBOR);

		ErrorResponse ex = new UnsupportedMediaTypeStatusException(
				MediaType.APPLICATION_XML, mediaTypes, HttpMethod.PATCH);

		assertStatus(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		assertDetail(ex, "Content-Type 'application/xml' is not supported.");

		HttpHeaders headers = ex.getHeaders();
		assertThat(headers.getAccept()).isEqualTo(mediaTypes);
		assertThat(headers.getAcceptPatch()).isEqualTo(mediaTypes);
	}

	@Test
	void unsupportedMediaTypeStatusExceptionWithParseError() {

		ErrorResponse ex = new UnsupportedMediaTypeStatusException(
				"Could not parse Accept header: Invalid mime type \"foo\": does not contain '/'");

		assertStatus(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		assertDetail(ex, "Could not parse Content-Type.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void notAcceptableStatusException() {

		List<MediaType> mediaTypes =
				Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_CBOR);

		ErrorResponse ex = new NotAcceptableStatusException(mediaTypes);

		assertStatus(ex, HttpStatus.NOT_ACCEPTABLE);
		assertDetail(ex, "Acceptable representations: 'application/json, application/cbor'.");

		assertThat(ex.getHeaders()).hasSize(1);
		assertThat(ex.getHeaders().getAccept()).isEqualTo(mediaTypes);
	}

	@Test
	void notAcceptableStatusExceptionWithParseError() {

		ErrorResponse ex = new NotAcceptableStatusException(
				"Could not parse Accept header: Invalid mime type \"foo\": does not contain '/'");


		assertStatus(ex, HttpStatus.NOT_ACCEPTABLE);
		assertDetail(ex, "Could not parse Accept header.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void missingRequestValueException() {

		ErrorResponse ex = new MissingRequestValueException(
				"foo", String.class, "header", this.methodParameter);

		assertStatus(ex, HttpStatus.BAD_REQUEST);
		assertDetail(ex, "Required header 'foo' is not present.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void unsatisfiedRequestParameterException() {

		ErrorResponse ex = new UnsatisfiedRequestParameterException(
				Arrays.asList("foo=bar", "bar=baz"),
				new LinkedMultiValueMap<>(Collections.singletonMap("q", Arrays.asList("1", "2"))));

		assertStatus(ex, HttpStatus.BAD_REQUEST);
		assertDetail(ex, "Invalid request parameters.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void webExchangeBindException() {

		BindingResult bindingResult = new BindException(new Object(), "object");
		bindingResult.addError(new FieldError("object", "field", "message"));

		ErrorResponse ex = new WebExchangeBindException(this.methodParameter, bindingResult);

		assertStatus(ex, HttpStatus.BAD_REQUEST);
		assertDetail(ex, "Invalid request content.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	@Test
	void methodNotAllowedException() {

		List<HttpMethod> supportedMethods = Arrays.asList(HttpMethod.GET, HttpMethod.POST);
		ErrorResponse ex = new MethodNotAllowedException(HttpMethod.PUT, supportedMethods);


		assertStatus(ex, HttpStatus.METHOD_NOT_ALLOWED);
		assertDetail(ex, "Supported methods: 'GET', 'POST'");

		assertThat(ex.getHeaders()).hasSize(1);
		assertThat(ex.getHeaders().getAllow()).containsExactly(HttpMethod.GET, HttpMethod.POST);
	}

	@Test
	void methodNotAllowedExceptionWithoutSupportedMethods() {

		ErrorResponse ex = new MethodNotAllowedException(HttpMethod.PUT, Collections.emptyList());


		assertStatus(ex, HttpStatus.METHOD_NOT_ALLOWED);
		assertDetail(ex, "Request method 'PUT' is not supported.");
		assertThat(ex.getHeaders()).isEmpty();
	}

	private void assertStatus(ErrorResponse ex, HttpStatus status) {
		ProblemDetail body = ex.getBody();
		assertThat(ex.getStatusCode()).isEqualTo(status);
		assertThat(body.getStatus()).isEqualTo(status.value());
		assertThat(body.getTitle()).isEqualTo(status.getReasonPhrase());
	}

	private void assertDetail(ErrorResponse ex, @Nullable String detail) {
		if (detail != null) {
			assertThat(ex.getBody().getDetail()).isEqualTo(detail);
		}
		else {
			assertThat(ex.getBody().getDetail()).isNull();
		}
	}


	@SuppressWarnings("unused")
	private void handle(String arg) {}

}
