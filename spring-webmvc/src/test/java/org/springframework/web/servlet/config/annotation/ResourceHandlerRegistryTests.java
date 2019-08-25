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

package org.springframework.web.servlet.config.annotation;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.accept.ContentNegotiationManager;
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
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ResourceHandlerRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceHandlerRegistryTests {

	private ResourceHandlerRegistry registry;

	private ResourceHandlerRegistration registration;

	private MockHttpServletResponse response;


	@BeforeEach
	public void setUp() {
		GenericWebApplicationContext appContext = new GenericWebApplicationContext();
		appContext.refresh();

		this.registry = new ResourceHandlerRegistry(appContext, new MockServletContext(),
				new ContentNegotiationManager(), new UrlPathHelper());

		this.registration = this.registry.addResourceHandler("/resources/**");
		this.registration.addResourceLocations("classpath:org/springframework/web/servlet/config/annotation/");
		this.response = new MockHttpServletResponse();
	}

	@Test
	public void noResourceHandlers() throws Exception {
		this.registry = new ResourceHandlerRegistry(new GenericWebApplicationContext(), new MockServletContext());
		assertThat((Object) this.registry.getHandlerMapping()).isNull();
	}

	@Test
	public void mapPathToLocation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/testStylesheet.css");

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		handler.handleRequest(request, this.response);

		assertThat(this.response.getContentAsString()).isEqualTo("test stylesheet content");
	}

	@Test
	public void cachePeriod() {
		assertThat(getHandler("/resources/**").getCacheSeconds()).isEqualTo(-1);

		this.registration.setCachePeriod(0);
		assertThat(getHandler("/resources/**").getCacheSeconds()).isEqualTo(0);
	}

	@Test
	public void cacheControl() {
		assertThat(getHandler("/resources/**").getCacheControl()).isNull();

		this.registration.setCacheControl(CacheControl.noCache().cachePrivate());
		assertThat(getHandler("/resources/**").getCacheControl().getHeaderValue())
				.isEqualTo(CacheControl.noCache().cachePrivate().getHeaderValue());
	}

	@Test
	public void order() {
		assertThat(registry.getHandlerMapping().getOrder()).isEqualTo(Integer.MAX_VALUE -1);

		registry.setOrder(0);
		assertThat(registry.getHandlerMapping().getOrder()).isEqualTo(0);
	}

	@Test
	public void hasMappingForPattern() {
		assertThat(this.registry.hasMappingForPattern("/resources/**")).isTrue();
		assertThat(this.registry.hasMappingForPattern("/whatever")).isFalse();
	}

	@Test
	public void resourceChain() throws Exception {
		ResourceResolver mockResolver = Mockito.mock(ResourceResolver.class);
		ResourceTransformer mockTransformer = Mockito.mock(ResourceTransformer.class);
		this.registration.resourceChain(true).addResolver(mockResolver).addTransformer(mockTransformer);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers).hasSize(4);
		assertThat(resolvers.get(0)).isInstanceOf(CachingResourceResolver.class);
		CachingResourceResolver cachingResolver = (CachingResourceResolver) resolvers.get(0);
		assertThat(cachingResolver.getCache()).isInstanceOf(ConcurrentMapCache.class);
		assertThat(resolvers.get(1)).isEqualTo(mockResolver);
		assertThat(resolvers.get(2)).isInstanceOf(WebJarsResourceResolver.class);
		assertThat(resolvers.get(3)).isInstanceOf(PathResourceResolver.class);

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers).hasSize(2);
		assertThat(transformers.get(0)).isInstanceOf(CachingResourceTransformer.class);
		assertThat(transformers.get(1)).isEqualTo(mockTransformer);
	}

	@Test
	public void resourceChainWithoutCaching() throws Exception {
		this.registration.resourceChain(false);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers).hasSize(2);
		assertThat(resolvers.get(0)).isInstanceOf(WebJarsResourceResolver.class);
		assertThat(resolvers.get(1)).isInstanceOf(PathResourceResolver.class);

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers).isEmpty();
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
		assertThat(resolvers).hasSize(4);
		assertThat(resolvers.get(0)).isInstanceOf(CachingResourceResolver.class);
		assertThat(resolvers.get(1)).isSameAs(versionResolver);
		assertThat(resolvers.get(2)).isInstanceOf(WebJarsResourceResolver.class);
		assertThat(resolvers.get(3)).isInstanceOf(PathResourceResolver.class);

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers).hasSize(3);
		assertThat(transformers.get(0)).isInstanceOf(CachingResourceTransformer.class);
		assertThat(transformers.get(1)).isInstanceOf(CssLinkResourceTransformer.class);
		assertThat(transformers.get(2)).isInstanceOf(AppCacheManifestTransformer.class);
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
		assertThat(resolvers).hasSize(4);
		assertThat(resolvers.get(0)).isSameAs(cachingResolver);
		assertThat(resolvers.get(1)).isSameAs(versionResolver);
		assertThat(resolvers.get(2)).isSameAs(webjarsResolver);
		assertThat(resolvers.get(3)).isSameAs(pathResourceResolver);

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers).hasSize(3);
		assertThat(transformers.get(0)).isSameAs(cachingTransformer);
		assertThat(transformers.get(1)).isSameAs(appCacheTransformer);
		assertThat(transformers.get(2)).isSameAs(cssLinkTransformer);
	}

	@Test
	public void urlResourceWithCharset() throws Exception {
		this.registration.addResourceLocations("[charset=ISO-8859-1]file:///tmp");
		this.registration.resourceChain(true);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		UrlResource resource = (UrlResource) handler.getLocations().get(1);
		assertThat(resource.getURL().toString()).isEqualTo("file:/tmp");
		assertThat(handler.getUrlPathHelper()).isNotNull();

		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		PathResourceResolver resolver = (PathResourceResolver) resolvers.get(resolvers.size()-1);
		Map<Resource, Charset> locationCharsets = resolver.getLocationCharsets();
		assertThat(locationCharsets.size()).isEqualTo(1);
		assertThat(locationCharsets.values().iterator().next()).isEqualTo(StandardCharsets.ISO_8859_1);
	}

	private ResourceHttpRequestHandler getHandler(String pathPattern) {
		SimpleUrlHandlerMapping hm = (SimpleUrlHandlerMapping) this.registry.getHandlerMapping();
		return (ResourceHttpRequestHandler) hm.getUrlMap().get(pathPattern);
	}

}
