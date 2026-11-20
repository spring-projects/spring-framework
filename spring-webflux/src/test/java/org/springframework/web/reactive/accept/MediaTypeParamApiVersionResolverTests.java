/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MediaTypeParamApiVersionResolver}.
 * @author Rossen Stoyanchev
 */
class MediaTypeParamApiVersionResolverTests {

	private final MediaType mediaType = MediaType.parseMediaType("application/x.abc+json");

	private final ApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(mediaType, "version");

	private final MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/path");


	@Test
	void resolveFromAccept() {
		String version = "3";
		this.request.accept(getMediaType(version));
		testResolve(this.resolver, this.request, version);
	}

	@Test
	void resolveFromContentType() {
		String version = "3";
		this.request.header(HttpHeaders.CONTENT_TYPE, getMediaType(version).toString());
		testResolve(this.resolver, this.request, version);
	}

	@Test
	void fallbackToContentType() {
		String version = "3";
		this.request.accept(MediaType.ALL);
		this.request.header(HttpHeaders.CONTENT_TYPE, getMediaType(version).toString());
		testResolve(this.resolver, this.request, version);
	}

	@Test
	void wildcard() {
		MediaType compatibleMediaType = MediaType.parseMediaType("application/*+json");
		ApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");

		String version = "3";
		this.request.accept(getMediaType(version));
		testResolve(resolver, this.request, version);
	}

	private MediaType getMediaType(String version) {
		return new MediaType(this.mediaType, Map.of("version", version));
	}

	private static void testResolve(
			ApiVersionResolver resolver, MockServerHttpRequest.BaseBuilder<?> request, String expected) {

		ServerWebExchange exchange = MockServerWebExchange.from(request);
		String actual = resolver.resolveVersion(exchange);
		assertThat(actual).isEqualTo(expected);
	}

}
