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

package org.springframework.web.reactive.resource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code ResourceTransformerSupport}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
class ResourceTransformerSupportTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);


	private ResourceTransformerChain chain;

	private TestResourceTransformerSupport transformer;


	@BeforeEach
	void setup() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(pathResolver);
		ResourceResolverChain resolverChain = new DefaultResourceResolverChain(resolvers);

		this.chain = new DefaultResourceTransformerChain(resolverChain, Collections.emptyList());
		this.transformer = new TestResourceTransformerSupport();
		this.transformer.setResourceUrlProvider(createUrlProvider(resolvers));
	}

	private ResourceUrlProvider createUrlProvider(List<ResourceResolver> resolvers) {
		ResourceWebHandler handler = new ResourceWebHandler();
		handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));
		handler.setResourceResolvers(resolvers);

		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		urlProvider.registerHandlers(Collections.singletonMap("/resources/**", handler));
		return urlProvider;
	}


	@Test
	void resolveUrlPath() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/resources/main.css"));
		String resourcePath = "/resources/bar.css";
		Resource resource = getResource("main.css");
		String actual = this.transformer.resolveUrlPath(resourcePath, exchange, resource, this.chain).block(TIMEOUT);

		assertThat(actual).isEqualTo("/resources/bar-11e16cf79faee7ac698c805cf28248d2.css");
		assertThat(actual).isEqualTo("/resources/bar-11e16cf79faee7ac698c805cf28248d2.css");
	}

	@Test
	void resolveUrlPathWithRelativePath() {
		Resource resource = getResource("main.css");
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		String actual = this.transformer.resolveUrlPath("bar.css", exchange, resource, this.chain).block(TIMEOUT);

		assertThat(actual).isEqualTo("bar-11e16cf79faee7ac698c805cf28248d2.css");
	}

	@Test
	void resolveUrlPathWithRelativePathInParentDirectory() {
		Resource resource = getResource("images/image.png");
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		String actual = this.transformer.resolveUrlPath("../bar.css", exchange, resource, this.chain).block(TIMEOUT);

		assertThat(actual).isEqualTo("../bar-11e16cf79faee7ac698c805cf28248d2.css");
	}

	@Test
	void toAbsolutePath() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/resources/main.css"));
		String absolute = this.transformer.toAbsolutePath("img/image.png", exchange);
		assertThat(absolute).isEqualTo("/resources/img/image.png");

		absolute = this.transformer.toAbsolutePath("/img/image.png", exchange);
		assertThat(absolute).isEqualTo("/img/image.png");
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}


	private static class TestResourceTransformerSupport extends ResourceTransformerSupport {

		@Override
		public Mono<Resource> transform(ServerWebExchange ex, Resource res, ResourceTransformerChain chain) {
			return Mono.error(new IllegalStateException("Should never be called"));
		}
	}

}
