/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.config;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.resource.CachingResourceResolver;
import org.springframework.web.reactive.resource.CachingResourceTransformer;
import org.springframework.web.reactive.resource.CssLinkResourceTransformer;
import org.springframework.web.reactive.resource.LiteWebJarsResourceResolver;
import org.springframework.web.reactive.resource.PathResourceResolver;
import org.springframework.web.reactive.resource.ResourceResolver;
import org.springframework.web.reactive.resource.ResourceTransformer;
import org.springframework.web.reactive.resource.ResourceTransformerSupport;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.resource.ResourceWebHandler;
import org.springframework.web.reactive.resource.VersionResourceResolver;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.pattern.PathPatternParser;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResourceHandlerRegistry}.
 *
 * @author Rossen Stoyanchev
 */
class ResourceHandlerRegistryTests {

	private ResourceHandlerRegistry registry;

	private ResourceHandlerRegistration registration;


	@BeforeEach
	void setup() {
		this.registry = new ResourceHandlerRegistry(new GenericApplicationContext());
		this.registration = this.registry.addResourceHandler("/resources/**");
		this.registration.addResourceLocations("classpath:org/springframework/web/reactive/config/");
	}


	@Test
	void noResourceHandlers() {
		this.registry = new ResourceHandlerRegistry(new GenericApplicationContext());
		assertThat(this.registry.getHandlerMapping()).isNull();
	}

	@Test
	void mapPathToLocation() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
		exchange.getAttributes().put(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
				PathContainer.parsePath("/testStylesheet.css"));
		exchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
				new PathPatternParser().parse("/**"));

		ResourceWebHandler handler = getHandler("/resources/**");
		handler.handle(exchange).block(Duration.ofSeconds(5));

		StepVerifier.create(exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertThat(buf.toString(UTF_8)).isEqualTo("test stylesheet content"))
				.expectComplete()
				.verify();
	}

	@Test
	void cacheControl() {
		assertThat(getHandler("/resources/**").getCacheControl()).isNull();

		this.registration.setCacheControl(CacheControl.noCache().cachePrivate());
		assertThat(getHandler("/resources/**").getCacheControl().getHeaderValue())
				.isEqualTo(CacheControl.noCache().cachePrivate().getHeaderValue());
	}

	@Test
	void mediaTypes() {
		MediaType mediaType = MediaType.parseMediaType("foo/bar");
		this.registration.setMediaTypes(Collections.singletonMap("bar", mediaType));
		ResourceWebHandler requestHandler = this.registration.getRequestHandler();

		assertThat(requestHandler.getMediaTypes()).hasSize(1);
		assertThat(requestHandler.getMediaTypes()).containsEntry("bar", mediaType);
	}

	@Test
	void order() {
		assertThat(this.registry.getHandlerMapping().getOrder()).isEqualTo(Integer.MAX_VALUE -1);

		this.registry.setOrder(0);
		assertThat(this.registry.getHandlerMapping().getOrder()).isEqualTo(0);
	}

	@Test
	void hasMappingForPattern() {
		assertThat(this.registry.hasMappingForPattern("/resources/**")).isTrue();
		assertThat(this.registry.hasMappingForPattern("/whatever")).isFalse();
	}

	@Test
	void resourceChain() {
		ResourceUrlProvider resourceUrlProvider = mock();
		this.registry.setResourceUrlProvider(resourceUrlProvider);
		ResourceResolver mockResolver = mock();
		ResourceTransformerSupport mockTransformer = mock();

		this.registration.resourceChain(true).addResolver(mockResolver).addTransformer(mockTransformer);

		ResourceWebHandler handler = getHandler("/resources/**");
		assertThat(handler.getResourceResolvers()).satisfiesExactly(
				zero -> assertThat(zero).isInstanceOfSatisfying(CachingResourceResolver.class,
						cachingResolver -> assertThat(cachingResolver.getCache()).isInstanceOf(ConcurrentMapCache.class)),
				one -> assertThat(one).isEqualTo(mockResolver),
				two -> assertThat(two).isInstanceOf(LiteWebJarsResourceResolver.class),
				three -> assertThat(three).isInstanceOf(PathResourceResolver.class));
		assertThat(handler.getResourceTransformers()).satisfiesExactly(
				zero -> assertThat(zero).isInstanceOf(CachingResourceTransformer.class),
				one -> assertThat(one).isEqualTo(mockTransformer));
		Mockito.verify(mockTransformer).setResourceUrlProvider(resourceUrlProvider);
	}

	@Test
	void resourceChainWithoutCaching() {
		this.registration.resourceChain(false);

		ResourceWebHandler handler = getHandler("/resources/**");
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

		ResourceWebHandler handler = getHandler("/resources/**");
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

		this.registration.setCacheControl(CacheControl.maxAge(3600, TimeUnit.MILLISECONDS))
				.resourceChain(false)
					.addResolver(cachingResolver)
					.addResolver(versionResolver)
					.addResolver(webjarsResolver)
					.addResolver(pathResourceResolver)
					.addTransformer(cachingTransformer)
					.addTransformer(cssLinkTransformer);

		ResourceWebHandler handler = getHandler("/resources/**");
		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers).containsExactly(
				cachingResolver, versionResolver, webjarsResolver, pathResourceResolver);

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers).containsExactly(cachingTransformer, cssLinkTransformer);
	}

	@Test
	void ignoreLastModified() {
		this.registration.setUseLastModified(false);
		assertThat(getHandler("/resources/**").isUseLastModified()).isFalse();
	}


	private ResourceWebHandler getHandler(String pathPattern) {
		SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) this.registry.getHandlerMapping();
		return (ResourceWebHandler) mapping.getUrlMap().get(pathPattern);
	}

}
