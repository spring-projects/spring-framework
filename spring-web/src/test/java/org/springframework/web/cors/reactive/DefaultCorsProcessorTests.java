/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.cors.reactive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.http.HttpHeaders.VARY;

/**
 * {@link DefaultCorsProcessor} tests with simple or pre-flight CORS request.
 *
 * @author Sebastien Deleuze
 */
public class DefaultCorsProcessorTests {

	private DefaultCorsProcessor processor;

	private CorsConfiguration conf;


	@BeforeEach
	public void setup() {
		this.conf = new CorsConfiguration();
		this.processor = new DefaultCorsProcessor();
	}


	@Test
	public void requestWithoutOriginHeader() {
		MockServerHttpRequest request = MockServerHttpRequest
				.method(HttpMethod.GET, "http://domain1.example/test.html")
				.build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void sameOriginRequest() {
		MockServerHttpRequest request = MockServerHttpRequest
				.method(HttpMethod.GET, "http://domain1.example/test.html")
				.header(HttpHeaders.ORIGIN, "http://domain1.example")
				.build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void actualRequestWithOriginHeader() {
		ServerWebExchange exchange = actualRequest();
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void actualRequestWithOriginHeaderAndNullConfig() {
		ServerWebExchange exchange = actualRequest();
		this.processor.process(null, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void actualRequestWithOriginHeaderAndAllowedOrigin() {
		ServerWebExchange exchange = actualRequest();
		this.conf.addAllowedOrigin("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
		assertThat(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_MAX_AGE)).isFalse();
		assertThat(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void actualRequestCredentials() {
		ServerWebExchange exchange = actualRequest();
		this.conf.addAllowedOrigin("https://domain1.com");
		this.conf.addAllowedOrigin("https://domain2.com");
		this.conf.addAllowedOrigin("http://domain3.example");
		this.conf.setAllowCredentials(true);
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isTrue();
		assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void actualRequestCredentialsWithWildcardOrigin() {
		ServerWebExchange exchange = actualRequest();
		this.conf.addAllowedOrigin("*");
		this.conf.setAllowCredentials(true);
		assertThatIllegalArgumentException().isThrownBy(() -> this.processor.process(this.conf, exchange));

		this.conf.setAllowedOrigins(null);
		this.conf.addAllowedOriginPattern("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isTrue();
		assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void actualRequestCaseInsensitiveOriginMatch() {
		ServerWebExchange exchange = actualRequest();
		this.conf.addAllowedOrigin("https://DOMAIN2.com");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat((Object) response.getStatusCode()).isNull();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
	}

	@Test // gh-26892
	public void actualRequestTrailingSlashOriginMatch() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.method(HttpMethod.GET, "http://localhost/test.html")
				.header(HttpHeaders.ORIGIN, "https://domain2.com/"));

		this.conf.addAllowedOrigin("https://domain2.com");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat((Object) response.getStatusCode()).isNull();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
	}

	@Test
	public void actualRequestExposedHeaders() {
		ServerWebExchange exchange = actualRequest();
		this.conf.addExposedHeader("header1");
		this.conf.addExposedHeader("header2");
		this.conf.addAllowedOrigin("https://domain2.com");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).isTrue();
		assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header1")).isTrue();
		assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).contains("header2")).isTrue();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void preflightRequestAllOriginsAllowed() {
		ServerWebExchange exchange = MockServerWebExchange.from(
				preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "GET"));
		this.conf.addAllowedOrigin("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}


	@Test
	public void preflightRequestWrongAllowedMethod() {
		ServerWebExchange exchange = MockServerWebExchange.from(
				preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "DELETE"));
		this.conf.addAllowedOrigin("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void preflightRequestMatchedAllowedMethod() {
		ServerWebExchange exchange = MockServerWebExchange.from(
				preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "GET"));
		this.conf.addAllowedOrigin("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat((Object) response.getStatusCode()).isNull();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET,HEAD");
	}

	@Test
	public void preflightRequestTestWithOriginButWithoutOtherHeaders() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest());
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void preflightRequestWithoutRequestMethod() {
		ServerWebExchange exchange = MockServerWebExchange.from(
				preFlightRequest().header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1"));
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void preflightRequestWithRequestAndMethodHeaderButNoConfig() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1"));

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void preflightRequestValidRequestAndConfig() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1"));

		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedMethod("GET");
		this.conf.addAllowedMethod("PUT");
		this.conf.addAllowedHeader("header1");
		this.conf.addAllowedHeader("header2");

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
		assertThat(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isTrue();
		assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET,PUT");
		assertThat(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_MAX_AGE)).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void preflightRequestCredentials() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1"));

		this.conf.addAllowedOrigin("https://domain1.com");
		this.conf.addAllowedOrigin("https://domain2.com");
		this.conf.addAllowedOrigin("http://domain3.example");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(response.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isTrue();
		assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void preflightRequestCredentialsWithWildcardOrigin() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1"));

		this.conf.addAllowedOrigin("https://domain1.com");
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedOrigin("http://domain3.example");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowCredentials(true);
		assertThatIllegalArgumentException().isThrownBy(() -> this.processor.process(this.conf, exchange));

		this.conf.setAllowedOrigins(null);
		this.conf.addAllowedOriginPattern("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void preflightRequestPrivateNetworkWithWildcardOrigin() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1")
				.header(DefaultCorsProcessor.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK, "true"));

		this.conf.addAllowedOrigin("https://domain1.com");
		this.conf.addAllowedOrigin("*");
		this.conf.addAllowedOrigin("http://domain3.example");
		this.conf.addAllowedHeader("Header1");
		this.conf.setAllowPrivateNetwork(true);
		assertThatIllegalArgumentException().isThrownBy(() -> this.processor.process(this.conf, exchange));

		this.conf.setAllowedOrigins(null);
		this.conf.addAllowedOriginPattern("*");
		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().containsKey(DefaultCorsProcessor.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat(response.getStatusCode()).isNull();
	}

	@Test
	public void preflightRequestAllowedHeaders() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2"));

		this.conf.addAllowedHeader("Header1");
		this.conf.addAllowedHeader("Header2");
		this.conf.addAllowedHeader("Header3");
		this.conf.addAllowedOrigin("https://domain2.com");

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS)).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1")).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2")).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header3")).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void preflightRequestAllowsAllHeaders() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2"));

		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("https://domain2.com");

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS)).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1")).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2")).isTrue();
		assertThat(response.getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS).contains("*")).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void preflightRequestWithEmptyHeaders() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, ""));

		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("https://domain2.com");

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS)).isFalse();
		assertThat(response.getHeaders().get(VARY)).contains(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
		assertThat((Object) response.getStatusCode()).isNull();
	}

	@Test
	public void preflightRequestWithNullConfig() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(
				preFlightRequest().header(ACCESS_CONTROL_REQUEST_METHOD, "GET"));
		this.conf.addAllowedOrigin("*");
		this.processor.process(null, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void preventDuplicatedVaryHeaders() {
		MockServerHttpRequest request = MockServerHttpRequest
				.method(HttpMethod.GET, "http://domain1.example/test.html")
				.header(HttpHeaders.ORIGIN, "http://domain1.example")
				.build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		ServerHttpResponse response = exchange.getResponse();
		HttpHeaders responseHeaders = response.getHeaders();
		responseHeaders.add(VARY, ORIGIN);
		responseHeaders.add(VARY, ACCESS_CONTROL_REQUEST_METHOD);
		responseHeaders.add(VARY, ACCESS_CONTROL_REQUEST_HEADERS);

		this.processor.process(this.conf, exchange);

		assertThat(responseHeaders.get(VARY)).containsOnlyOnce(ORIGIN,
				ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS);
	}

	@Test
	public void preflightRequestWithoutAccessControlRequestPrivateNetwork() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET"));

		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("https://domain2.com");

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().containsKey(DefaultCorsProcessor.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isFalse();
		assertThat(response.getStatusCode()).isNull();
	}

	@Test
	public void preflightRequestWithAccessControlRequestPrivateNetworkNotAllowed() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(DefaultCorsProcessor.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK, "true"));

		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("https://domain2.com");

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().containsKey(DefaultCorsProcessor.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isFalse();
		assertThat(response.getStatusCode()).isNull();
	}

	@Test
	public void preflightRequestWithAccessControlRequestPrivateNetworkAllowed() {
		ServerWebExchange exchange = MockServerWebExchange.from(preFlightRequest()
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(DefaultCorsProcessor.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK, "true"));

		this.conf.addAllowedHeader("*");
		this.conf.addAllowedOrigin("https://domain2.com");
		this.conf.setAllowPrivateNetwork(true);

		this.processor.process(this.conf, exchange);

		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)).isTrue();
		assertThat(response.getHeaders().containsKey(DefaultCorsProcessor.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isTrue();
		assertThat(response.getStatusCode()).isNull();
	}


	private ServerWebExchange actualRequest() {
		return MockServerWebExchange.from(corsRequest(HttpMethod.GET));
	}

	private MockServerHttpRequest.BaseBuilder<?> preFlightRequest() {
		return corsRequest(HttpMethod.OPTIONS);
	}

	private MockServerHttpRequest.BaseBuilder<?> corsRequest(HttpMethod method) {
		return MockServerHttpRequest
				.method(method, "http://localhost/test.html")
				.header(HttpHeaders.ORIGIN, "https://domain2.com");
	}

}
