/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.FileCopyUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;

/**
 * Unit tests for {@link AppCacheManifestTransformer}.
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class AppCacheManifestTransformerTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	
	private AppCacheManifestTransformer transformer;

	private ResourceTransformerChain chain;


	@Before
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

		assertSame(expected, actual);
	}

	@Test
	public void syntaxErrorInManifest() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/error.appcache"));
		Resource expected = getResource("error.appcache");
		Resource actual = this.transformer.transform(exchange, expected, this.chain).block(TIMEOUT);

		assertEquals(expected, actual);
	}

	@Test
	public void transformManifest() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/static/test.appcache"));
		Resource resource = getResource("test.appcache");
		Resource actual = this.transformer.transform(exchange, resource, this.chain).block(TIMEOUT);

		assertNotNull(actual);
		byte[] bytes = FileCopyUtils.copyToByteArray(actual.getInputStream());
		String content = new String(bytes, "UTF-8");

		assertThat("should rewrite resource links", content,
				containsString("/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css"));
		assertThat("should rewrite resource links", content,
				containsString("/static/bar-11e16cf79faee7ac698c805cf28248d2.css"));
		assertThat("should rewrite resource links", content,
				containsString("/static/js/bar-bd508c62235b832d960298ca6c0b7645.js"));

		assertThat("should not rewrite external resources", content, containsString("//example.org/style.css"));
		assertThat("should not rewrite external resources", content, containsString("http://example.org/image.png"));

		// Not the same hash as Spring MVC
		// Hash is computed from links, and not from the linked content

		assertThat("should generate fingerprint", content,
				containsString("# Hash: 8eefc904df3bd46537fa7bdbbc5ab9fb"));
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}

}
