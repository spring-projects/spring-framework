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

package org.springframework.web.reactive.function.client;

import java.net.URI;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultClientRequestObservationConvention}.
 *
 * @author Brian Clozel
 */
class DefaultClientRequestObservationConventionTests {

	private final DefaultClientRequestObservationConvention observationConvention = new DefaultClientRequestObservationConvention();

	@Test
	void shouldHaveName() {
		assertThat(this.observationConvention.getName()).isEqualTo("http.client.requests");
	}

	@Test
	void shouldHaveContextualName() {
		ClientRequestObservationContext context = new ClientRequestObservationContext(ClientRequest.create(HttpMethod.GET, URI.create("/test")));
		assertThat(this.observationConvention.getContextualName(context)).isEqualTo("http get");
	}

	@Test
	void shouldOnlySupportWebClientObservationContext() {
		ClientRequest.Builder request = ClientRequest.create(HttpMethod.GET, URI.create("/test"));
		assertThat(this.observationConvention.supportsContext(new ClientRequestObservationContext(request))).isTrue();
		assertThat(this.observationConvention.supportsContext(new Observation.Context())).isFalse();
	}

	@Test
	@SuppressWarnings("removal")
	void shouldAddKeyValuesForNullExchange() {
		ClientRequestObservationContext context = new ClientRequestObservationContext();
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("method", "none"), KeyValue.of("uri", "none"), KeyValue.of("status", "CLIENT_ERROR"),
						KeyValue.of("client.name", "none"),
						KeyValue.of("exception", "none"), KeyValue.of("outcome", "UNKNOWN"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).hasSize(1)
				.contains(KeyValue.of("http.url", "none"));
	}

	@Test
	void shouldAddKeyValuesForExchangeWithException() {
		ClientRequest.Builder request = ClientRequest.create(HttpMethod.GET, URI.create("/test"));
		ClientRequestObservationContext context = new ClientRequestObservationContext(request);
		context.setError(new IllegalStateException("Could not create client request"));
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("method", "GET"), KeyValue.of("uri", "none"), KeyValue.of("status", "CLIENT_ERROR"),
						KeyValue.of("client.name", "none"), KeyValue.of("exception", "IllegalStateException"), KeyValue.of("outcome", "UNKNOWN"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).hasSize(1)
				.contains(KeyValue.of("http.url", "/test"));
	}

	@Test
	void shouldAddKeyValuesForRequestWithUriTemplate() {
		ClientRequest.Builder request = ClientRequest.create(HttpMethod.GET, URI.create("/resource/42"))
				.attribute(WebClient.class.getName() + ".uriTemplate", "/resource/{id}");
		ClientRequestObservationContext context = createContext(request);
		context.setUriTemplate("/resource/{id}");
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context))
				.contains(KeyValue.of("exception", "none"), KeyValue.of("method", "GET"), KeyValue.of("uri", "/resource/{id}"),
						KeyValue.of("status", "200"), KeyValue.of("client.name", "none"), KeyValue.of("outcome", "SUCCESS"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).hasSize(1)
				.contains(KeyValue.of("http.url", "/resource/42"));
	}

	@Test
	void shouldAddKeyValuesForRequestWithoutUriTemplate() {
		ClientRequestObservationContext context = createContext(ClientRequest.create(HttpMethod.GET, URI.create("/resource/42")));
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context))
				.contains(KeyValue.of("method", "GET"), KeyValue.of("uri", "none"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).hasSize(1).contains(KeyValue.of("http.url", "/resource/42"));
	}

	@Test
	void shouldAddClientNameKeyValueForRequestWithHost() {
		ClientRequestObservationContext context = createContext(ClientRequest.create(HttpMethod.GET, URI.create("https://localhost:8080/resource/42")));
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("client.name", "localhost"));
	}

	@Test
	void shouldAddRootUriEvenIfTemplateMissing() {
		ClientRequestObservationContext context = createContext(ClientRequest.create(HttpMethod.GET, URI.create("https://example.org/")));
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("uri", "/"));
	}

	@Test
	void shouldOnlyConsiderPathForUriKeyValue() {
		ClientRequestObservationContext context = createContext(ClientRequest.create(HttpMethod.GET, URI.create("https://example.org/resource/42")));
		context.setUriTemplate("https://example.org/resource/{id}");
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("uri", "/resource/{id}"));
	}

	private ClientRequestObservationContext createContext(ClientRequest.Builder request) {
		ClientRequestObservationContext context = new ClientRequestObservationContext(request);
		context.setResponse(ClientResponse.create(HttpStatus.OK).build());
		return context;
	}

}
