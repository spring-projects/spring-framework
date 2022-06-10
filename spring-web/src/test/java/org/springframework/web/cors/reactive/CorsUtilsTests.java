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

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.options;

/**
 * Test case for reactive {@link CorsUtils}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class CorsUtilsTests {

	@Test
	public void isCorsRequest() {
		ServerHttpRequest request = get("http://domain.example/").header(HttpHeaders.ORIGIN, "https://domain.com").build();
		assertThat(CorsUtils.isCorsRequest(request)).isTrue();
	}

	@Test
	public void isNotCorsRequest() {
		ServerHttpRequest request = get("/").build();
		assertThat(CorsUtils.isCorsRequest(request)).isFalse();
	}

	@Test
	public void isPreFlightRequest() {
		ServerHttpRequest request = options("/")
				.header(HttpHeaders.ORIGIN, "https://domain.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.build();
		assertThat(CorsUtils.isPreFlightRequest(request)).isTrue();
	}

	@Test
	public void isNotPreFlightRequest() {
		ServerHttpRequest request = get("/").build();
		assertThat(CorsUtils.isPreFlightRequest(request)).isFalse();

		request = options("/").header(HttpHeaders.ORIGIN, "https://domain.com").build();
		assertThat(CorsUtils.isPreFlightRequest(request)).isFalse();
	}

	@Test  // SPR-16262
	public void isSameOriginWithXForwardedHeaders() {
		String server = "mydomain1.example";
		testWithXForwardedHeaders(server, -1, "https", null, -1, "https://mydomain1.example");
		testWithXForwardedHeaders(server, 123, "https", null, -1, "https://mydomain1.example");
		testWithXForwardedHeaders(server, -1, "https", "mydomain2.example", -1, "https://mydomain2.example");
		testWithXForwardedHeaders(server, 123, "https", "mydomain2.example", -1, "https://mydomain2.example");
		testWithXForwardedHeaders(server, -1, "https", "mydomain2.example", 456, "https://mydomain2.example:456");
		testWithXForwardedHeaders(server, 123, "https", "mydomain2.example", 456, "https://mydomain2.example:456");
	}

	@Test  // SPR-16262
	public void isSameOriginWithForwardedHeader() {
		String server = "mydomain1.example";
		testWithForwardedHeader(server, -1, "proto=https", "https://mydomain1.example");
		testWithForwardedHeader(server, 123, "proto=https", "https://mydomain1.example");
		testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.example", "https://mydomain2.example");
		testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.example", "https://mydomain2.example");
		testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.example:456", "https://mydomain2.example:456");
		testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.example:456", "https://mydomain2.example:456");
	}

	@Test  // SPR-16362
	@SuppressWarnings("deprecation")
	public void isSameOriginWithDifferentSchemes() {
		MockServerHttpRequest request = MockServerHttpRequest
				.get("http://mydomain1.example")
				.header(HttpHeaders.ORIGIN, "https://mydomain1.example")
				.build();
		assertThat(CorsUtils.isSameOrigin(request)).isFalse();
	}

	@SuppressWarnings("deprecation")
	private void testWithXForwardedHeaders(String serverName, int port,
			String forwardedProto, String forwardedHost, int forwardedPort, String originHeader) {

		String url = "http://" + serverName;
		if (port != -1) {
			url = url + ":" + port;
		}

		MockServerHttpRequest.BaseBuilder<?> builder = get(url).header(HttpHeaders.ORIGIN, originHeader);
		if (forwardedProto != null) {
			builder.header("X-Forwarded-Proto", forwardedProto);
		}
		if (forwardedHost != null) {
			builder.header("X-Forwarded-Host", forwardedHost);
		}
		if (forwardedPort != -1) {
			builder.header("X-Forwarded-Port", String.valueOf(forwardedPort));
		}

		ServerHttpRequest request = adaptFromForwardedHeaders(builder);
		assertThat(CorsUtils.isSameOrigin(request)).isTrue();
	}

	@SuppressWarnings("deprecation")
	private void testWithForwardedHeader(String serverName, int port,
			String forwardedHeader, String originHeader) {

		String url = "http://" + serverName;
		if (port != -1) {
			url = url + ":" + port;
		}

		MockServerHttpRequest.BaseBuilder<?> builder = get(url)
				.header("Forwarded", forwardedHeader)
				.header(HttpHeaders.ORIGIN, originHeader);

		ServerHttpRequest request = adaptFromForwardedHeaders(builder);
		assertThat(CorsUtils.isSameOrigin(request)).isTrue();
	}

	// SPR-16668
	private ServerHttpRequest adaptFromForwardedHeaders(MockServerHttpRequest.BaseBuilder<?> builder) {
		MockServerWebExchange exchange = MockServerWebExchange.from(builder);
		return new ForwardedHeaderTransformer().apply(exchange.getRequest());
	}

}
