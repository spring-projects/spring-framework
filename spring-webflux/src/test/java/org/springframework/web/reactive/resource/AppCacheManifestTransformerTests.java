/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.get;

/**
 * Unit tests for {@link AppCacheManifestTransformer}.
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class AppCacheManifestTransformerTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);


	private AppCacheManifestTransformer transformer;

	private ResourceTransformerChain chain;


	@BeforeEach
	public void setup() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		ResourceResolverChain resolverChain = new DefaultResourceResolverChain(resolvers);

		this.chain = new DefaultResourceTransformerChain(resolverChain, Collections.emptyList());
		this.transformer = new AppCacheManifestTransformer();
		this.transformer.setResourceUrlProvider(createUrlProvider(resolvers));
	}

	private ResourceUrlProvider createUrlProvider(List<ResourceResolver> resolvers) {
		ResourceWebHandler handler = new ResourceWebHandler();
		handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));
		handler.setResourceResolvers(resolvers);

		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		urlProvider.registerHandlers(Collections.singletonMap("/static/**", handler));
		return urlProvider;
	}


	@Test
	public void noTransformIfExtensionDoesNotMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/foo.css"));
		Resource expected = getResource("foo.css");
		Resource actual = this.transformer.transform(exchange, expected, this.chain).block(TIMEOUT);

		assertThat(actual).isSameAs(expected);
	}

	@Test
	public void syntaxErrorInManifest() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/error.appcache"));
		Resource expected = getResource("error.appcache");
		Resource actual = this.transformer.transform(exchange, expected, this.chain).block(TIMEOUT);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void transformManifest() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/test.appcache"));
		Resource resource = getResource("test.appcache");
		Resource actual = this.transformer.transform(exchange, resource, this.chain).block(TIMEOUT);

		assertThat(actual).isNotNull();
		byte[] bytes = FileCopyUtils.copyToByteArray(actual.getInputStream());
		String content = new String(bytes, "UTF-8");

		assertThat(content).as("rewrite resource links")
				.contains("/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css")
				.contains("/static/bar-11e16cf79faee7ac698c805cf28248d2.css")
				.contains("/static/js/bar-bd508c62235b832d960298ca6c0b7645.js");

		assertThat(content).as("not rewrite external resources")
				.contains("//example.org/style.css")
				.contains("https://example.org/image.png");

		// Not the same hash as Spring MVC
		// Hash is computed from links, and not from the linked content

		assertThat(content).as("generate fingerprint")
				.contains("# Hash: d4437f1d7ae9530ab3ae71d5375b46ff");
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}

}
