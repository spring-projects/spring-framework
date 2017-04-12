/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * Unit tests for {@link AppCacheManifestTransformer}.
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class AppCacheManifestTransformerTests {

	private AppCacheManifestTransformer transformer;

	private ResourceTransformerChain chain;


	@Before
	public void setup() {
		ClassPathResource allowedLocation = new ClassPathResource("test/", getClass());
		ResourceWebHandler resourceHandler = new ResourceWebHandler();
		ResourceUrlProvider resourceUrlProvider = new ResourceUrlProvider();
		resourceUrlProvider.setHandlerMap(Collections.singletonMap("/static/**", resourceHandler));

		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(allowedLocation);
		List<ResourceResolver> resolvers = Arrays.asList(versionResolver, pathResolver);
		ResourceResolverChain resolverChain = new DefaultResourceResolverChain(resolvers);

		CssLinkResourceTransformer cssLinkResourceTransformer = new CssLinkResourceTransformer();
		cssLinkResourceTransformer.setResourceUrlProvider(resourceUrlProvider);
		List<ResourceTransformer> transformers = Collections.singletonList(cssLinkResourceTransformer);
		this.chain = new DefaultResourceTransformerChain(resolverChain, transformers);
		this.transformer = new AppCacheManifestTransformer();
		this.transformer.setResourceUrlProvider(resourceUrlProvider);

		resourceHandler.setResourceResolvers(resolvers);
		resourceHandler.setResourceTransformers(transformers);
		resourceHandler.setLocations(Collections.singletonList(allowedLocation));
	}


	@Test
	public void noTransformIfExtensionNoMatch() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.get("/static/foobar.file").toExchange();
		this.chain = mock(ResourceTransformerChain.class);
		Resource resource = mock(Resource.class);
		given(resource.getFilename()).willReturn("foobar.file");
		given(this.chain.transform(exchange, resource)).willReturn(Mono.just(resource));

		Resource result = this.transformer.transform(exchange, resource, this.chain).block(Duration.ofMillis(5000));
		assertEquals(resource, result);
	}

	@Test
	public void syntaxErrorInManifest() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.get("/static/error.appcache").toExchange();
		this.chain = mock(ResourceTransformerChain.class);
		Resource resource = new ClassPathResource("test/error.appcache", getClass());
		given(this.chain.transform(exchange, resource)).willReturn(Mono.just(resource));

		Resource result = this.transformer.transform(exchange, resource, this.chain).block(Duration.ofMillis(5000));
		assertEquals(resource, result);
	}

	@Test
	public void transformManifest() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.get("/static/test.appcache").toExchange();
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));

		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));

		List<ResourceResolver> resolvers = Arrays.asList(versionResolver, pathResolver);
		ResourceResolverChain resolverChain = new DefaultResourceResolverChain(resolvers);

		List<ResourceTransformer> transformers = new ArrayList<>();
		transformers.add(new CssLinkResourceTransformer());
		this.chain = new DefaultResourceTransformerChain(resolverChain, transformers);

		Resource resource = new ClassPathResource("test/test.appcache", getClass());
		Resource result = this.transformer.transform(exchange, resource, this.chain).block(Duration.ofMillis(5000));
		byte[] bytes = FileCopyUtils.copyToByteArray(result.getInputStream());
		String content = new String(bytes, "UTF-8");

		assertThat("should rewrite resource links", content,
				Matchers.containsString("/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css"));
		assertThat("should rewrite resource links", content,
				Matchers.containsString("/static/bar-11e16cf79faee7ac698c805cf28248d2.css"));
		assertThat("should rewrite resource links", content,
				Matchers.containsString("/static/js/bar-bd508c62235b832d960298ca6c0b7645.js"));

		assertThat("should not rewrite external resources", content,
				Matchers.containsString("//example.org/style.css"));
		assertThat("should not rewrite external resources", content,
				Matchers.containsString("http://example.org/image.png"));

		assertThat("should generate fingerprint", content,
				Matchers.containsString("# Hash: 4bf0338bcbeb0a5b3a4ec9ed8864107d"));
	}

}
