/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.CacheControl;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.resource.AppCacheManifestTransformer;
import org.springframework.web.reactive.resource.CachingResourceResolver;
import org.springframework.web.reactive.resource.CachingResourceTransformer;
import org.springframework.web.reactive.resource.CssLinkResourceTransformer;
import org.springframework.web.reactive.resource.PathResourceResolver;
import org.springframework.web.reactive.resource.ResourceResolver;
import org.springframework.web.reactive.resource.ResourceTransformer;
import org.springframework.web.reactive.resource.ResourceWebHandler;
import org.springframework.web.reactive.resource.VersionResourceResolver;
import org.springframework.web.reactive.resource.WebJarsResourceResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ResourceHandlerRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceHandlerRegistryTests {

	private ResourceHandlerRegistry registry;

	private ResourceHandlerRegistration registration;

	private ServerWebExchange exchange;

	private MockServerHttpResponse response;


	@Before
	public void setup() {
		this.registry = new ResourceHandlerRegistry(new GenericApplicationContext());
		this.registration = this.registry.addResourceHandler("/resources/**");
		this.registration.addResourceLocations("classpath:org/springframework/web/reactive/config/");

		MockServerHttpRequest request = MockServerHttpRequest.get("").build();
		this.response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, this.response);
	}


	@Test
	public void noResourceHandlers() throws Exception {
		this.registry = new ResourceHandlerRegistry(new GenericApplicationContext());
		assertNull(this.registry.getHandlerMapping());
	}

	@Test
	public void mapPathToLocation() throws Exception {
		this.exchange.getAttributes().put(
				HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/testStylesheet.css");

		ResourceWebHandler handler = getHandler("/resources/**");
		handler.handle(this.exchange).blockMillis(5000);

		StepVerifier.create(this.response.getBody())
				.consumeNextWith(buf -> assertEquals("test stylesheet content",
						DataBufferTestUtils.dumpString(buf, StandardCharsets.UTF_8)))
				.expectComplete()
				.verify();
	}

	@Test
	public void cacheControl() {
		assertThat(getHandler("/resources/**").getCacheControl(), Matchers.nullValue());

		this.registration.setCacheControl(CacheControl.noCache().cachePrivate());
		assertThat(getHandler("/resources/**").getCacheControl().getHeaderValue(),
				Matchers.equalTo(CacheControl.noCache().cachePrivate().getHeaderValue()));
	}

	@Test
	public void order() {
		assertEquals(Integer.MAX_VALUE -1, this.registry.getHandlerMapping().getOrder());

		this.registry.setOrder(0);
		assertEquals(0, this.registry.getHandlerMapping().getOrder());
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

		ResourceWebHandler handler = getHandler("/resources/**");
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

		ResourceWebHandler handler = getHandler("/resources/**");
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

		ResourceWebHandler handler = getHandler("/resources/**");
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

		this.registration.setCacheControl(CacheControl.maxAge(3600, TimeUnit.MILLISECONDS))
				.resourceChain(false)
					.addResolver(cachingResolver)
					.addResolver(versionResolver)
					.addResolver(webjarsResolver)
					.addResolver(pathResourceResolver)
					.addTransformer(cachingTransformer)
					.addTransformer(appCacheTransformer)
					.addTransformer(cssLinkTransformer);

		ResourceWebHandler handler = getHandler("/resources/**");
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


	private ResourceWebHandler getHandler(String pathPattern) {
		SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) this.registry.getHandlerMapping();
		return (ResourceWebHandler) mapping.getUrlMap().get(pathPattern);
	}

}
