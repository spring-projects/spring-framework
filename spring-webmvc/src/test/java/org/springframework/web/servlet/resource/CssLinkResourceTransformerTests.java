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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.util.StringUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CssLinkResourceTransformer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public class CssLinkResourceTransformerTests {

	private ResourceTransformerChain transformerChain;

	private MockHttpServletRequest request;


	@Before
	public void setUp() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));

		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));

		List<ResourceResolver> resolvers = Arrays.asList(versionResolver, pathResolver);
		List<ResourceTransformer> transformers = Arrays.asList(new CssLinkResourceTransformer());

		ResourceResolverChain resolverChain = new DefaultResourceResolverChain(resolvers);
		this.transformerChain = new DefaultResourceTransformerChain(resolverChain, transformers);

		this.request = new MockHttpServletRequest();
	}


	@Test
	public void transform() throws Exception {
		Resource css = new ClassPathResource("test/main.css", getClass());
		TransformedResource actual = (TransformedResource) this.transformerChain.transform(this.request, css);

		String expected = "\n" +
				"@import url(\"bar-11e16cf79faee7ac698c805cf28248d2.css\");\n" +
				"@import url('bar-11e16cf79faee7ac698c805cf28248d2.css');\n" +
				"@import url(bar-11e16cf79faee7ac698c805cf28248d2.css);\n\n" +
				"@import \"foo-e36d2e05253c6c7085a91522ce43a0b4.css\";\n" +
				"@import 'foo-e36d2e05253c6c7085a91522ce43a0b4.css';\n\n" +
				"body { background: url(\"images/image-f448cd1d5dba82b774f3202c878230b3.png\") }\n";

		String result = new String(actual.getByteArray(), "UTF-8");
		result = StringUtils.deleteAny(result, "\r");
		assertEquals(expected, result);
	}

	@Test
	public void transformNoLinks() throws Exception {
		Resource expected = new ClassPathResource("test/foo.css", getClass());
		Resource actual = this.transformerChain.transform(this.request, expected);
		assertSame(expected, actual);
	}

	@Test
	public void transformExtLinksNotAllowed() throws Exception {
		ResourceResolverChain resolverChain = Mockito.mock(DefaultResourceResolverChain.class);
		ResourceTransformerChain transformerChain = new DefaultResourceTransformerChain(resolverChain,
				Arrays.asList(new CssLinkResourceTransformer()));

		Resource externalCss = new ClassPathResource("test/external.css", getClass());
		Resource resource = transformerChain.transform(this.request, externalCss);
		TransformedResource transformedResource = (TransformedResource) resource;

		String expected = "@import url(\"https://example.org/fonts/css\");\n" +
				"body { background: url(\"file:///home/spring/image.png\") }\n" +
				"figure { background: url(\"//example.org/style.css\")}";
		String result = new String(transformedResource.getByteArray(), "UTF-8");
		result = StringUtils.deleteAny(result, "\r");
		assertEquals(expected, result);

		Mockito.verify(resolverChain, Mockito.never())
				.resolveUrlPath("https://example.org/fonts/css", Arrays.asList(externalCss));
		Mockito.verify(resolverChain, Mockito.never())
				.resolveUrlPath("file:///home/spring/image.png", Arrays.asList(externalCss));
		Mockito.verify(resolverChain, Mockito.never())
				.resolveUrlPath("//example.org/style.css", Arrays.asList(externalCss));
	}

	@Test
	public void transformWithNonCssResource() throws Exception {
		Resource expected = new ClassPathResource("test/images/image.png", getClass());
		Resource actual = this.transformerChain.transform(this.request, expected);
		assertSame(expected, actual);
	}

	@Test // https://github.com/spring-projects/spring-framework/issues/22602
	public void transformEmptyUrlFunction() throws Exception {
		this.request = new MockHttpServletRequest("GET", "/static/empty_url_function.css");
		Resource css = getResource("empty_url_function.css");
		String expected =
				".fooStyle {\n" +
				"\tbackground: transparent url() no-repeat left top;\n" +
				"}";

		TransformedResource actual = (TransformedResource) this.transformerChain.transform(this.request, css);
		String result = new String(actual.getByteArray(), "UTF-8");
		result = StringUtils.deleteAny(result, "\r");
		assertEquals(expected, result);
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}

}
