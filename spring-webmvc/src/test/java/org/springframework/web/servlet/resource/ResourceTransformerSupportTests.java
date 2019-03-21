/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockHttpServletRequest;

import static org.junit.Assert.*;

/**
 * Unit tests for {@code ResourceTransformerSupport}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class ResourceTransformerSupportTests {

	private ResourceTransformerChain transformerChain;

	private TestResourceTransformerSupport transformer;

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");


	@Before
	public void setUp() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));
		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(pathResolver);
		this.transformerChain = new DefaultResourceTransformerChain(new DefaultResourceResolverChain(resolvers), null);

		this.transformer = new TestResourceTransformerSupport();
		this.transformer.setResourceUrlProvider(createUrlProvider(resolvers));
	}

	private ResourceUrlProvider createUrlProvider(List<ResourceResolver> resolvers) {
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));
		handler.setResourceResolvers(resolvers);

		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		urlProvider.setHandlerMap(Collections.singletonMap("/resources/**", handler));
		return urlProvider;
	}


	@Test
	public void resolveUrlPath() {
		this.request.setRequestURI("/context/servlet/resources/main.css");
		this.request.setContextPath("/context");
		this.request.setServletPath("/servlet");
		String resourcePath = "/context/servlet/resources/bar.css";
		Resource resource = getResource("main.css");
		String actual = this.transformer.resolveUrlPath(resourcePath, this.request, resource, this.transformerChain);

		assertEquals("/context/servlet/resources/bar-11e16cf79faee7ac698c805cf28248d2.css", actual);
	}

	@Test
	public void resolveUrlPathWithRelativePath() {
		Resource resource = getResource("main.css");
		String actual = this.transformer.resolveUrlPath("bar.css", this.request, resource, this.transformerChain);

		assertEquals("bar-11e16cf79faee7ac698c805cf28248d2.css", actual);
	}

	@Test
	public void resolveUrlPathWithRelativePathInParentDirectory() {
		Resource resource = getResource("images/image.png");
		String actual = this.transformer.resolveUrlPath("../bar.css", this.request, resource, this.transformerChain);

		assertEquals("../bar-11e16cf79faee7ac698c805cf28248d2.css", actual);
	}

	@Test
	public void toAbsolutePath() {
		String absolute = this.transformer.toAbsolutePath("img/image.png",
				new MockHttpServletRequest("GET", "/resources/style.css"));
		assertEquals("/resources/img/image.png", absolute);

		absolute = this.transformer.toAbsolutePath("/img/image.png",
				new MockHttpServletRequest("GET", "/resources/style.css"));
		assertEquals("/img/image.png", absolute);
	}

	private Resource getResource(String filePath) {
		return new ClassPathResource("test/" + filePath, getClass());
	}


	private static class TestResourceTransformerSupport extends ResourceTransformerSupport {

		@Override
		public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain chain) {
			throw new IllegalStateException("Should never be called");
		}
	}

}
