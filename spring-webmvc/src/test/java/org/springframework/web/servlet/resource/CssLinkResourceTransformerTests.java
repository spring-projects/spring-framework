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

package org.springframework.web.servlet.resource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.EncodedResourceResolver.EncodedResource;
import org.springframework.web.servlet.resource.GzipSupport.GzippedFiles;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CssLinkResourceTransformer}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.1
 */
@ExtendWith(GzipSupport.class)
class CssLinkResourceTransformerTests {

	private ResourceTransformerChain transformerChain;

	private MockHttpServletRequest request;


	@BeforeEach
	void setUp() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));
		List<ResourceResolver> resolvers = List.of(versionResolver, new PathResourceResolver());
		ResourceUrlProvider resourceUrlProvider = createUrlProvider(resolvers);

		CssLinkResourceTransformer cssLinkTransformer = new CssLinkResourceTransformer();
		cssLinkTransformer.setResourceUrlProvider(resourceUrlProvider);

		this.transformerChain = new DefaultResourceTransformerChain(
				new DefaultResourceResolverChain(resolvers), Collections.singletonList(cssLinkTransformer));
	}

	private ResourceUrlProvider createUrlProvider(List<ResourceResolver> resolvers) {
		ResourceHttpRequestHandler resourceHandler = new ResourceHttpRequestHandler();
		resourceHandler.setResourceResolvers(resolvers);
		resourceHandler.setLocations(List.of(new ClassPathResource("test/", getClass())));

		ResourceUrlProvider resourceUrlProvider = new ResourceUrlProvider();
		resourceUrlProvider.setHandlerMap(Map.of("/static/**", resourceHandler));
		return resourceUrlProvider;
	}


	@Test
	void transform() throws Exception {
		this.request = new MockHttpServletRequest("GET", "/static/main.css");
		Resource css = getResource("main.css");
		String expected = """

				@import url("/static/bar-11e16cf79faee7ac698c805cf28248d2.css?#iefix");
				@import url('/static/bar-11e16cf79faee7ac698c805cf28248d2.css#bla-normal');
				@import url(/static/bar-11e16cf79faee7ac698c805cf28248d2.css);

				@import "/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css";
				@import '/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css';

				body { background: url("/static/images/image-f448cd1d5dba82b774f3202c878230b3.png?#iefix") }
				""";

		TransformedResource actual = (TransformedResource) this.transformerChain.transform(this.request, css);
		String result = new String(actual.getByteArray(), UTF_8);
		assertThat(result).isEqualToNormalizingNewlines(expected);
	}

	@Test
	void transformNoLinks() throws Exception {
		this.request = new MockHttpServletRequest("GET", "/static/foo.css");
		Resource expected = getResource("foo.css");
		Resource actual = this.transformerChain.transform(this.request, expected);
		assertThat(actual).isSameAs(expected);
	}

	@Test
	void transformExtLinksNotAllowed() throws Exception {
		this.request = new MockHttpServletRequest("GET", "/static/external.css");

		List<ResourceTransformer> transformers = Collections.singletonList(new CssLinkResourceTransformer());
		ResourceResolverChain mockChain = mock();
		ResourceTransformerChain chain = new DefaultResourceTransformerChain(mockChain, transformers);

		Resource resource = getResource("external.css");
		String expected = """
				@import url("https://example.org/fonts/css");
				body { background: url("file:///home/spring/image.png") }
				figure { background: url("//example.org/style.css")}""";

		TransformedResource transformedResource = (TransformedResource) chain.transform(this.request, resource);
		String result = new String(transformedResource.getByteArray(), UTF_8);
		assertThat(result).isEqualToNormalizingNewlines(expected);

		List<Resource> locations = List.of(resource);
		verify(mockChain, never()).resolveUrlPath("https://example.org/fonts/css", locations);
		verify(mockChain, never()).resolveUrlPath("file:///home/spring/image.png", locations);
		verify(mockChain, never()).resolveUrlPath("//example.org/style.css", locations);
	}

	@Test
	void transformSkippedForNonCssResource() throws Exception {
		this.request = new MockHttpServletRequest("GET", "/static/images/image.png");
		Resource expected = getResource("images/image.png");
		Resource actual = this.transformerChain.transform(this.request, expected);

		assertThat(actual).isSameAs(expected);
	}

	@Test
	void transformSkippedForGzippedResource(GzippedFiles gzippedFiles) throws Exception {
		gzippedFiles.create("main.css");

		this.request = new MockHttpServletRequest("GET", "/static/main.css");
		Resource original = new ClassPathResource("test/main.css", getClass());
		EncodedResource gzipped = new EncodedResource(original, "gzip", ".gz");
		Resource actual = this.transformerChain.transform(this.request, gzipped);

		assertThat(actual).isSameAs(gzipped);
	}

	@Test // https://github.com/spring-projects/spring-framework/issues/22602
	void transformEmptyUrlFunction() throws Exception {
		this.request = new MockHttpServletRequest("GET", "/static/empty_url_function.css");
		Resource css = getResource("empty_url_function.css");
		String expected = """
						.fooStyle {
							background: transparent url() no-repeat left top;
						}""";

		TransformedResource actual = (TransformedResource) this.transformerChain.transform(this.request, css);
		String result = new String(actual.getByteArray(), UTF_8);
		assertThat(result).isEqualToNormalizingNewlines(expected);
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}

}
