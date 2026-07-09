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

package org.springframework.web.reactive.function.client;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebClientUtils#getRequestDescription}.
 *
 * @author Park Dong-Yun
 */
class WebClientUtilsTests {

	@Test
	void stripsQueryParams() {
		URI uri = URI.create("https://api.example.com/search?q=test&page=1");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET https://api.example.com/search");
	}

	@Test
	void noQueryReturnsAsIs() {
		URI uri = URI.create("https://api.example.com/health");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET https://api.example.com/health");
	}

	@Test
	void preservesPort() {
		URI uri = URI.create("https://api.example.com:8443/path?q=1");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET https://api.example.com:8443/path");
	}

	@Test
	void remainsPathEncodedWhenStrippingQuery() {
		URI uri = URI.create("https://host/hello%20world?q=1");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET https://host/hello%20world");
	}

	@Test
	void stripsUserInfoWithQuery() {
		URI uri = URI.create("https://admin:secret@host/api?token=abc");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET https://host/api");
	}

	@Test
	void stripsUserInfoWithoutQuery() {
		URI uri = URI.create("https://admin:secret@host/api");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET https://host/api");
	}

	@Test
	void stripsFragmentWithQuery() {
		URI uri = URI.create("https://host/page?q=1#section");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET https://host/page");
	}

	@Test
	void stripsFragmentWithoutQuery() {
		URI uri = URI.create("https://host/page#section");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET https://host/page");
	}

	@Test
	void questionMarkInFragmentNotTreatedAsQuery() {
		URI uri = URI.create("https://host/page#frag?param=value");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET https://host/page");
	}

	@Test
	void ipv6Host() {
		URI uri = URI.create("http://[::1]:8080/path?q=1");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET http://[::1]:8080/path");
	}

	@Test
	void opaqueUriUnchanged() {
		URI uri = URI.create("mailto:user@example.com?subject=hello");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET mailto:user@example.com?subject=hello");
	}

	@Test
	void relativeUri() {
		URI uri = URI.create("/api/search?q=test");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.GET, uri))
				.isEqualTo("GET /api/search");
	}

	@Test
	void prefixesHttpMethod() {
		URI uri = URI.create("https://host/resource?v=1");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.POST, uri))
				.startsWith("POST ");
		assertThat(WebClientUtils.getRequestDescription(HttpMethod.DELETE, uri))
				.startsWith("DELETE ");
	}

}
