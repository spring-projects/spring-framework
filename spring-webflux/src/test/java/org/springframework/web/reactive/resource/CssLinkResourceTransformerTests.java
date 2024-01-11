/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.resource.EncodedResourceResolver.EncodedResource;
import org.springframework.web.reactive.resource.GzipSupport.GzippedFiles;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Tests for {@link CssLinkResourceTransformer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@ExtendWith(GzipSupport.class)
class CssLinkResourceTransformerTests {

	private ResourceTransformerChain transformerChain;


	@BeforeEach
	void setup() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		List<ResourceResolver> resolvers = List.of(versionResolver, new PathResourceResolver());

		CssLinkResourceTransformer cssLinkTransformer = new CssLinkResourceTransformer();
		cssLinkTransformer.setResourceUrlProvider(createUrlProvider(resolvers));

		this.transformerChain = new DefaultResourceTransformerChain(
				new DefaultResourceResolverChain(resolvers), Collections.singletonList(cssLinkTransformer));
	}

	private ResourceUrlProvider createUrlProvider(List<ResourceResolver> resolvers) {
		ResourceWebHandler handler = new ResourceWebHandler();
		handler.setLocations(List.of(new ClassPathResource("test/", getClass())));
		handler.setResourceResolvers(resolvers);

		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		urlProvider.registerHandlers(Map.of("/static/**", handler));
		return urlProvider;
	}


	@Test
	void transform() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/main.css"));
		Resource css = getResource("main.css");
		String expected = """

				@import url("/static/bar-11e16cf79faee7ac698c805cf28248d2.css?#iefix");
				@import url('/static/bar-11e16cf79faee7ac698c805cf28248d2.css#bla-normal');
				@import url(/static/bar-11e16cf79faee7ac698c805cf28248d2.css);

				@import "/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css";
				@import '/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css';

				body { background: url("/static/images/image-f448cd1d5dba82b774f3202c878230b3.png?#iefix") }
				""";

		StepVerifier.create(this.transformerChain.transform(exchange, css)
				.cast(TransformedResource.class))
				.consumeNextWith(transformedResource -> {
					String result = new String(transformedResource.getByteArray(), UTF_8);
					assertThat(result).isEqualToNormalizingNewlines(expected);
				})
				.expectComplete()
				.verify();
	}

	@Test
	void transformNoLinks() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/foo.css"));
		Resource expected = getResource("foo.css");

		StepVerifier.create(this.transformerChain.transform(exchange, expected))
				.consumeNextWith(resource -> assertThat(resource).isSameAs(expected))
				.expectComplete().verify();
	}

	@Test
	void transformExtLinksNotAllowed() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/external.css"));

		List<ResourceTransformer> transformers = Collections.singletonList(new CssLinkResourceTransformer());
		ResourceResolverChain mockChain = mock();
		ResourceTransformerChain chain = new DefaultResourceTransformerChain(mockChain, transformers);

		Resource resource = getResource("external.css");
		String expected = """
				@import url("https://example.org/fonts/css");
				body { background: url("file:///home/spring/image.png") }
				figure { background: url("//example.org/style.css")}""";

		StepVerifier.create(chain.transform(exchange, resource)
				.cast(TransformedResource.class))
				.consumeNextWith(transformedResource -> {
					String result = new String(transformedResource.getByteArray(), UTF_8);
					assertThat(result).isEqualToNormalizingNewlines(expected);
				})
				.expectComplete()
				.verify();

		List<Resource> locations = Collections.singletonList(resource);
		verify(mockChain, never()).resolveUrlPath("https://example.org/fonts/css", locations);
		verify(mockChain, never()).resolveUrlPath("file:///home/spring/image.png", locations);
		verify(mockChain, never()).resolveUrlPath("//example.org/style.css", locations);
	}

	@Test
	void transformSkippedForNonCssResource() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/images/image.png"));
		Resource expected = getResource("images/image.png");

		StepVerifier.create(this.transformerChain.transform(exchange, expected))
				.expectNext(expected)
				.expectComplete()
				.verify();
	}

	@Test
	void transformSkippedForGzippedResource(GzippedFiles gzippedFiles) throws Exception {
		gzippedFiles.create("main.css");

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/main.css"));
		Resource resource = getResource("main.css");
		EncodedResource gzipped = new EncodedResource(resource, "gzip", ".gz");

		StepVerifier.create(this.transformerChain.transform(exchange, gzipped))
				.expectNext(gzipped)
				.expectComplete()
				.verify();
	}

	@Test // https://github.com/spring-projects/spring-framework/issues/22602
	void transformEmptyUrlFunction() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/empty_url_function.css"));
		Resource css = getResource("empty_url_function.css");
		String expected = """
						.fooStyle {
							background: transparent url() no-repeat left top;
						}""";

		StepVerifier.create(this.transformerChain.transform(exchange, css)
				.cast(TransformedResource.class))
				.consumeNextWith(transformedResource -> {
					String result = new String(transformedResource.getByteArray(), UTF_8);
					assertThat(result).isEqualToNormalizingNewlines(expected);
				})
				.expectComplete()
				.verify();
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}

}
