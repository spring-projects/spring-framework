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

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.converter.HttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpMethod.GET;

/**
 * Tests for the client HTTP observations with {@link RestTemplate}.
 * @author Brian Clozel
 */
class RestTemplateObservationTests {


	private final TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	private final ClientHttpRequestFactory requestFactory = mock();

	private final ClientHttpRequest request = mock();

	private final ClientHttpResponse response = mock();

	private final ResponseErrorHandler errorHandler = mock();

	@SuppressWarnings("unchecked")
	private final HttpMessageConverter<String> converter = mock();

	private final RestTemplate template = new RestTemplate(List.of(converter));


	@BeforeEach
	void setupEach() {
		this.template.setRequestFactory(this.requestFactory);
		this.template.setErrorHandler(this.errorHandler);
		this.template.setObservationRegistry(this.observationRegistry);
		this.observationRegistry.observationConfig().observationHandler(new ContextAssertionObservationHandler());
	}

	@Test
	void executeVarArgsAddsUriTemplateAsKeyValue() throws Exception {
		mockSentRequest(GET, "https://example.com/hotels/42/bookings/21");
		mockResponseStatus(HttpStatus.OK);

		template.execute("https://example.com/hotels/{hotel}/bookings/{booking}", GET,
				null, null, "42", "21");

		assertThatHttpObservation().hasLowCardinalityKeyValue("uri", "/hotels/{hotel}/bookings/{booking}");
	}

	@Test
	void executeArgsMapAddsUriTemplateAsKeyValue() throws Exception {
		mockSentRequest(GET, "https://example.com/hotels/42/bookings/21");
		mockResponseStatus(HttpStatus.OK);

		Map<String, String> vars = Map.of("hotel", "42", "booking", "21");

		template.execute("https://example.com/hotels/{hotel}/bookings/{booking}", GET,
				null, null, vars);

		assertThatHttpObservation().hasLowCardinalityKeyValue("uri", "/hotels/{hotel}/bookings/{booking}");
	}


	@Test
	void executeAddsSuccessAsOutcome() throws Exception {
		mockSentRequest(GET, "https://example.org");
		mockResponseStatus(HttpStatus.OK);
		mockResponseBody("Hello World", MediaType.TEXT_PLAIN);

		template.execute("https://example.org", GET, null, null);

		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS");
	}

	@Test
	void executeAddsServerErrorAsOutcome() throws Exception {
		String url = "https://example.org";
		mockSentRequest(GET, url);
		mockResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
				.given(errorHandler).handleError(URI.create(url), GET, response);

		assertThatExceptionOfType(HttpServerErrorException.class).isThrownBy(() ->
				template.execute(url, GET, null, null));

		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SERVER_ERROR");
	}

	@Test
	void executeAddsExceptionAsKeyValue() throws Exception {
		mockSentRequest(GET, "https://example.org/resource");
		mockResponseStatus(HttpStatus.OK);

		given(converter.canRead(String.class, null)).willReturn(true);
		MediaType supportedMediaType = new MediaType("test", "supported");
		given(converter.getSupportedMediaTypes()).willReturn(List.of(supportedMediaType));
		MediaType other = new MediaType("test", "other");
		mockResponseBody("Test Body", other);
		given(converter.canRead(String.class, other)).willReturn(false);

		assertThatExceptionOfType(RestClientException.class).isThrownBy(() ->
				template.getForObject("https://example.org/{p}", String.class, "resource"));
		assertThatHttpObservation().hasLowCardinalityKeyValue("exception", "UnknownContentTypeException");
	}

	@Test
	void executeWithIoExceptionAddsUnknownOutcome() throws Exception {
		String url = "https://example.org/resource";
		mockSentRequest(GET, url);
		given(request.execute()).willThrow(new IOException("Socket failure"));

		assertThatExceptionOfType(ResourceAccessException.class).isThrownBy(() ->
						template.getForObject(url, String.class));
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "UNKNOWN");
	}


	private void mockSentRequest(HttpMethod method, String uri) throws Exception {
		mockSentRequest(method, uri, new HttpHeaders());
	}

	private void mockSentRequest(HttpMethod method, String uri, HttpHeaders requestHeaders) throws Exception {
		given(requestFactory.createRequest(URI.create(uri), method)).willReturn(request);
		given(request.getHeaders()).willReturn(requestHeaders);
		given(request.getMethod()).willReturn(method);
		given(request.getURI()).willReturn(URI.create(uri));
	}

	private void mockResponseStatus(HttpStatus responseStatus) throws Exception {
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(responseStatus.isError());
		given(response.getStatusCode()).willReturn(responseStatus);
		given(response.getStatusText()).willReturn(responseStatus.getReasonPhrase());
	}

	private void mockResponseBody(String expectedBody, MediaType mediaType) throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(mediaType);
		responseHeaders.setContentLength(expectedBody.length());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expectedBody.getBytes()));
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willReturn(expectedBody);
	}


	private TestObservationRegistryAssert.TestObservationRegistryAssertReturningObservationContextAssert assertThatHttpObservation() {
		return TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.hasObservationWithNameEqualTo("http.client.requests").that();
	}

	static class ContextAssertionObservationHandler implements ObservationHandler<ClientRequestObservationContext> {

		@Override
		public boolean supportsContext(Observation.Context context) {
			return context instanceof ClientRequestObservationContext;
		}

		@Override
		public void onStart(ClientRequestObservationContext context) {
			assertThat(context.getCarrier()).isNotNull();
		}
	}

}
