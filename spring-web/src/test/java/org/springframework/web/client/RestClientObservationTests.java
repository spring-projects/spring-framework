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
import java.util.Map;
import java.util.UUID;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.http.converter.HttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

/**
 * Tests for the client HTTP observations with {@link RestClient}.
 * @author Brian Clozel
 */
class RestClientObservationTests {


	private final TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	private final ClientHttpRequestFactory requestFactory = mock();

	private final ClientHttpRequest request = mock();

	private final ClientHttpResponse response = mock();

	private final ResponseErrorHandler errorHandler = mock();

	@SuppressWarnings("unchecked")
	private final HttpMessageConverter<String> converter = mock();

	private RestClient client;


	@BeforeEach
	void setupEach() {

		this.client = RestClient.builder()
				.messageConverters(converters -> converters.add(0, this.converter))
				.requestFactory(this.requestFactory)
				.defaultStatusHandler(this.errorHandler)
				.observationRegistry(this.observationRegistry)
				.build();
		this.observationRegistry.observationConfig().observationHandler(new ContextAssertionObservationHandler());
	}

	@Test
	void executeVarArgsAddsUriTemplateAsKeyValue() throws Exception {
		mockSentRequest(GET, "https://example.com/hotels/42/bookings/21");
		mockResponseStatus(HttpStatus.OK);

		client.get().uri("https://example.com/hotels/{hotel}/bookings/{booking}", "42", "21")
				.retrieve().toBodilessEntity();

		assertThatHttpObservation().hasLowCardinalityKeyValue("uri", "/hotels/{hotel}/bookings/{booking}");
	}

	@Test
	void executeArgsMapAddsUriTemplateAsKeyValue() throws Exception {
		mockSentRequest(GET, "https://example.com/hotels/42/bookings/21");
		mockResponseStatus(HttpStatus.OK);

		Map<String, String> vars = Map.of("hotel", "42", "booking", "21");

		client.get().uri("https://example.com/hotels/{hotel}/bookings/{booking}", vars)
				.retrieve().toBodilessEntity();

		assertThatHttpObservation().hasLowCardinalityKeyValue("uri", "/hotels/{hotel}/bookings/{booking}");
	}


	@Test
	void executeAddsSuccessAsOutcome() throws Exception {
		mockSentRequest(GET, "https://example.org");
		mockResponseStatus(HttpStatus.OK);
		mockResponseBody("Hello World", MediaType.TEXT_PLAIN);

		client.get().uri("https://example.org").retrieve().toBodilessEntity();

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
				client.get().uri(url).retrieve().toBodilessEntity());

		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SERVER_ERROR");
	}

	@Test
	void executeAddsExceptionAsKeyValue() throws Exception {
		mockSentRequest(POST, "https://example.org/resource");
		mockResponseStatus(HttpStatus.OK);

		MediaType other = new MediaType("test", "other");
		mockResponseBody("Test Body", other);

		assertThatExceptionOfType(RestClientException.class).isThrownBy(() ->
				client.post().uri("https://example.org/{p}", "resource")
						.contentType(other)
						.body(UUID.randomUUID())
						.retrieve().toBodilessEntity());
		assertThatHttpObservation().hasLowCardinalityKeyValue("exception", "RestClientException");
	}

	@Test
	void executeWithIoExceptionAddsUnknownOutcome() throws Exception {
		String url = "https://example.org/resource";
		mockSentRequest(GET, url);
		given(request.execute()).willThrow(new IOException("Socket failure"));

		assertThatExceptionOfType(ResourceAccessException.class).isThrownBy(() ->
						client.get().uri(url).retrieve().body(String.class));
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "UNKNOWN");
	}

	@Test
	void executeWithCustomConventionUsesCustomObservationName() throws Exception {
		ClientRequestObservationConvention observationConvention =
				new DefaultClientRequestObservationConvention("custom.requests");
		RestClient restClient = this.client.mutate().observationConvention(observationConvention).build();
		mockSentRequest(GET, "https://example.org");
		mockResponseStatus(HttpStatus.OK);

		restClient.get().uri("https://example.org").retrieve().toBodilessEntity();

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.hasObservationWithNameEqualTo("custom.requests");
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
