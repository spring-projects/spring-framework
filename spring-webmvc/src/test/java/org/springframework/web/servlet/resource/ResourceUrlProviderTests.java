/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ResourceUrlProvider}.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class ResourceUrlProviderTests {

	private final List<Resource> locations = new ArrayList<>();

	private final ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();

	private final Map<String, ResourceHttpRequestHandler> handlerMap = new HashMap<>();

	private final ResourceUrlProvider urlProvider = new ResourceUrlProvider();


	@BeforeEach
	void setUp() throws Exception {
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
		this.handler.setServletContext(new MockServletContext());
		this.handler.setLocations(locations);
		this.handler.afterPropertiesSet();
		this.handlerMap.put("/resources/**", this.handler);
		this.urlProvider.setHandlerMap(this.handlerMap);
	}


	@Test
	void getStaticResourceUrl() {
		String url = this.urlProvider.getForLookupPath("/resources/foo.css");
		assertThat(url).isEqualTo("/resources/foo.css");
	}

	@Test // SPR-13374
	void getStaticResourceUrlRequestWithQueryOrHash() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContextPath("/");
		request.setRequestURI("/");

		String url = "/resources/foo.css?foo=bar&url=https://example.org";
		String resolvedUrl = this.urlProvider.getForRequestUrl(request, url);
		assertThat(resolvedUrl).isEqualTo("/resources/foo.css?foo=bar&url=https://example.org");

		url = "/resources/foo.css#hash";
		resolvedUrl = this.urlProvider.getForRequestUrl(request, url);
		assertThat(resolvedUrl).isEqualTo("/resources/foo.css#hash");
	}

	@Test // SPR-16526
	void getStaticResourceWithMissingContextPath() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContextPath("/contextpath-longer-than-request-path");
		request.setRequestURI("/contextpath-longer-than-request-path/style.css");
		String url = "/resources/foo.css";
		String resolvedUrl = this.urlProvider.getForRequestUrl(request, url);
		assertThat((Object) resolvedUrl).isNull();
	}

	@Test
	void getFingerprintedResourceUrl() {
		Map<String, VersionStrategy> versionStrategyMap = new HashMap<>();
		versionStrategyMap.put("/**", new ContentVersionStrategy());
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(versionStrategyMap);

		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		this.handler.setResourceResolvers(resolvers);

		String url = this.urlProvider.getForLookupPath("/resources/foo.css");
		assertThat(url).isEqualTo("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css");
	}

	@Test // SPR-12647
	void bestPatternMatch() throws Exception {
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
		assertThat(url).isEqualTo("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css");
	}

	@Test // SPR-12592
	@SuppressWarnings("resource")
	void initializeOnce() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(HandlerMappingConfiguration.class);
		context.refresh();

		ResourceUrlProvider urlProviderBean = context.getBean(ResourceUrlProvider.class);
		assertThat(urlProviderBean.getHandlerMap()).containsKey("/resources/**");
		assertThat(urlProviderBean.isAutodetect()).isFalse();
	}

	@Test
	@SuppressWarnings("resource")
	void initializeOnCurrentContext() {
		AnnotationConfigWebApplicationContext parentContext = new AnnotationConfigWebApplicationContext();
		parentContext.setServletContext(new MockServletContext());
		parentContext.register(ParentHandlerMappingConfiguration.class);

		AnnotationConfigWebApplicationContext childContext = new AnnotationConfigWebApplicationContext();
		childContext.setParent(parentContext);
		childContext.setServletContext(new MockServletContext());
		childContext.register(HandlerMappingConfiguration.class);

		parentContext.refresh();
		childContext.refresh();

		ResourceUrlProvider parentUrlProvider = parentContext.getBean(ResourceUrlProvider.class);
		assertThat(parentUrlProvider.getHandlerMap()).isEmpty();
		assertThat(parentUrlProvider.isAutodetect()).isTrue();
		ResourceUrlProvider childUrlProvider = childContext.getBean(ResourceUrlProvider.class);
		assertThat(childUrlProvider.getHandlerMap()).containsOnlyKeys("/resources/**");
		assertThat(childUrlProvider.isAutodetect()).isFalse();
	}

	@Test // SPR-16296
	void getForLookupPathShouldNotFailIfPathContainsDoubleSlashes() {
		// given
		ResourceResolver mockResourceResolver = mock();
		given(mockResourceResolver.resolveUrlPath(any(), any(), any())).willReturn("some-path");

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.getResourceResolvers().add(mockResourceResolver);

		ResourceUrlProvider provider = new ResourceUrlProvider();
		provider.getHandlerMap().put("/some-pattern/**", handler);

		// when
		String lookupForPath = provider.getForLookupPath("/some-pattern/some-lib//some-resource");

		// then
		assertThat(lookupForPath).isEqualTo("/some-pattern/some-path");
	}


	@Configuration
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class HandlerMappingConfiguration {

		@Bean
		public SimpleUrlHandlerMapping simpleUrlHandlerMapping() {
			return new SimpleUrlHandlerMapping(
				Collections.singletonMap("/resources/**", new ResourceHttpRequestHandler()));
		}

		@Bean
		public ResourceUrlProvider resourceUrlProvider() {
			return new ResourceUrlProvider();
		}
	}

	@Configuration
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class ParentHandlerMappingConfiguration {

		@Bean
		public ResourceUrlProvider resourceUrlProvider() {
			return new ResourceUrlProvider();
		}
	}

}
