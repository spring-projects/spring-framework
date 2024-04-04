/*
 * Copyright 2002-2024 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.http.CacheControl;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.CachingResourceResolver;
import org.springframework.web.servlet.resource.CachingResourceTransformer;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.LiteWebJarsResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResourceHandlerRegistry}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class ResourceHandlerRegistryTests {

	private ResourceHandlerRegistry registry;

	private ResourceHandlerRegistration registration;

	private MockHttpServletResponse response;


	@BeforeEach
	void setup() {
		GenericWebApplicationContext appContext = new GenericWebApplicationContext();
		appContext.refresh();

		this.registry = new ResourceHandlerRegistry(appContext, new MockServletContext(),
				new ContentNegotiationManager(), new UrlPathHelper());

		this.registration = this.registry.addResourceHandler("/resources/**");
		this.registration.addResourceLocations("classpath:org/springframework/web/servlet/config/annotation/");
		this.response = new MockHttpServletResponse();
	}

	private ResourceHttpRequestHandler getHandler(String pathPattern) {
		SimpleUrlHandlerMapping hm = (SimpleUrlHandlerMapping) this.registry.getHandlerMapping();
		return (ResourceHttpRequestHandler) hm.getUrlMap().get(pathPattern);
	}


	@Test
	void noResourceHandlers() {
		this.registry = new ResourceHandlerRegistry(new GenericWebApplicationContext(), new MockServletContext());
		assertThat(this.registry.getHandlerMapping()).isNull();
	}

	@Test
	void mapPathToLocation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/testStylesheet.css");

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		handler.handleRequest(request, this.response);

		assertThat(this.response.getContentAsString()).isEqualTo("test stylesheet content");
	}

	@Test
	void cachePeriod() {
		assertThat(getHandler("/resources/**").getCacheSeconds()).isEqualTo(-1);

		this.registration.setCachePeriod(0);
		assertThat(getHandler("/resources/**").getCacheSeconds()).isEqualTo(0);
	}

	@Test
	void cacheControl() {
		assertThat(getHandler("/resources/**").getCacheControl()).isNull();

		this.registration.setCacheControl(CacheControl.noCache().cachePrivate());
		assertThat(getHandler("/resources/**").getCacheControl().getHeaderValue())
				.isEqualTo(CacheControl.noCache().cachePrivate().getHeaderValue());
	}

	@Test
	void order() {
		assertThat(registry.getHandlerMapping().getOrder()).isEqualTo(Integer.MAX_VALUE -1);

		registry.setOrder(0);
		assertThat(registry.getHandlerMapping().getOrder()).isEqualTo(0);
	}

	@Test
	void hasMappingForPattern() {
		assertThat(this.registry.hasMappingForPattern("/resources/**")).isTrue();
		assertThat(this.registry.hasMappingForPattern("/whatever")).isFalse();
	}

	@Test
	@SuppressWarnings("removal")
	void resourceChain() {
		ResourceResolver mockResolver = mock();
		ResourceTransformer mockTransformer = mock();
		this.registration.resourceChain(true).addResolver(mockResolver).addTransformer(mockTransformer);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		assertThat(handler.getResourceResolvers()).satisfiesExactly(
				zero -> assertThat(zero).isInstanceOfSatisfying(CachingResourceResolver.class,
						cachingResolver -> assertThat(cachingResolver.getCache()).isInstanceOf(ConcurrentMapCache.class)),
				one -> assertThat(one).isEqualTo(mockResolver),
				two -> assertThat(two).isInstanceOf(LiteWebJarsResourceResolver.class),
				three -> assertThat(three).isInstanceOf(PathResourceResolver.class));
		assertThat(handler.getResourceTransformers()).satisfiesExactly(
				zero -> assertThat(zero).isInstanceOf(CachingResourceTransformer.class),
				one -> assertThat(one).isEqualTo(mockTransformer));
	}

	@Test
	void resourceChainWithoutCaching() {
		this.registration.resourceChain(false);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		assertThat(handler.getResourceResolvers()).hasExactlyElementsOfTypes(
				LiteWebJarsResourceResolver.class, PathResourceResolver.class);
		assertThat(handler.getResourceTransformers()).isEmpty();
	}

	@Test
	void resourceChainWithVersionResolver() {
		VersionResourceResolver versionResolver = new VersionResourceResolver()
				.addFixedVersionStrategy("fixed", "/**/*.js")
				.addContentVersionStrategy("/**");

		this.registration.resourceChain(true).addResolver(versionResolver);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		assertThat(handler.getResourceResolvers()).satisfiesExactly(
				zero -> assertThat(zero).isInstanceOf(CachingResourceResolver.class),
				one -> assertThat(one).isSameAs(versionResolver),
				two -> assertThat(two).isInstanceOf(LiteWebJarsResourceResolver.class),
				three -> assertThat(three).isInstanceOf(PathResourceResolver.class));
		assertThat(handler.getResourceTransformers()).hasExactlyElementsOfTypes(
				CachingResourceTransformer.class, CssLinkResourceTransformer.class);
	}

	@Test
	void resourceChainWithOverrides() {
		CachingResourceResolver cachingResolver = mock();
		VersionResourceResolver versionResolver = mock();
		LiteWebJarsResourceResolver webjarsResolver = mock();
		PathResourceResolver pathResourceResolver = new PathResourceResolver();
		CachingResourceTransformer cachingTransformer = mock();
		CssLinkResourceTransformer cssLinkTransformer = new CssLinkResourceTransformer();

		this.registration.setCachePeriod(3600)
				.resourceChain(false)
					.addResolver(cachingResolver)
					.addResolver(versionResolver)
					.addResolver(webjarsResolver)
					.addResolver(pathResourceResolver)
					.addTransformer(cachingTransformer)
					.addTransformer(cssLinkTransformer);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers).containsExactly(
				cachingResolver, versionResolver, webjarsResolver, pathResourceResolver);

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers).containsExactly(cachingTransformer, cssLinkTransformer);
	}

	@Test
	void urlResourceWithCharset() {
		this.registration.addResourceLocations("[charset=ISO-8859-1]file:///tmp");
		this.registration.resourceChain(true);

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		assertThat(handler.getUrlPathHelper()).isNotNull();

		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		PathResourceResolver resolver = (PathResourceResolver) resolvers.get(resolvers.size() - 1);
		assertThat(resolver.getLocationCharsets()).hasSize(1).containsValue(StandardCharsets.ISO_8859_1);
	}

	@Test
	void lastModifiedDisabled() {
		this.registration.setUseLastModified(false);
		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		assertThat(handler.isUseLastModified()).isFalse();
	}

}
