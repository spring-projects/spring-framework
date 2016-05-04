/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.http.CacheControl;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.AppCacheManifestTransformer;
import org.springframework.web.servlet.resource.CachingResourceResolver;
import org.springframework.web.servlet.resource.CachingResourceTransformer;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.servlet.resource.WebJarsResourceResolver;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ResourceHandlerRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceHandlerRegistryTests {

	private ResourceHandlerRegistry registry;

	private ResourceHandlerRegistration registration;

	private MockHttpServletResponse response;


	@Before
	public void setUp() {
		this.registry = new ResourceHandlerRegistry(new GenericWebApplicationContext(), new MockServletContext());
		this.registration = registry.addResourceHandler("/resources/**");
		this.registration.addResourceLocations("classpath:org/springframework/web/servlet/config/annotation/");
		this.response = new MockHttpServletResponse();
	}

	@Test
	public void noResourceHandlers() throws Exception {
		this.registry = new ResourceHandlerRegistry(new GenericWebApplicationContext(), new MockServletContext());
		assertNull(this.registry.getHandlerMapping());
	}

	@Test
	public void mapPathToLocation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/testStylesheet.css");

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		handler.handleRequest(request, this.response);

		assertEquals("test stylesheet content", this.response.getContentAsString());
	}

	@Test
	public void cachePeriod() {
		assertEquals(-1, getHandler("/resources/**").getCacheSeconds());

		this.registration.setCachePeriod(0);
		assertEquals(0, getHandler("/resources/**").getCacheSeconds());
	}

	@Test
	public void cacheControl() {
		assertThat(getHandler("/resources/**").getCacheControl(),
				Matchers.nullValue());

		this.registration.setCacheControl(CacheControl.noCache().cachePrivate());
		assertThat(getHandler("/resources/**").getCacheControl().getHeaderValue(),
				Matchers.equalTo(CacheControl.noCache().cachePrivate().getHeaderValue()));
	}

	@Test
	public void order() {
		assertEquals(Integer.MAX_VALUE -1, registry.getHandlerMapping().getOrder());

		registry.setOrder(0);
		assertEquals(0, registry.getHandlerMapping().getOrder());
	}

	@Test
	public void hasMappingForPattern() {
		assertTrue(this.registry.hasMappingForPattern("/resources/**"));
		assertFalse(this.registry.hasMappingForPattern("/whatever"));
	}

	@Test
	public void resourceChain() throws Exception {
		ResourceResolver mockResolver = Mockito.mock(ResourceResolver.class);
		ResourceTransformer mockTransformer = Mockito.mock(ResourceTransformer.class);
		this.registration.resourceChain(true).addResolver(mockResolver).addTransformer(mockTransformer);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers.toString(), resolvers, Matchers.hasSize(4));
		assertThat(resolvers.get(0), Matchers.instanceOf(CachingResourceResolver.class));
		CachingResourceResolver cachingResolver = (CachingResourceResolver) resolvers.get(0);
		assertThat(cachingResolver.getCache(), Matchers.instanceOf(ConcurrentMapCache.class));
		assertThat(resolvers.get(1), Matchers.equalTo(mockResolver));
		assertThat(resolvers.get(2), Matchers.instanceOf(WebJarsResourceResolver.class));
		assertThat(resolvers.get(3), Matchers.instanceOf(PathResourceResolver.class));

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers, Matchers.hasSize(2));
		assertThat(transformers.get(0), Matchers.instanceOf(CachingResourceTransformer.class));
		assertThat(transformers.get(1), Matchers.equalTo(mockTransformer));
	}

	@Test
	public void resourceChainWithoutCaching() throws Exception {
		this.registration.resourceChain(false);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers, Matchers.hasSize(2));
		assertThat(resolvers.get(0), Matchers.instanceOf(WebJarsResourceResolver.class));
		assertThat(resolvers.get(1), Matchers.instanceOf(PathResourceResolver.class));

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers, Matchers.hasSize(0));
	}

	@Test
	public void resourceChainWithVersionResolver() throws Exception {
		VersionResourceResolver versionResolver = new VersionResourceResolver()
				.addFixedVersionStrategy("fixed", "/**/*.js")
				.addContentVersionStrategy("/**");

		this.registration.resourceChain(true).addResolver(versionResolver)
				.addTransformer(new AppCacheManifestTransformer());

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers.toString(), resolvers, Matchers.hasSize(4));
		assertThat(resolvers.get(0), Matchers.instanceOf(CachingResourceResolver.class));
		assertThat(resolvers.get(1), Matchers.sameInstance(versionResolver));
		assertThat(resolvers.get(2), Matchers.instanceOf(WebJarsResourceResolver.class));
		assertThat(resolvers.get(3), Matchers.instanceOf(PathResourceResolver.class));

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers, Matchers.hasSize(3));
		assertThat(transformers.get(0), Matchers.instanceOf(CachingResourceTransformer.class));
		assertThat(transformers.get(1), Matchers.instanceOf(CssLinkResourceTransformer.class));
		assertThat(transformers.get(2), Matchers.instanceOf(AppCacheManifestTransformer.class));
	}

	@Test
	public void resourceChainWithOverrides() throws Exception {
		CachingResourceResolver cachingResolver = Mockito.mock(CachingResourceResolver.class);
		VersionResourceResolver versionResolver = Mockito.mock(VersionResourceResolver.class);
		WebJarsResourceResolver webjarsResolver = Mockito.mock(WebJarsResourceResolver.class);
		PathResourceResolver pathResourceResolver = new PathResourceResolver();
		CachingResourceTransformer cachingTransformer = Mockito.mock(CachingResourceTransformer.class);
		AppCacheManifestTransformer appCacheTransformer = Mockito.mock(AppCacheManifestTransformer.class);
		CssLinkResourceTransformer cssLinkTransformer = new CssLinkResourceTransformer();

		this.registration.setCachePeriod(3600)
				.resourceChain(false)
					.addResolver(cachingResolver)
					.addResolver(versionResolver)
					.addResolver(webjarsResolver)
					.addResolver(pathResourceResolver)
					.addTransformer(cachingTransformer)
					.addTransformer(appCacheTransformer)
					.addTransformer(cssLinkTransformer);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers.toString(), resolvers, Matchers.hasSize(4));
		assertThat(resolvers.get(0), Matchers.sameInstance(cachingResolver));
		assertThat(resolvers.get(1), Matchers.sameInstance(versionResolver));
		assertThat(resolvers.get(2), Matchers.sameInstance(webjarsResolver));
		assertThat(resolvers.get(3), Matchers.sameInstance(pathResourceResolver));

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers, Matchers.hasSize(3));
		assertThat(transformers.get(0), Matchers.sameInstance(cachingTransformer));
		assertThat(transformers.get(1), Matchers.sameInstance(appCacheTransformer));
		assertThat(transformers.get(2), Matchers.sameInstance(cssLinkTransformer));
	}

	private ResourceHttpRequestHandler getHandler(String pathPattern) {
		SimpleUrlHandlerMapping handlerMapping = (SimpleUrlHandlerMapping) this.registry.getHandlerMapping();
		return (ResourceHttpRequestHandler) handlerMapping.getUrlMap().get(pathPattern);
	}

}
