/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.resource.CssLinkResourceTransformer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class CssLinkResourceTransformerTests {

	private ResourceTransformerChain transformerChain;

	private MockHttpServletRequest request;


	@Before
	public void setUp() {
		List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();
		resolvers.add(new FingerprintResourceResolver());
		resolvers.add(new PathResourceResolver());
		ResourceResolverChain resolverChain = new DefaultResourceResolverChain(resolvers);

		List<ResourceTransformer> transformers = new ArrayList<>();
		transformers.add(new CssLinkResourceTransformer());
		this.transformerChain = new DefaultResourceTransformerChain(resolverChain, transformers);

		this.request = new MockHttpServletRequest();
	}


	@Test
	public void transformNotCss() throws Exception {
		Resource expected = new ClassPathResource("test/images/image.png", getClass());
		Resource actual = this.transformerChain.transform(this.request, expected);
		assertSame(expected, actual);
	}

	@Test
	public void transform() throws Exception {
		Resource mainCss = new ClassPathResource("test/main.css", getClass());
		Resource resource = this.transformerChain.transform(this.request, mainCss);
		TransformedResource transformedResource = (TransformedResource) resource;

		String expected = "\n" +
				"@import url(\"bar-11e16cf79faee7ac698c805cf28248d2.css\");\n" +
				"@import url('bar-11e16cf79faee7ac698c805cf28248d2.css');\n" +
				"@import url(bar-11e16cf79faee7ac698c805cf28248d2.css);\n\n" +
				"@import \"foo-e36d2e05253c6c7085a91522ce43a0b4.css\";\n" +
				"@import 'foo-e36d2e05253c6c7085a91522ce43a0b4.css';\n\n" +
				"body { background: url(\"images/image-f448cd1d5dba82b774f3202c878230b3.png\") }\n\n" +
				"li { list-style: url(http://www.example.com/redball.png) disc }\n";

		assertEquals(expected, new String(transformedResource.getByteArray(), "UTF-8"));
	}

	@Test
	public void transformNoLinks() throws Exception {
		Resource expected = new ClassPathResource("test/foo.css", getClass());
		Resource actual = this.transformerChain.transform(this.request, expected);
		assertSame(expected, actual);
	}

}
