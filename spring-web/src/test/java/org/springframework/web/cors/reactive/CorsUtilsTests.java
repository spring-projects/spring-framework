/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.cors.reactive;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.filter.reactive.ForwardedHeaderFilter;

import static org.junit.Assert.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;

/**
 * Test case for reactive {@link CorsUtils}.
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class CorsUtilsTests {

	@Test
	public void isCorsRequest() {
		ServerHttpRequest request = get("/").header(HttpHeaders.ORIGIN, "http://domain.com").build();
		assertTrue(CorsUtils.isCorsRequest(request));
	}

	@Test
	public void isNotCorsRequest() {
		ServerHttpRequest request = get("/").build();
		assertFalse(CorsUtils.isCorsRequest(request));
	}

	@Test
	public void isPreFlightRequest() {
		ServerHttpRequest request = options("/")
				.header(HttpHeaders.ORIGIN, "http://domain.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.build();
		assertTrue(CorsUtils.isPreFlightRequest(request));
	}

	@Test
	public void isNotPreFlightRequest() {
		ServerHttpRequest request = get("/").build();
		assertFalse(CorsUtils.isPreFlightRequest(request));

		request = options("/").header(HttpHeaders.ORIGIN, "http://domain.com").build();
		assertFalse(CorsUtils.isPreFlightRequest(request));

		request = options("/").header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET").build();
		assertFalse(CorsUtils.isPreFlightRequest(request));
	}

	@Test  // SPR-16262
	public void isSameOriginWithXForwardedHeaders() {
		String server = "mydomain1.com";
		testWithXForwardedHeaders(server, -1, "https", null, -1, "https://mydomain1.com");
		testWithXForwardedHeaders(server, 123, "https", null, -1, "https://mydomain1.com");
		testWithXForwardedHeaders(server, -1, "https", "mydomain2.com", -1, "https://mydomain2.com");
		testWithXForwardedHeaders(server, 123, "https", "mydomain2.com", -1, "https://mydomain2.com");
		testWithXForwardedHeaders(server, -1, "https", "mydomain2.com", 456, "https://mydomain2.com:456");
		testWithXForwardedHeaders(server, 123, "https", "mydomain2.com", 456, "https://mydomain2.com:456");
	}

	@Test  // SPR-16262
	public void isSameOriginWithForwardedHeader() {
		String server = "mydomain1.com";
		testWithForwardedHeader(server, -1, "proto=https", "https://mydomain1.com");
		testWithForwardedHeader(server, 123, "proto=https", "https://mydomain1.com");
		testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.com", "https://mydomain2.com");
		testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.com", "https://mydomain2.com");
		testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.com:456", "https://mydomain2.com:456");
		testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.com:456", "https://mydomain2.com:456");
	}

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
		assertTrue(CorsUtils.isSameOrigin(request));
	}

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
		assertTrue(CorsUtils.isSameOrigin(request));
	}

	// SPR-16668
	private ServerHttpRequest adaptFromForwardedHeaders(MockServerHttpRequest.BaseBuilder<?> builder) {
		AtomicReference<ServerHttpRequest> requestRef = new AtomicReference<>();
		MockServerWebExchange exchange = MockServerWebExchange.from(builder);
		new ForwardedHeaderFilter().filter(exchange, exchange2 -> {
			requestRef.set(exchange2.getRequest());
			return Mono.empty();
		}).block();
		return requestRef.get();
	}

}
