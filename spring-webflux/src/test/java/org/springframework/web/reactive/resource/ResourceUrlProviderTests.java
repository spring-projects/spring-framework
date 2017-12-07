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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


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
		this.urlProvider.registerHandlers(this.handlerMap);
	}


	@Test
	public void getStaticResourceUrl() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		String uriString = "/resources/foo.css";
		String actual = this.urlProvider.getForUriString(uriString, exchange).block(Duration.ofSeconds(5));
		assertEquals(uriString, actual);
	}

	@Test  // SPR-13374
	public void getStaticResourceUrlRequestWithQueryOrHash() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		String url = "/resources/foo.css?foo=bar&url=http://example.org";
		String resolvedUrl = this.urlProvider.getForUriString(url, exchange).block(Duration.ofSeconds(5));
		assertEquals(url, resolvedUrl);

		url = "/resources/foo.css#hash";
		resolvedUrl = this.urlProvider.getForUriString(url, exchange).block(Duration.ofSeconds(5));
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

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		String path = "/resources/foo.css";
		String url = this.urlProvider.getForUriString(path, exchange).block(Duration.ofSeconds(5));
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
		this.urlProvider.registerHandlers(this.handlerMap);

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		String path = "/resources/foo.css";
		String url = this.urlProvider.getForUriString(path, exchange).block(Duration.ofSeconds(5));
		assertEquals("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", url);
	}

	@Test  // SPR-12592
	public void initializeOnce() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(HandlerMappingConfiguration.class);
		context.refresh();

		ResourceUrlProvider urlProviderBean = context.getBean(ResourceUrlProvider.class);
		assertThat(urlProviderBean.getHandlerMap(), Matchers.hasKey(pattern("/resources/**")));
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

	private static PathPatternMatcher pattern(String pattern) {
		return new PathPatternMatcher(pattern);
	}

	private static class PathPatternMatcher extends BaseMatcher<PathPattern> {

		private final String pattern;

		public PathPatternMatcher(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean matches(Object item) {
			if (item != null && item instanceof PathPattern) {
				return ((PathPattern) item).getPatternString().equals(pattern);
			}
			return false;
		}

		@Override
		public void describeTo(Description description) {

		}
	}

}
