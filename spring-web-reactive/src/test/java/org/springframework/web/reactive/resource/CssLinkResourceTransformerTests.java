/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link CssLinkResourceTransformer}.
 * @author Rossen Stoyanchev
 */
public class CssLinkResourceTransformerTests {

	private ResourceTransformerChain transformerChain;

	private ServerWebExchange exchange;


	@Before
	public void setUp() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));

		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));

		List<ResourceResolver> resolvers = Arrays.asList(versionResolver, pathResolver);
		List<ResourceTransformer> transformers = Collections.singletonList(new CssLinkResourceTransformer());

		ResourceResolverChain resolverChain = new DefaultResourceResolverChain(resolvers);
		this.transformerChain = new DefaultResourceTransformerChain(resolverChain, transformers);

		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "");
		ServerHttpResponse response = new MockServerHttpResponse();
		WebSessionManager manager = new DefaultWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, response, manager);
	}


	@Test
	public void transform() throws Exception {
		Resource css = new ClassPathResource("test/main.css", getClass());
		TransformedResource actual = (TransformedResource) this.transformerChain.transform(this.exchange, css);

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
		Resource actual = this.transformerChain.transform(this.exchange, expected);
		assertSame(expected, actual);
	}

	@Test
	public void transformExtLinksNotAllowed() throws Exception {
		ResourceResolverChain resolverChain = Mockito.mock(DefaultResourceResolverChain.class);
		ResourceTransformerChain transformerChain = new DefaultResourceTransformerChain(resolverChain,
				Collections.singletonList(new CssLinkResourceTransformer()));

		Resource externalCss = new ClassPathResource("test/external.css", getClass());
		Resource resource = transformerChain.transform(this.exchange, externalCss);
		TransformedResource transformedResource = (TransformedResource) resource;

		String expected = "@import url(\"http://example.org/fonts/css\");\n" +
				"body { background: url(\"file:///home/spring/image.png\") }\n" +
				"figure { background: url(\"//example.org/style.css\")}";
		String result = new String(transformedResource.getByteArray(), "UTF-8");
		result = StringUtils.deleteAny(result, "\r");
		assertEquals(expected, result);

		Mockito.verify(resolverChain, Mockito.never())
				.resolveUrlPath("http://example.org/fonts/css", Collections.singletonList(externalCss));
		Mockito.verify(resolverChain, Mockito.never())
				.resolveUrlPath("file:///home/spring/image.png", Collections.singletonList(externalCss));
		Mockito.verify(resolverChain, Mockito.never())
				.resolveUrlPath("//example.org/style.css", Collections.singletonList(externalCss));
	}

	@Test
	public void transformWithNonCssResource() throws Exception {
		Resource expected = new ClassPathResource("test/images/image.png", getClass());
		Resource actual = this.transformerChain.transform(this.exchange, expected);
		assertSame(expected, actual);
	}

}
