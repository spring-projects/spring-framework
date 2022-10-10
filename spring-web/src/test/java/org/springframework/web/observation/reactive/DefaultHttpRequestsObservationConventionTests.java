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

package org.springframework.web.observation.reactive;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultHttpRequestsObservationConvention}.
 * @author Brian Clozel
 */
class DefaultHttpRequestsObservationConventionTests {

	private final DefaultHttpRequestsObservationConvention convention = new DefaultHttpRequestsObservationConvention();


	@Test
	void shouldHaveDefaultName() {
		assertThat(convention.getName()).isEqualTo("http.server.requests");
	}

	@Test
	void shouldHaveContextualName() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test/resource"));
		HttpRequestsObservationContext context = new HttpRequestsObservationContext(exchange);
		assertThat(convention.getContextualName(context)).isEqualTo("http get");
	}

	@Test
	void supportsOnlyHttpRequestsObservationContext() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/test/resource"));
		HttpRequestsObservationContext context = new HttpRequestsObservationContext(exchange);
		assertThat(this.convention.supportsContext(context)).isTrue();
		assertThat(this.convention.supportsContext(new Observation.Context())).isFalse();
	}

	@Test
	void addsKeyValuesForExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/test/resource"));
		exchange.getResponse().setRawStatusCode(201);
		HttpRequestsObservationContext context = new HttpRequestsObservationContext(exchange);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(5)
				.contains(KeyValue.of("method", "POST"), KeyValue.of("uri", "UNKNOWN"), KeyValue.of("status", "201"),
						KeyValue.of("exception", "none"), KeyValue.of("outcome", "SUCCESS"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(1)
				.contains(KeyValue.of("http.url", "/test/resource"));
	}

	@Test
	void addsKeyValuesForExchangeWithPathPattern() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test/resource"));
		exchange.getResponse().setRawStatusCode(200);
		HttpRequestsObservationContext context = new HttpRequestsObservationContext(exchange);
		PathPattern pathPattern = getPathPattern("/test/{name}");
		context.setPathPattern(pathPattern);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(5)
				.contains(KeyValue.of("method", "GET"), KeyValue.of("uri", "/test/{name}"), KeyValue.of("status", "200"),
						KeyValue.of("exception", "none"), KeyValue.of("outcome", "SUCCESS"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(1)
				.contains(KeyValue.of("http.url", "/test/resource"));
	}


	@Test
	void addsKeyValuesForErrorExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test/resource"));
		HttpRequestsObservationContext context = new HttpRequestsObservationContext(exchange);
		context.setError(new IllegalArgumentException("custom error"));
		exchange.getResponse().setRawStatusCode(500);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(5)
				.contains(KeyValue.of("method", "GET"), KeyValue.of("uri", "UNKNOWN"), KeyValue.of("status", "500"),
						KeyValue.of("exception", "IllegalArgumentException"), KeyValue.of("outcome", "SERVER_ERROR"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(1)
				.contains(KeyValue.of("http.url", "/test/resource"));
	}

	@Test
	void addsKeyValuesForRedirectExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test/redirect"));
		HttpRequestsObservationContext context = new HttpRequestsObservationContext(exchange);
		exchange.getResponse().setRawStatusCode(302);
		exchange.getResponse().getHeaders().add("Location", "https://example.org/other");

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(5)
				.contains(KeyValue.of("method", "GET"), KeyValue.of("uri", "REDIRECTION"), KeyValue.of("status", "302"),
						KeyValue.of("exception", "none"), KeyValue.of("outcome", "REDIRECTION"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(1)
				.contains(KeyValue.of("http.url", "/test/redirect"));
	}

	@Test
	void addsKeyValuesForNotFoundExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test/notFound"));
		HttpRequestsObservationContext context = new HttpRequestsObservationContext(exchange);
		exchange.getResponse().setRawStatusCode(404);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(5)
				.contains(KeyValue.of("method", "GET"), KeyValue.of("uri", "NOT_FOUND"), KeyValue.of("status", "404"),
						KeyValue.of("exception", "none"), KeyValue.of("outcome", "CLIENT_ERROR"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(1)
				.contains(KeyValue.of("http.url", "/test/notFound"));
	}

	@Test
	void addsKeyValuesForCancelledExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test/resource"));
		HttpRequestsObservationContext context = new HttpRequestsObservationContext(exchange);
		context.setConnectionAborted(true);
		exchange.getResponse().setRawStatusCode(200);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(5)
				.contains(KeyValue.of("method", "GET"), KeyValue.of("uri", "UNKNOWN"), KeyValue.of("status", "UNKNOWN"),
						KeyValue.of("exception", "none"), KeyValue.of("outcome", "UNKNOWN"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(1)
				.contains(KeyValue.of("http.url", "/test/resource"));
	}

	private static PathPattern getPathPattern(String pattern) {
		PathPatternParser pathPatternParser = new PathPatternParser();
		return pathPatternParser.parse(pattern);
	}

}
