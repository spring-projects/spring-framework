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

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AppCacheManifestTransformer}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class AppCacheManifestTransformerTests {

	private AppCacheManifestTransformer transformer;

	private ResourceTransformerChain chain;

	private HttpServletRequest request;


	@BeforeEach
	public void setup() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));
		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(pathResolver);
		ResourceResolverChain resolverChain = new DefaultResourceResolverChain(resolvers);

		this.chain = new DefaultResourceTransformerChain(resolverChain, Collections.emptyList());
		this.transformer = new AppCacheManifestTransformer();
		this.transformer.setResourceUrlProvider(createUrlProvider(resolvers));
	}

	private ResourceUrlProvider createUrlProvider(List<ResourceResolver> resolvers) {
		ClassPathResource allowedLocation = new ClassPathResource("test/", getClass());
		ResourceHttpRequestHandler resourceHandler = new ResourceHttpRequestHandler();

		resourceHandler.setResourceResolvers(resolvers);
		resourceHandler.setLocations(Collections.singletonList(allowedLocation));

		ResourceUrlProvider resourceUrlProvider = new ResourceUrlProvider();
		resourceUrlProvider.setHandlerMap(Collections.singletonMap("/static/**", resourceHandler));
		return resourceUrlProvider;
	}


	@Test
	public void noTransformIfExtensionDoesNotMatch() throws Exception {
		this.request = new MockHttpServletRequest("GET", "/static/foo.css");
		Resource resource = getResource("foo.css");
		Resource result = this.transformer.transform(this.request, resource, this.chain);

		assertThat(result).isEqualTo(resource);
	}

	@Test
	public void syntaxErrorInManifest() throws Exception {
		this.request = new MockHttpServletRequest("GET", "/static/error.appcache");
		Resource resource = getResource("error.appcache");
		Resource result = this.transformer.transform(this.request, resource, this.chain);

		assertThat(result).isEqualTo(resource);
	}

	@Test
	public void transformManifest() throws Exception {
		this.request = new MockHttpServletRequest("GET", "/static/test.appcache");
		Resource resource = getResource("test.appcache");
		Resource actual = this.transformer.transform(this.request, resource, this.chain);

		byte[] bytes = FileCopyUtils.copyToByteArray(actual.getInputStream());
		String content = new String(bytes, "UTF-8");

		assertThat(content).as("rewrite resource links")
				.contains("/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css")
				.contains("/static/bar-11e16cf79faee7ac698c805cf28248d2.css")
				.contains("/static/js/bar-bd508c62235b832d960298ca6c0b7645.js");

		assertThat(content).as("not rewrite external resources")
				.contains("//example.org/style.css")
				.contains("https://example.org/image.png");

		assertThat(content).as("generate fingerprint")
				.contains("# Hash: 65ebc023e50b2b731fcace2871f0dae3");
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}

}
