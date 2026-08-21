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

package org.springframework.http.server.reactive.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTelemetryServerRequestObservationConvention}.
 *
 * @author Brian Clozel
 * @author Tommy Ludwig
 */
class OpenTelemetryServerRequestObservationConventionTests {

	private final OpenTelemetryServerRequestObservationConvention convention = new OpenTelemetryServerRequestObservationConvention();

	private final ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/resource"));

	private final ServerRequestObservationContext context = new ServerRequestObservationContext(
			this.exchange.getRequest(), this.exchange.getResponse(), this.exchange.getAttributes());


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
	void setsContextualNameWithPathPatternButInvalidMethod() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.valueOf("SPRING"), "http://localhost/test/resource"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(
				exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		context.setPathPattern("/test/{name}");
		assertThat(convention.getContextualName(context)).isEqualTo("HTTP /test/{name}");
	}

	@Test
	void supportsOnlyHttpRequestsObservationContext() {
		assertThat(this.convention.supportsContext(this.context)).isTrue();
		assertThat(this.convention.supportsContext(new Observation.Context())).isFalse();
	}

	@Test
	void addsKeyValuesForExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("http://localhost/test/resource"));
		exchange.getResponse().setRawStatusCode(201);
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "POST"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "201"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "SUCCESS"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/resource"), KeyValue.of("http.request.method_original", "POST"));
	}

	@Test
	void addsKeyValuesForExchangeWithPathPattern() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/resource"));
		exchange.getResponse().setRawStatusCode(200);
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		context.setPathPattern("/test/{name}");

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "/test/{name}"), KeyValue.of("http.response.status_code", "200"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "SUCCESS"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/resource"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForErrorExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/resource"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		context.setError(new IllegalArgumentException("custom error"));
		exchange.getResponse().setRawStatusCode(500);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "500"),
						KeyValue.of("error.type", "java.lang.IllegalArgumentException"), KeyValue.of("outcome", "SERVER_ERROR"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/resource"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForRedirectExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/redirect"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getResponse().setRawStatusCode(302);
		exchange.getResponse().getHeaders().add("Location", "https://example.org/other");

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "REDIRECTION"), KeyValue.of("http.response.status_code", "302"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "REDIRECTION"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/redirect"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForNotFoundExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/notFound"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getResponse().setRawStatusCode(404);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "NOT_FOUND"), KeyValue.of("http.response.status_code", "404"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "CLIENT_ERROR"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/notFound"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForUnknownHttpMethodExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.valueOf("SPRING"), "http://localhost/test"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getResponse().setRawStatusCode(404);

		assertThat(this.convention.getContextualName(context)).isEqualTo("HTTP");
		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "_OTHER"), KeyValue.of("http.route", "NOT_FOUND"), KeyValue.of("http.response.status_code", "404"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "CLIENT_ERROR"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test"), KeyValue.of("http.request.method_original", "SPRING"));
	}

	@Test
	void addsKeyValuesForInvalidStatusExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/invalidStatus"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getResponse().setRawStatusCode(999);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "999"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "UNKNOWN"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/invalidStatus"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void supportsNullStatusCode() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/resource"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "UNKNOWN"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "UNKNOWN"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/resource"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForConnectionAbort() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/resource"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		context.setConnectionAborted(true);
		exchange.getResponse().setRawStatusCode(200);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "UNKNOWN"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "UNKNOWN"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/resource"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForConnectionAbortWhenResponseCommitted() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/resource"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		context.setConnectionAborted(true);
		exchange.getResponse().setRawStatusCode(404);
		exchange.getResponse().setComplete().block();

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "NOT_FOUND"), KeyValue.of("http.response.status_code", "404"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "UNKNOWN"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/resource"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void urlPathExcludesQueryString() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/users/123?foo=bar"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getResponse().setRawStatusCode(200);

		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/users/123"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForCustomClientErrorStatusExchange() {
		// Custom status code that is not an HttpStatus enum constant (HttpStatus.resolve returns null).
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/customStatus"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getResponse().setRawStatusCode(499);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "499"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "CLIENT_ERROR"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/customStatus"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForCustomServerErrorStatusExchange() {
		// Custom status code that is not an HttpStatus enum constant (HttpStatus.resolve returns null).
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/test/customServerError"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getResponse().setRawStatusCode(599);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "599"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "SERVER_ERROR"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/customServerError"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForRootRouteExchange() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getResponse().setRawStatusCode(200);
		context.setPathPattern("");

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "root"), KeyValue.of("http.response.status_code", "200"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "SUCCESS"), KeyValue.of("url.scheme", "http"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/"), KeyValue.of("http.request.method_original", "GET"));
	}

	@Test
	void addsKeyValuesForExchangeWithoutScheme() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test/resource"));
		ServerRequestObservationContext context = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getResponse().setRawStatusCode(200);

		assertThat(this.convention.getLowCardinalityKeyValues(context)).hasSize(6)
				.contains(KeyValue.of("http.request.method", "GET"), KeyValue.of("http.route", "UNKNOWN"), KeyValue.of("http.response.status_code", "200"),
						KeyValue.of("error.type", "none"), KeyValue.of("outcome", "SUCCESS"), KeyValue.of("url.scheme", "UNKNOWN"));
		assertThat(this.convention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("url.path", "/test/resource"), KeyValue.of("http.request.method_original", "GET"));
	}

}
