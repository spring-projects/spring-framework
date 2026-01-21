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

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultRestClient}.
 *
 * @author Sebastien Deleuze
 */
class DefaultRestClientTests {

	private final ClientHttpRequestFactory requestFactory = mock();

	private final ClientHttpRequest request = mock();

	private final ClientHttpResponse response = mock();

	private RestClient client;


	@BeforeEach
	void setup() {
		this.client = RestClient.builder()
				.requestFactory(this.requestFactory)
				.build();
	}


	@Test
	void requiredBodyWithClass() throws IOException {
		mockSentRequest(HttpMethod.GET, "https://example.org");
		mockResponseStatus(HttpStatus.OK);
		mockResponseBody("Hello World", MediaType.TEXT_PLAIN);

		String result = this.client.get()
				.uri("https://example.org")
				.retrieve()
				.requiredBody(String.class);

		assertThat(result).isEqualTo("Hello World");
	}

	@Test
	void requiredBodyWithClassAndNullBody() throws IOException {
		mockSentRequest(HttpMethod.GET, "https://example.org");
		mockResponseStatus(HttpStatus.OK);
		mockEmptyResponseBody();

		assertThatIllegalStateException().isThrownBy(() ->
				this.client.get()
						.uri("https://example.org")
						.retrieve()
						.requiredBody(String.class)
		);
	}

	@Test
	void requiredBodyWithParameterizedTypeReference() throws IOException {
		mockSentRequest(HttpMethod.GET, "https://example.org");
		mockResponseStatus(HttpStatus.OK);
		mockResponseBody("Hello World", MediaType.TEXT_PLAIN);

		String result = this.client.get()
				.uri("https://example.org")
				.retrieve()
				.requiredBody(new ParameterizedTypeReference<>() {});

		assertThat(result).isEqualTo("Hello World");
	}

	@Test
	void requiredBodyWithParameterizedTypeReferenceAndNullBody() throws IOException {
		mockSentRequest(HttpMethod.GET, "https://example.org");
		mockResponseStatus(HttpStatus.OK);
		mockEmptyResponseBody();

		assertThatIllegalStateException().isThrownBy(() ->
				this.client.get()
						.uri("https://example.org")
						.retrieve()
						.requiredBody(new ParameterizedTypeReference<String>() {})
		);
	}


	private void mockSentRequest(HttpMethod method, String uri) throws IOException {
		given(this.requestFactory.createRequest(URI.create(uri), method)).willReturn(this.request);
		given(this.request.getHeaders()).willReturn(new HttpHeaders());
		given(this.request.getMethod()).willReturn(method);
		given(this.request.getURI()).willReturn(URI.create(uri));
	}

	private void mockResponseStatus(HttpStatus responseStatus) throws IOException {
		given(this.request.execute()).willReturn(this.response);
		given(this.response.getStatusCode()).willReturn(responseStatus);
		given(this.response.getStatusText()).willReturn(responseStatus.getReasonPhrase());
	}

	private void mockResponseBody(String expectedBody, MediaType mediaType) throws IOException {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(mediaType);
		responseHeaders.setContentLength(expectedBody.length());
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(expectedBody.getBytes()));
	}

	private void mockEmptyResponseBody() throws IOException {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentLength(0);
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(new byte[0]));
	}

}
