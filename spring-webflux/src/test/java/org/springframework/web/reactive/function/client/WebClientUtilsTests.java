/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
