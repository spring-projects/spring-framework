/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MediaTypeParamApiVersionResolver}.
 * @author Rossen Stoyanchev
 */
class MediaTypeParamApiVersionResolverTests {

	private final MediaType mediaType = MediaType.parseMediaType("application/x.abc+json");

	private final ApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(mediaType, "version");

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");


	@Test
	void resolveFromAccept() {
		String version = "3";
		this.request.addHeader("Accept", getMediaType(version));
		testResolve(this.resolver, this.request, version);
	}

	@Test
	void resolveFromContentType() {
		String version = "3";
		this.request.setContentType(getMediaType(version).toString());
		testResolve(this.resolver, this.request, version);
	}

	@Test
	void fallbackToContentType() {
		String version = "3";
		this.request.addHeader("Accept", "*/*");
		this.request.setContentType(getMediaType(version).toString());
		testResolve(this.resolver, this.request, version);
	}

	@Test
	void wildcard() {
		MediaType compatibleMediaType = MediaType.parseMediaType("application/*+json");
		ApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");

		String version = "3";
		this.request.addHeader("Accept", getMediaType(version));
		testResolve(resolver, this.request, version);
	}

	private MediaType getMediaType(String version) {
		return new MediaType(this.mediaType, Map.of("version", version));
	}

	private void testResolve(ApiVersionResolver resolver, MockHttpServletRequest request, String expected) {
		try {
			ServletRequestPathUtils.parseAndCache(request);
			String actual = resolver.resolveVersion(request);
			assertThat(actual).isEqualTo(expected);
		}
		finally {
			ServletRequestPathUtils.clearParsedRequestPath(request);
		}
	}

}
