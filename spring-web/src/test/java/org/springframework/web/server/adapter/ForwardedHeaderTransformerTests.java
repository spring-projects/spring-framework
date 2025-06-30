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

package org.springframework.web.server.adapter;

import java.net.InetSocketAddress;
import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ForwardedHeaderTransformer}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class ForwardedHeaderTransformerTests {

	private static final String BASE_URL = "https://example.com/path";

	private final ForwardedHeaderTransformer requestMutator = new ForwardedHeaderTransformer();

	@Test
	void removeOnly() {
		this.requestMutator.setRemoveOnly(true);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Forwarded", "for=192.0.2.60;proto=http;by=203.0.113.43");
		headers.add("X-Forwarded-Host", "example.com");
		headers.add("X-Forwarded-Port", "8080");
		headers.add("X-Forwarded-Proto", "http");
		headers.add("X-Forwarded-Prefix", "prefix");
		headers.add("X-Forwarded-Ssl", "on");
		headers.add("X-Forwarded-For", "203.0.113.195");
		ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

		assertForwardedHeadersRemoved(request);
	}

	@Test
	void xForwardedHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Host", "84.198.58.199");
		headers.add("X-Forwarded-Port", "443");
		headers.add("X-Forwarded-Proto", "https");
		headers.add("foo", "bar");
		ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

		assertThat(request.getURI()).isEqualTo(URI.create("https://84.198.58.199/path"));
		assertForwardedHeadersRemoved(request);
	}

	@Test
	void forwardedHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Forwarded", "host=84.198.58.199;proto=https");
		ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

		assertThat(request.getURI()).isEqualTo(URI.create("https://84.198.58.199/path"));
		assertForwardedHeadersRemoved(request);
	}

	@Test
	void xForwardedPrefix() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Prefix", "/prefix");
		ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

		assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/prefix/path"));
		assertThat(request.getPath().value()).isEqualTo("/prefix/path");
		assertForwardedHeadersRemoved(request);
	}

	@Test // gh-23305
	void xForwardedPrefixShouldNotLeadToDecodedPath() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Prefix", "/prefix");
		ServerHttpRequest request = MockServerHttpRequest
				.method(HttpMethod.GET, URI.create("https://example.com/a%20b?q=a%2Bb"))
				.headers(headers)
				.build();

		request = this.requestMutator.apply(request);

		assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/prefix/a%20b?q=a%2Bb"));
		assertThat(request.getPath().value()).isEqualTo("/prefix/a%20b");
		assertForwardedHeadersRemoved(request);
	}

	@Test
	void xForwardedPrefixTrailingSlash() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Prefix", "/prefix////");
		ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

		assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/prefix/path"));
		assertThat(request.getPath().value()).isEqualTo("/prefix/path");
		assertForwardedHeadersRemoved(request);
	}

	@Test // SPR-17525
	void shouldNotDoubleEncode() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Forwarded", "host=84.198.58.199;proto=https");

		ServerHttpRequest request = MockServerHttpRequest
				.method(HttpMethod.GET, URI.create("https://example.com/a%20b?q=a%2Bb"))
				.headers(headers)
				.build();

		request = this.requestMutator.apply(request);

		assertThat(request.getURI()).isEqualTo(URI.create("https://84.198.58.199/a%20b?q=a%2Bb"));
		assertForwardedHeadersRemoved(request);
	}

	@Test // gh-30137
	void shouldHandleUnencodedUri() {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Forwarded", "host=84.198.58.199;proto=https");
			ServerHttpRequest request = MockServerHttpRequest
							.method(HttpMethod.GET, URI.create("https://example.com/a?q=1+1=2"))
							.headers(headers)
							.build();

			request = this.requestMutator.apply(request);

			assertThat(request.getURI()).isEqualTo(URI.create("https://84.198.58.199/a?q=1+1=2"));
			assertForwardedHeadersRemoved(request);
	}

	@Test
	void shouldConcatenatePrefixes() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Prefix", "/first,/second");
		ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

		assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/first/second/path"));
		assertThat(request.getPath().value()).isEqualTo("/first/second/path");
		assertForwardedHeadersRemoved(request);
	}

	@Test
	void shouldConcatenatePrefixesWithTrailingSlashes() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Prefix", "/first/,/second//");
		ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

		assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/first/second/path"));
		assertThat(request.getPath().value()).isEqualTo("/first/second/path");
		assertForwardedHeadersRemoved(request);
	}

	@Test // gh-33465
	void shouldRemoveSingleTrailingSlash() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Forwarded-Prefix", "/prefix,/");
		ServerHttpRequest request = this.requestMutator.apply(getRequest(headers));

		assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/prefix/path"));
		assertThat(request.getPath().value()).isEqualTo("/prefix/path");
		assertForwardedHeadersRemoved(request);
	}

	@Test
	void forwardedForNotPresent() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Forwarded", "host=84.198.58.199;proto=https");

		InetSocketAddress remoteAddress = new InetSocketAddress("example.client", 47011);

		ServerHttpRequest request = MockServerHttpRequest
				.method(HttpMethod.GET, URI.create("https://example.com/a%20b?q=a%2Bb"))
				.remoteAddress(remoteAddress)
				.headers(headers)
				.build();

		request = this.requestMutator.apply(request);
		assertThat(request.getRemoteAddress()).isEqualTo(remoteAddress);
	}

	@Test
	void forwardedFor() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Forwarded", "for=\"203.0.113.195:4711\";host=84.198.58.199;proto=https");

		InetSocketAddress remoteAddress = new InetSocketAddress("example.client", 47011);

		ServerHttpRequest request = MockServerHttpRequest
				.method(HttpMethod.GET, URI.create("https://example.com/a%20b?q=a%2Bb"))
				.remoteAddress(remoteAddress)
				.headers(headers)
				.build();

		request = this.requestMutator.apply(request);
		assertThat(request.getRemoteAddress()).isNotNull();
		assertThat(request.getRemoteAddress().getHostName()).isEqualTo("203.0.113.195");
		assertThat(request.getRemoteAddress().getPort()).isEqualTo(4711);
	}

	@Test
	void xForwardedFor() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("x-forwarded-for", "203.0.113.195, 70.41.3.18, 150.172.238.178");

		ServerHttpRequest request = MockServerHttpRequest
				.method(HttpMethod.GET, URI.create("https://example.com/a%20b?q=a%2Bb"))
				.headers(headers)
				.build();

		request = this.requestMutator.apply(request);
		assertThat(request.getRemoteAddress()).isNotNull();
		assertThat(request.getRemoteAddress().getHostName()).isEqualTo("203.0.113.195");
	}


	private MockServerHttpRequest getRequest(HttpHeaders headers) {
		return MockServerHttpRequest.get(BASE_URL).headers(headers).build();
	}

	private void assertForwardedHeadersRemoved(ServerHttpRequest request) {
		ForwardedHeaderTransformer.FORWARDED_HEADER_NAMES
				.forEach(name -> assertThat(request.getHeaders().containsHeader(name)).isFalse());
	}

}
