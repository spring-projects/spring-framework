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

package org.springframework.http.server.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTelemetryServerRequestObservationConvention}.
 * @author Brian Clozel
 * @author Tommy Ludwig
 */
class OpenTelemetryServerRequestObservationConventionTests {

	private final OpenTelemetryServerRequestObservationConvention convention = new OpenTelemetryServerRequestObservationConvention();

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/resource");

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final ServerRequestObservationContext context = new ServerRequestObservationContext(this.request, this.response);


	@Test
	void shouldHaveName() {
		assertThat(convention.getName()).isEqualTo("http.server.request.duration");
	}

	@Test
	void shouldHaveContextualName() {
		assertThat(convention.getContextualName(this.context)).isEqualTo("GET");
	}

	@Test
	void contextualNameShouldUsePathPatternWhenAvailable() {
		this.context.setPathPattern("/test/{name}");
		assertThat(convention.getContextualName(this.context)).isEqualTo("GET /test/{name}");
	}

	@Test
	void supportsOnlyHttpRequestsObservationContext() {
		assertThat(this.convention.supportsContext(this.context)).isTrue();
		assertThat(this.convention.supportsContext(new Observation.Context())).isFalse();
	}

	@Test
	void addsKeyValuesForExchange() {
		this.request.setMethod("POST");
		this.request.setRequestURI("/test/resource");

		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "POST"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "200"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "SUCCESS"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(this.context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/resource"), KeyValue.of("http.request.method_original", "POST"));
	}

	@Test
	void addsKeyValuesForExchangeWithPathPattern() {
		this.request.setRequestURI("/test/resource");
		this.context.setPathPattern("/test/{name}");

		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "/test/{name}"), KeyValue.of("http.response.status_code", "200"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "SUCCESS"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(this.context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/resource"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForErrorExchange() {
		this.request.setRequestURI("/test/resource");
		this.context.setError(new IllegalArgumentException("custom error"));
		this.response.setStatus(500);

		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "500"),
						KeyValue.of("error.type", "java.lang.IllegalArgumentException"), KeyValue.of("outcome", "SERVER_ERROR"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(this.context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/resource"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForRedirectExchange() {
		this.request.setRequestURI("/test/redirect");
		this.response.setStatus(302);
		this.response.addHeader("Location", "https://example.org/other");

		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "REDIRECTION"), KeyValue.of("http.response.status_code", "302"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "REDIRECTION"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(this.context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/redirect"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForNotFoundExchange() {
		this.request.setRequestURI("/test/notFound");
		this.response.setStatus(404);

		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "NOT_FOUND"), KeyValue.of("http.response.status_code", "404"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "CLIENT_ERROR"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(this.context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/notFound"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForUnknownHttpMethodExchange() {
		this.request.setMethod("SPRING");
		this.request.setRequestURI("/test");
		this.response.setStatus(404);

		assertThat(this.convention.getContextualName(this.context)).isEqualTo("HTTP");
		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "_OTHER"), KeyValue.of("http.route", "NOT_FOUND"), KeyValue.of("http.response.status_code", "404"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "CLIENT_ERROR"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(this.context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test"), KeyValue.of("http.request.method_original", "SPRING"));
	}

	@Test
	void setsContextualNameWithPathPatternButInvalidMethod() {
		this.request.setMethod("CUSTOM");
		this.context.setPathPattern("/test/{name}");

		assertThat(this.convention.getContextualName(this.context)).isEqualTo("HTTP /test/{name}");
	}

	@Test
	void addsKeyValuesForInvalidStatusExchange() {
		this.request.setRequestURI("/test/invalidStatus");
		this.response.setStatus(0);

		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "0"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "UNKNOWN"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(this.context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/invalidStatus"), KeyValue.of("http.request.method_original", "GET"));
	}

}
