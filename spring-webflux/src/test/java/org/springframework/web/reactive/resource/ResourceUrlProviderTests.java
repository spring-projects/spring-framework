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

package org.springframework.web.reactive.resource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import static org.junit.Assert.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;

/**
 * Unit tests for {@link ResourceUrlProvider}.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceUrlProviderTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);


	private final List<Resource> locations = new ArrayList<>();

	private final ResourceWebHandler handler = new ResourceWebHandler();

	private final Map<String, ResourceWebHandler> handlerMap = new HashMap<>();

	private final ResourceUrlProvider urlProvider = new ResourceUrlProvider();

	private final MockServerWebExchange exchange = MockServerWebExchange.from(get("/"));


	@Before
	public void setup() throws Exception {
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
		this.handler.setLocations(this.locations);
		this.handler.afterPropertiesSet();
		this.handlerMap.put("/resources/**", this.handler);
		this.urlProvider.registerHandlers(this.handlerMap);
	}


	@Test
	public void getStaticResourceUrl() {
		String expected = "/resources/foo.css";
		String actual = this.urlProvider.getForUriString(expected, this.exchange).block(TIMEOUT);

		assertEquals(expected, actual);
	}

	@Test  // SPR-13374
	public void getStaticResourceUrlRequestWithQueryOrHash() {

		String url = "/resources/foo.css?foo=bar&url=https://example.org";
		String resolvedUrl = this.urlProvider.getForUriString(url, this.exchange).block(TIMEOUT);
		assertEquals(url, resolvedUrl);

		url = "/resources/foo.css#hash";
		resolvedUrl = this.urlProvider.getForUriString(url, this.exchange).block(TIMEOUT);
		assertEquals(url, resolvedUrl);
	}

	@Test
	public void getVersionedResourceUrl() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		this.handler.setResourceResolvers(resolvers);

		String path = "/resources/foo.css";
		String url = this.urlProvider.getForUriString(path, this.exchange).block(TIMEOUT);

		assertEquals("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", url);
	}

	@Test  // SPR-12647
	public void bestPatternMatch() {
		ResourceWebHandler otherHandler = new ResourceWebHandler();
		otherHandler.setLocations(this.locations);

		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		otherHandler.setResourceResolvers(resolvers);

		this.handlerMap.put("/resources/*.css", otherHandler);
		this.urlProvider.registerHandlers(this.handlerMap);

		String path = "/resources/foo.css";
		String url = this.urlProvider.getForUriString(path, this.exchange).block(TIMEOUT);
		assertEquals("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", url);
	}

	@Test  // SPR-12592
	@SuppressWarnings("resource")
	public void initializeOnce() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(HandlerMappingConfiguration.class);
		context.refresh();

		assertThat(context.getBean(ResourceUrlProvider.class).getHandlerMap(),
				Matchers.hasKey(pattern("/resources/**")));
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
