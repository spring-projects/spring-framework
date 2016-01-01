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

package org.springframework.web.servlet.resource;

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
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import static org.junit.Assert.*;


/**
 * Unit tests for {@link ResourceUrlProvider}.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 */
public class ResourceUrlProviderTests {

	private List<Resource> locations;

	private ResourceUrlProvider translator;

	private ResourceHttpRequestHandler handler;

	private Map<String, ResourceHttpRequestHandler> handlerMap;


	@Before
	public void setUp() {
		this.locations = new ArrayList<Resource>();
		this.locations.add(new ClassPathResource("test/", getClass()));
		this.locations.add(new ClassPathResource("testalternatepath/", getClass()));

		this.handler = new ResourceHttpRequestHandler();
		this.handler.setLocations(locations);

		this.handlerMap = new HashMap<String, ResourceHttpRequestHandler>();
		this.handlerMap.put("/resources/**", this.handler);
	}

	@Test
	public void getStaticResourceUrl() {
		initTranslator();

		String url = this.translator.getForLookupPath("/resources/foo.css");
		assertEquals("/resources/foo.css", url);
	}

	// SPR-13374
	@Test
	public void getStaticResourceUrlRequestWithRequestParams() {
		initTranslator();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContextPath("/");
		request.setRequestURI("/");

		String url = this.translator.getForRequestUrl(request, "/resources/foo.css?foo=bar&url=http://example.org");
		assertEquals("/resources/foo.css?foo=bar&url=http://example.org", url);
	}

	@Test
	public void getFingerprintedResourceUrl() {
		Map<String, VersionStrategy> versionStrategyMap = new HashMap<>();
		versionStrategyMap.put("/**", new ContentVersionStrategy());
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(versionStrategyMap);

		List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		this.handler.setResourceResolvers(resolvers);
		initTranslator();

		String url = this.translator.getForLookupPath("/resources/foo.css");
		assertEquals("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", url);
	}

	private void initTranslator() {
		this.translator = new ResourceUrlProvider();
		this.translator.setHandlerMap(this.handlerMap);
	}

	// SPR-12647
	@Test
	public void bestPatternMatch() throws Exception {
		ResourceHttpRequestHandler otherHandler = new ResourceHttpRequestHandler();
		otherHandler.setLocations(this.locations);
		Map<String, VersionStrategy> versionStrategyMap = new HashMap<>();
		versionStrategyMap.put("/**", new ContentVersionStrategy());
		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(versionStrategyMap);

		List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();
		resolvers.add(versionResolver);
		resolvers.add(new PathResourceResolver());
		otherHandler.setResourceResolvers(resolvers);

		this.handlerMap.put("/resources/*.css", otherHandler);
		initTranslator();

		String url = this.translator.getForLookupPath("/resources/foo.css");
		assertEquals("/resources/foo-e36d2e05253c6c7085a91522ce43a0b4.css", url);
	}

	// SPR-12592
	@Test
	public void initializeOnce() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(HandlerMappingConfiguration.class);
		context.refresh();
		ResourceUrlProvider translator = context.getBean(ResourceUrlProvider.class);
		assertThat(translator.getHandlerMap(), Matchers.hasKey("/resources/**"));
		assertFalse(translator.isAutodetect());
	}

	@Configuration
	public static class HandlerMappingConfiguration {
		@Bean
		public SimpleUrlHandlerMapping simpleUrlHandlerMapping() {
			ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
			HashMap<String, ResourceHttpRequestHandler> handlerMap = new HashMap<String, ResourceHttpRequestHandler>();
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
