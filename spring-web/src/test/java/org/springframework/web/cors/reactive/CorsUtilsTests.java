/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.get;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.options;

/**
 * Test case for reactive {@link CorsUtils}.
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class CorsUtilsTests {

	@Test
	public void isCorsRequest() {
		MockServerHttpRequest request = get("/").header(HttpHeaders.ORIGIN, "http://domain.com").build();
		assertTrue(CorsUtils.isCorsRequest(request));
	}

	@Test
	public void isNotCorsRequest() {
		MockServerHttpRequest request = get("/").build();
		assertFalse(CorsUtils.isCorsRequest(request));
	}

	@Test
	public void isPreFlightRequest() {
		MockServerHttpRequest request = options("/")
				.header(HttpHeaders.ORIGIN, "http://domain.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.build();
		assertTrue(CorsUtils.isPreFlightRequest(request));
	}

	@Test
	public void isNotPreFlightRequest() {
		MockServerHttpRequest request = get("/").build();
		assertFalse(CorsUtils.isPreFlightRequest(request));

		request = options("/").header(HttpHeaders.ORIGIN, "http://domain.com").build();
		assertFalse(CorsUtils.isPreFlightRequest(request));

		request = options("/").header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET").build();
		assertFalse(CorsUtils.isPreFlightRequest(request));
	}

	@Test  // SPR-16262
	public void isSameOriginWithXForwardedHeaders() {
		assertTrue(checkSameOriginWithXForwardedHeaders("mydomain1.com", -1, "https", null, -1, "https://mydomain1.com"));
		assertTrue(checkSameOriginWithXForwardedHeaders("mydomain1.com", 123, "https", null, -1, "https://mydomain1.com"));
		assertTrue(checkSameOriginWithXForwardedHeaders("mydomain1.com", -1, "https", "mydomain2.com", -1, "https://mydomain2.com"));
		assertTrue(checkSameOriginWithXForwardedHeaders("mydomain1.com", 123, "https", "mydomain2.com", -1, "https://mydomain2.com"));
		assertTrue(checkSameOriginWithXForwardedHeaders("mydomain1.com", -1, "https", "mydomain2.com", 456, "https://mydomain2.com:456"));
		assertTrue(checkSameOriginWithXForwardedHeaders("mydomain1.com", 123, "https", "mydomain2.com", 456, "https://mydomain2.com:456"));
	}

	@Test  // SPR-16262
	public void isSameOriginWithForwardedHeader() {
		assertTrue(checkSameOriginWithForwardedHeader("mydomain1.com", -1, "proto=https", "https://mydomain1.com"));
		assertTrue(checkSameOriginWithForwardedHeader("mydomain1.com", 123, "proto=https", "https://mydomain1.com"));
		assertTrue(checkSameOriginWithForwardedHeader("mydomain1.com", -1, "proto=https; host=mydomain2.com", "https://mydomain2.com"));
		assertTrue(checkSameOriginWithForwardedHeader("mydomain1.com", 123, "proto=https; host=mydomain2.com", "https://mydomain2.com"));
		assertTrue(checkSameOriginWithForwardedHeader("mydomain1.com", -1, "proto=https; host=mydomain2.com:456", "https://mydomain2.com:456"));
		assertTrue(checkSameOriginWithForwardedHeader("mydomain1.com", 123, "proto=https; host=mydomain2.com:456", "https://mydomain2.com:456"));
	}

	private boolean checkSameOriginWithXForwardedHeaders(String serverName, int port, String forwardedProto, String forwardedHost, int forwardedPort, String originHeader) {
		String url = "http://" + serverName;
		if (port != -1) {
			url = url + ":" + port;
		}
		MockServerHttpRequest.BaseBuilder<?> builder = get(url)
				.header(HttpHeaders.ORIGIN, originHeader);
		if (forwardedProto != null) {
			builder.header("X-Forwarded-Proto", forwardedProto);
		}
		if (forwardedHost != null) {
			builder.header("X-Forwarded-Host", forwardedHost);
		}
		if (forwardedPort != -1) {
			builder.header("X-Forwarded-Port", String.valueOf(forwardedPort));
		}
		return CorsUtils.isSameOrigin(builder.build());
	}

	private boolean checkSameOriginWithForwardedHeader(String serverName, int port, String forwardedHeader, String originHeader) {
		String url = "http://" + serverName;
		if (port != -1) {
			url = url + ":" + port;
		}
		MockServerHttpRequest.BaseBuilder<?> builder = get(url)
				.header("Forwarded", forwardedHeader)
				.header(HttpHeaders.ORIGIN, originHeader);
		return CorsUtils.isSameOrigin(builder.build());
	}

}
