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

package org.springframework.http.client.observation;


import java.io.IOException;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.testfixture.http.client.MockClientHttpRequest;
import org.springframework.web.testfixture.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultClientRequestObservationConvention}.
 *
 * @author Brian Clozel
 */
class DefaultClientRequestObservationConventionTests {

	private final MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/test");

	private final DefaultClientRequestObservationConvention observationConvention = new DefaultClientRequestObservationConvention();

	@Test
	void shouldHaveName() {
		assertThat(this.observationConvention.getName()).isEqualTo("http.client.requests");
	}

	@Test
	void shouldHaveContextualName() {
		ClientRequestObservationContext context = new ClientRequestObservationContext(this.request);
		assertThat(this.observationConvention.getContextualName(context)).isEqualTo("http get");
	}

	@Test
	void supportsOnlyClientHttpObservationContext() {
		ClientRequestObservationContext context = new ClientRequestObservationContext(this.request);
		assertThat(this.observationConvention.supportsContext(context)).isTrue();
		assertThat(this.observationConvention.supportsContext(new Observation.Context())).isFalse();
	}

	@Test
	void addsKeyValuesForRequestWithUriTemplate() {
		ClientRequestObservationContext context = createContext(
				new MockClientHttpRequest(HttpMethod.GET, "/resource/{id}", 42), new MockClientHttpResponse());
		context.setUriTemplate("/resource/{id}");
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context))
				.contains(KeyValue.of("exception", "none"), KeyValue.of("method", "GET"), KeyValue.of("uri", "/resource/{id}"),
						KeyValue.of("status", "200"), KeyValue.of("client.name", "none"), KeyValue.of("outcome", "SUCCESS"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).contains(KeyValue.of("http.url", "/resource/42"));
	}

	@Test
	void addsKeyValuesForRequestWithUriTemplateWithHost() {
		ClientRequestObservationContext context = createContext(
				new MockClientHttpRequest(HttpMethod.GET, "https://example.org/resource/{id}", 42), new MockClientHttpResponse());
		context.setUriTemplate("https://example.org/resource/{id}");
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context))
				.contains(KeyValue.of("exception", "none"), KeyValue.of("method", "GET"), KeyValue.of("uri", "/resource/{id}"),
						KeyValue.of("status", "200"), KeyValue.of("client.name", "example.org"), KeyValue.of("outcome", "SUCCESS"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).contains(KeyValue.of("http.url", "https://example.org/resource/42"));
	}


	@Test
	void addsKeyValuesForRequestWithoutUriTemplate() {
		ClientRequestObservationContext context = createContext(
				new MockClientHttpRequest(HttpMethod.GET, "/resource/42"), new MockClientHttpResponse());
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context))
				.contains(KeyValue.of("method", "GET"), KeyValue.of("client.name", "none"), KeyValue.of("uri", "none"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).contains(KeyValue.of("http.url", "/resource/42"));
	}

	@Test
	void addsClientNameForRequestWithHost() {
		ClientRequestObservationContext context = createContext(
				new MockClientHttpRequest(HttpMethod.GET, "https://localhost:8080/resource/42"),
				new MockClientHttpResponse());
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("client.name", "localhost"));
	}

	@Test
	void addsKeyValueForNonResolvableStatus() throws Exception {
		ClientRequestObservationContext context = new ClientRequestObservationContext(this.request);
		ClientHttpResponse response = mock();
		context.setResponse(response);
		given(response.getStatusCode()).willThrow(new IOException("test error"));
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("status", "IO_ERROR"));
	}

	private ClientRequestObservationContext createContext(ClientHttpRequest request, ClientHttpResponse response) {
		ClientRequestObservationContext context = new ClientRequestObservationContext(request);
		context.setResponse(response);
		return context;
	}

}
