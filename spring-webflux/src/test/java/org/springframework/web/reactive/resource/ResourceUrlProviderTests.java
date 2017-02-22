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

package org.springframework.web.reactive.resource;

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
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.*;


/**
 * Unit tests for {@link ResourceUrlProvider}.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceUrlProviderTests {

	private final List<Resource> locations = new ArrayList<>();

	private final ResourceWebHandler handler = new ResourceWebHandler();

	private final Map<String, ResourceWebHandler> handlerMap = new HashMap<>();

	private final ResourceUrlProvider urlProvider = new ResourceUrlProvider();


	@Before
	public void setup() throws Exception {
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
		this.handler.setLocations(locations);
		this.handler.afterPropertiesSet();
		this.handlerMap.put("/resources/**", this.handler);
		this.urlProvider.setHandlerMap(this.handlerMap);
	}


	@Test
	public void getStaticResourceUrl() {
		String url = this.urlProvider.getForLookupPath("/resources/foo.css").blockMillis(5000);
		assertEquals("/resources/foo.css", url);
	}

	@Test  // SPR-13374
	public void getStaticResourceUrlRequestWithQueryOrHash() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, response);

		String url = "/resources/foo.css?foo=bar&url=http://example.org";
		String resolvedUrl = this.urlProvider.getForRequestUrl(exchange, url).blockMillis(5000);
		assertEquals(url, resolvedUrl);

		url = "/resources/foo.css#hash";
		resolvedUrl = this.urlProvider.getForRequestUrl(exchange, url).blockMillis(5000);
		assertEquals(url, resolvedUrl);
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

		String url = this.urlProvider.getForLookupPath("/resources/foo.css").blockMillis(5000);
		assertEquals("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", url);
	}

	@Test  // SPR-12647
	public void bestPatternMatch() throws Exception {
		ResourceWebHandler otherHandler = new ResourceWebHandler();
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

		String url = this.urlProvider.getForLookupPath("/resources/foo.css").blockMillis(5000);
		assertEquals("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", url);
	}

	@Test  // SPR-12592
	public void initializeOnce() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(HandlerMappingConfiguration.class);
		context.refresh();

		ResourceUrlProvider urlProviderBean = context.getBean(ResourceUrlProvider.class);
		assertThat(urlProviderBean.getHandlerMap(), Matchers.hasKey("/resources/**"));
		assertFalse(urlProviderBean.isAutodetect());
	}


	@Configuration
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class HandlerMappingConfiguration {

		@Bean
		public SimpleUrlHandlerMapping simpleUrlHandlerMapping() {
			ResourceWebHandler handler = new ResourceWebHandler();
			HashMap<String, ResourceWebHandler> handlerMap = new HashMap<>();
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
