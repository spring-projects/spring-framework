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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * Unit tests for {@code ResourceTransformerSupport}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class ResourceTransformerSupportTests {

	private ResourceTransformerChain transformerChain;

	private TestResourceTransformerSupport transformer;

	private MockHttpServletRequest request;


	@Before
	public void setUp() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		this.transformerChain = new DefaultResourceTransformerChain(new DefaultResourceResolverChain(resolvers), null);

		List<Resource> locations = new ArrayList<>();
		locations.add(new ClassPathResource("test/", getClass()));

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setLocations(locations);
		handler.setResourceResolvers(resolvers);

		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		urlProvider.setHandlerMap(Collections.singletonMap("/resources/**", handler));

		this.transformer = new TestResourceTransformerSupport();
		this.transformer.setResourceUrlProvider(urlProvider);

		this.request = new MockHttpServletRequest();
	}

	@Test
	public void rewriteAbsolutePathWithContext() throws Exception {
		this.request.setRequestURI("/servlet/context/resources/main.css");
		this.request.setMethod("GET");
		this.request.setServletPath("/servlet");
		this.request.setContextPath("/context");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/resources/main.css");

		String resourcePath = "/servlet/context/resources/bar.css";
		Resource mainCss = new ClassPathResource("test/main.css", getClass());
		String actual = this.transformer.resolveUrlPath(resourcePath, this.request, mainCss, this.transformerChain);
		assertEquals("/servlet/context/resources/bar-11e16cf79faee7ac698c805cf28248d2.css", actual);
	}

	@Test
	public void rewriteAbsolutePath() throws Exception {
		this.request.setRequestURI("/resources/main.css");
		this.request.setMethod("GET");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/resources/main.css");

		String resourcePath = "/resources/bar.css";
		Resource mainCss = new ClassPathResource("test/main.css", getClass());
		String actual = this.transformer.resolveUrlPath(resourcePath, this.request, mainCss, this.transformerChain);
		assertEquals("/resources/bar-11e16cf79faee7ac698c805cf28248d2.css", actual);

		actual = this.transformer.resolveUrlPath("bar.css", this.request, mainCss, this.transformerChain);
		assertEquals("bar-11e16cf79faee7ac698c805cf28248d2.css", actual);
	}

	@Test
	public void rewriteRelativePath() throws Exception {
		this.request.setRequestURI("/servlet/context/resources/main.css");
		this.request.setMethod("GET");
		this.request.setServletPath("/servlet");
		this.request.setContextPath("/context");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/resources/main.css");

		Resource mainCss = new ClassPathResource("test/main.css", getClass());
		String actual = this.transformer.resolveUrlPath("bar.css", this.request, mainCss, this.transformerChain);
		assertEquals("bar-11e16cf79faee7ac698c805cf28248d2.css", actual);
	}

	@Test(expected = IllegalStateException.class)
	public void rewriteAbsolutePathWrongPath() throws Exception {
		this.request.setRequestURI("/servlet/context/resources/main.css");
		this.request.setMethod("GET");
		this.request.setServletPath("/servlet");
		this.request.setContextPath("/context");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/wrong/main.css");

		String resourcePath = "/servlet/context/resources/bar.css";
		Resource mainCss = new ClassPathResource("test/main.css", getClass());
		this.transformer.resolveUrlPath(resourcePath, this.request, mainCss, this.transformerChain);
	}

	@Test
	public void rewriteRelativePathUpperLevel() throws Exception {
		this.request.setRequestURI("/servlet/context/resources/images/image.png");
		this.request.setMethod("GET");
		this.request.setServletPath("/servlet");
		this.request.setContextPath("/context");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/resources/images/image.png");

		Resource imagePng = new ClassPathResource("test/images/image.png", getClass());
		String actual = this.transformer.resolveUrlPath("../bar.css", this.request, imagePng, this.transformerChain);
		assertEquals("../bar-11e16cf79faee7ac698c805cf28248d2.css", actual);
	}


	private static class TestResourceTransformerSupport extends ResourceTransformerSupport {

		@Override
		public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain chain) {
			throw new IllegalStateException("Should never be called");
		}
	}

}
