/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestedContentTypeResolverBuilder}.
 * @author Rossen Stoyanchev
 */
public class RequestedContentTypeResolverBuilderTests {

	@Test
	void defaultSettings() {
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder().build();
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/flower").accept(MediaType.IMAGE_GIF));
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertThat(mediaTypes).isEqualTo(Collections.singletonList(MediaType.IMAGE_GIF));
	}

	@Test
	void parameterResolver() {
		RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
		builder.parameterResolver().mediaType("json", MediaType.APPLICATION_JSON);
		RequestedContentTypeResolver resolver = builder.build();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/flower?format=json"));
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

		assertThat(mediaTypes).isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
	}

	@Test
	void parameterResolverWithCustomParamName() {
		RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
		builder.parameterResolver().mediaType("json", MediaType.APPLICATION_JSON).parameterName("s");
		RequestedContentTypeResolver resolver = builder.build();

		List<MediaType> mediaTypes = resolver.resolveMediaTypes(
				MockServerWebExchange.from(MockServerHttpRequest.get("/flower?s=json")));

		assertThat(mediaTypes).isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
	}

	@Test // SPR-10513
	void fixedResolver() {
		RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
		builder.fixedResolver(MediaType.APPLICATION_JSON);
		RequestedContentTypeResolver resolver = builder.build();

		List<MediaType> mediaTypes = resolver.resolveMediaTypes(
				MockServerWebExchange.from(MockServerHttpRequest.get("/").accept(MediaType.ALL)));

		assertThat(mediaTypes).isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
	}

	@Test // SPR-12286
	void resolver() {
		RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
		builder.resolver(new FixedContentTypeResolver(MediaType.APPLICATION_JSON));
		RequestedContentTypeResolver resolver = builder.build();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);
		assertThat(mediaTypes).isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));

		exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").accept(MediaType.ALL));
		mediaTypes = resolver.resolveMediaTypes(exchange);
		assertThat(mediaTypes).isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
	}

	@Test
	void removeQualityFactorForMediaTypeAllChecks() {
		RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
		builder.resolver(new HeaderContentTypeResolver());
		builder.resolver(new FixedContentTypeResolver(MediaType.APPLICATION_JSON));
		RequestedContentTypeResolver resolver = builder.build();

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
				.accept(MediaType.valueOf("*/*;q=0.8")));
		List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);
		assertThat(mediaTypes).isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
	}

}
