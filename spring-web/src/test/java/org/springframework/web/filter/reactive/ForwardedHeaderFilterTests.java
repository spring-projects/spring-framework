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

package org.springframework.web.filter.reactive;

import java.net.URI;
import java.time.Duration;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ForwardedHeaderFilter}.
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class ForwardedHeaderFilterTests {

	private static final String BASE_URL = "http://example.com/path";


	private final ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

	private final TestWebFilterChain filterChain = new TestWebFilterChain();


	@Test
	public void removeOnly() {

		this.filter.setRemoveOnly(true);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Forwarded", "for=192.0.2.60;proto=http;by=203.0.113.43");
		headers.add("X-Forwarded-Host", "example.com");
		headers.add("X-Forwarded-Port", "8080");
		headers.add("X-Forwarded-Proto", "http");
		headers.add("X-Forwarded-Prefix", "prefix");
		headers.add("X-Forwarded-Ssl", "on");
		this.filter.filter(getExchange(headers), this.filterChain).block(Duration.ZERO);

		this.filterChain.assertForwardedHeadersRemoved();
	}

	@Test
	public void xForwardedHeaders() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Host", "84.198.58.199");
		headers.add("X-Forwarded-Port", "443");
		headers.add("X-Forwarded-Proto", "https");
		headers.add("foo", "bar");
		this.filter.filter(getExchange(headers), this.filterChain).block(Duration.ZERO);

		assertEquals(new URI("https://84.198.58.199/path"), this.filterChain.uri);
		this.filterChain.assertForwardedHeadersRemoved();
	}

	@Test
	public void forwardedHeader() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Forwarded", "host=84.198.58.199;proto=https");
		this.filter.filter(getExchange(headers), this.filterChain).block(Duration.ZERO);

		assertEquals(new URI("https://84.198.58.199/path"), this.filterChain.uri);
		this.filterChain.assertForwardedHeadersRemoved();
	}

	@Test
	public void xForwardedPrefix() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Prefix", "/prefix");
		this.filter.filter(getExchange(headers), this.filterChain).block(Duration.ZERO);

		assertEquals(new URI("http://example.com/prefix/path"), this.filterChain.uri);
		assertEquals("/prefix/path", this.filterChain.requestPathValue);
		this.filterChain.assertForwardedHeadersRemoved();
	}

	@Test
	public void xForwardedPrefixTrailingSlash() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Prefix", "/prefix////");
		this.filter.filter(getExchange(headers), this.filterChain).block(Duration.ZERO);

		assertEquals(new URI("http://example.com/prefix/path"), this.filterChain.uri);
		assertEquals("/prefix/path", this.filterChain.requestPathValue);
		this.filterChain.assertForwardedHeadersRemoved();
	}

	private MockServerWebExchange getExchange(HttpHeaders headers) {
		MockServerHttpRequest request = MockServerHttpRequest.get(BASE_URL).headers(headers).build();
		return MockServerWebExchange.from(request);
	}


	private static class TestWebFilterChain implements WebFilterChain {

		@Nullable
		private HttpHeaders headers;

		@Nullable
		private URI uri;

		@Nullable String requestPathValue;


		@Nullable
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Nullable
		public String getHeader(String name) {
			assertNotNull(this.headers);
			return this.headers.getFirst(name);
		}

		public void assertForwardedHeadersRemoved() {
			assertNotNull(this.headers);
			ForwardedHeaderFilter.FORWARDED_HEADER_NAMES
					.forEach(name -> assertFalse(this.headers.containsKey(name)));
		}

		@Nullable
		public URI getUri() {
			return this.uri;
		}

		@Override
		public Mono<Void> filter(ServerWebExchange exchange) {
			ServerHttpRequest request = exchange.getRequest();
			this.headers = request.getHeaders();
			this.uri = request.getURI();
			this.requestPathValue = request.getPath().value();
			return Mono.empty();
		}
	}

}
