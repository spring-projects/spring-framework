/*
 * Copyright 2002-2021 the original author or authors.
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

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.pattern.PathPattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Unit tests for {@link ResourceUrlProvider}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class ResourceUrlProviderTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);


	private final List<Resource> locations = new ArrayList<>();

	private final ResourceWebHandler handler = new ResourceWebHandler();

	private final Map<String, ResourceWebHandler> handlerMap = new HashMap<>();

	private final ResourceUrlProvider urlProvider = new ResourceUrlProvider();

	private final MockServerWebExchange exchange = MockServerWebExchange.from(get("/"));


	@BeforeEach
	void setup() throws Exception {
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));
		this.handler.setLocations(this.locations);
		this.handler.afterPropertiesSet();
		this.handlerMap.put("/resources/**", this.handler);
		this.urlProvider.registerHandlers(this.handlerMap);
	}


	@Test
	void getStaticResourceUrl() {
		String expected = "/resources/foo.css";
		String actual = this.urlProvider.getForUriString(expected, this.exchange).block(TIMEOUT);

		assertThat(actual).isEqualTo(expected);
	}

	@Test  // SPR-13374
	void getStaticResourceUrlRequestWithQueryOrHash() {

		String url = "/resources/foo.css?foo=bar&url=https://example.org";
		String resolvedUrl = this.urlProvider.getForUriString(url, this.exchange).block(TIMEOUT);
		assertThat(resolvedUrl).isEqualTo(url);

		url = "/resources/foo.css#hash";
		resolvedUrl = this.urlProvider.getForUriString(url, this.exchange).block(TIMEOUT);
		assertThat(resolvedUrl).isEqualTo(url);
	}

	@Test
	void getVersionedResourceUrl() {
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		List<ResourceResolver> resolvers = new ArrayList<>();
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		this.handler.setResourceResolvers(resolvers);

		String path = "/resources/foo.css";
		String url = this.urlProvider.getForUriString(path, this.exchange).block(TIMEOUT);

		assertThat(url).isEqualTo("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css");
	}

	@Test  // SPR-12647
	void bestPatternMatch() {
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
		assertThat(url).isEqualTo("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css");
	}

	@Test  // SPR-12592
	@SuppressWarnings("resource")
	void initializeOnce() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(HandlerMappingConfiguration.class);
		context.refresh();

		assertThat(context.getBean(ResourceUrlProvider.class).getHandlerMap())
				.hasKeySatisfying(pathPatternStringOf("/resources/**"));
	}

	@Test
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
		ResourceUrlProvider childUrlProvider = childContext.getBean(ResourceUrlProvider.class);
		assertThat(childUrlProvider.getHandlerMap()).hasKeySatisfying(pathPatternStringOf("/resources/**"));
		childContext.close();
		parentContext.close();
	}


	private Condition<PathPattern> pathPatternStringOf(String expected) {
		return new Condition<PathPattern>(
				actual -> actual != null && actual.getPatternString().equals(expected),
				"Pattern %s", expected);
	}

	@Configuration
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class HandlerMappingConfiguration {

		@Bean
		public SimpleUrlHandlerMapping simpleUrlHandlerMapping() {
			return new SimpleUrlHandlerMapping(Collections.singletonMap("/resources/**", new ResourceWebHandler()));
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
