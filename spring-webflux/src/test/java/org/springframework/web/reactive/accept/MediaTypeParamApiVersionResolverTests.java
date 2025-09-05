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
public class MediaTypeParamApiVersionResolverTests {

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
