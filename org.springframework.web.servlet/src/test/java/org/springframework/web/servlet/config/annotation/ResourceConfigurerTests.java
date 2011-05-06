/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Test fixture with a {@link ResourceConfigurer}.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceConfigurerTests {

	private ResourceConfigurer configurer;

	private MockHttpServletResponse response;

	@Before
	public void setUp() {
		configurer = new ResourceConfigurer(new GenericWebApplicationContext(), new MockServletContext());
		configurer.addPathMapping("/resources/**");
		configurer.addResourceLocation("classpath:org/springframework/web/servlet/config/annotation/");

		response = new MockHttpServletResponse();
	}

	@Test
	public void noMappings() throws Exception {
		configurer = new ResourceConfigurer(new GenericWebApplicationContext(), new MockServletContext());
		assertTrue(configurer.getHandlerMapping().getUrlMap().isEmpty());
	}

	@Test
	public void mapPathToLocation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/testStylesheet.css");

		ResourceHttpRequestHandler handler = getResourceHandler("/resources/**");
		handler.handleRequest(request, response);

		assertEquals("test stylesheet content", response.getContentAsString());
	}

	@Test
	public void cachePeriod() {
		assertEquals(-1, getResourceHandler("/resources/**").getCacheSeconds());

		configurer.setCachePeriod(0);
		assertEquals(0, getResourceHandler("/resources/**").getCacheSeconds());
	}

	@Test
	public void order() {
		assertEquals(Integer.MAX_VALUE -1, configurer.getHandlerMapping().getOrder());

		configurer.setOrder(0);
		assertEquals(0, configurer.getHandlerMapping().getOrder());
	}

	private ResourceHttpRequestHandler getResourceHandler(String pathPattern) {
		return (ResourceHttpRequestHandler) configurer.getHandlerMapping().getUrlMap().get(pathPattern);
	}

}
