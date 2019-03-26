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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ResourceUrlProvider}.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 */
public class ResourceUrlProviderTests {

	private final List<Resource> locations = new ArrayList<>();

	private final ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

	private final Map<String, ResourceHttpRequestHandler> handlerMap = new HashMap<>();

	private final ResourceUrlProvider urlProvider = new ResourceUrlProvider();


	@Before
	public void setUp() throws Exception {
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
		this.handler.setServletContext(new MockServletContext());
		this.handler.setLocations(locations);
		this.handler.afterPropertiesSet();
		this.handlerMap.put("/resources/**", this.handler);
		this.urlProvider.setHandlerMap(this.handlerMap);
	}


	@Test
	public void getStaticResourceUrl() {
		String url = this.urlProvider.getForLookupPath("/resources/foo.css");
		assertEquals("/resources/foo.css", url);
	}

	@Test // SPR-13374
	public void getStaticResourceUrlRequestWithQueryOrHash() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContextPath("/");
		request.setRequestURI("/");

		String url = "/resources/foo.css?foo=bar&url=https://example.org";
		String resolvedUrl = this.urlProvider.getForRequestUrl(request, url);
		assertEquals("/resources/foo.css?foo=bar&url=https://example.org", resolvedUrl);

		url = "/resources/foo.css#hash";
		resolvedUrl = this.urlProvider.getForRequestUrl(request, url);
		assertEquals("/resources/foo.css#hash", resolvedUrl);
	}

	@Test // SPR-16526
	public void getStaticResourceWithMissingContextPath() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContextPath("/contextpath-longer-than-request-path");
		request.setRequestURI("/contextpath-longer-than-request-path/style.css");
		String url = "/resources/foo.css";
		String resolvedUrl = this.urlProvider.getForRequestUrl(request, url);
		assertNull(resolvedUrl);
	}

	@Test
	public void getFingerprintedResourceUrl() {
		Map<String, VersionStrategy> versionStrategyMap = new HashMap<>();
		versionStrategyMap.put("/**", new ContentVersionStrategy());
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(versionStrategyMap);

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		this.handler.setResourceResolvers(resolvers);

		String url = this.urlProvider.getForLookupPath("/resources/foo.css");
		assertEquals("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", url);
	}

	@Test // SPR-12647
	public void bestPatternMatch() throws Exception {
		ResourceHttpRequestHandler otherHandler = new ResourceHttpRequestHandler();
		otherHandler.setLocations(this.locations);
		Map<String, VersionStrategy> versionStrategyMap = new HashMap<>();
		versionStrategyMap.put("/**", new ContentVersionStrategy());
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(versionStrategyMap);

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		otherHandler.setResourceResolvers(resolvers);

		this.handlerMap.put("/resources/*.css", otherHandler);
		this.urlProvider.setHandlerMap(this.handlerMap);

		String url = this.urlProvider.getForLookupPath("/resources/foo.css");
		assertEquals("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", url);
	}

	@Test // SPR-12592
	@SuppressWarnings("resource")
	public void initializeOnce() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(HandlerMappingConfiguration.class);
		context.refresh();

		ResourceUrlProvider urlProviderBean = context.getBean(ResourceUrlProvider.class);
		assertThat(urlProviderBean.getHandlerMap(), Matchers.hasKey("/resources/**"));
		assertFalse(urlProviderBean.isAutodetect());
	}

	@Test // SPR-16296
	public void getForLookupPathShouldNotFailIfPathContainsDoubleSlashes() {
		// given
		ResourceResolver mockResourceResolver = mock(ResourceResolver.class);
		when(mockResourceResolver.resolveUrlPath(any(), any(), any())).thenReturn("some-path");

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.getResourceResolvers().add(mockResourceResolver);

		ResourceUrlProvider provider = new ResourceUrlProvider();
		provider.getHandlerMap().put("/some-pattern/**", handler);

		// when
		String lookupForPath = provider.getForLookupPath("/some-pattern/some-lib//some-resource");

		// then
		assertEquals("/some-pattern/some-path", lookupForPath);
	}


	@Configuration
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class HandlerMappingConfiguration {

		@Bean
		public SimpleUrlHandlerMapping simpleUrlHandlerMapping() {
			ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
			HashMap<String, ResourceHttpRequestHandler> handlerMap = new HashMap<>();
			handlerMap.put("/resources/**", handler);
			SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
			hm.setUrlMap(handlerMap);
			return hm;
		}

		@Bean
		public ResourceUrlProvider resourceUrlProvider() {
			return new ResourceUrlProvider();
		}
	}

}
