/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Test fixture with a {@link ResourceHandlerRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceHandlerRegistryTests {

	private ResourceHandlerRegistry registry;

	private ResourceHandlerRegistration registration;

	private MockHttpServletResponse response;

	@Before
	public void setUp() {
		registry = new ResourceHandlerRegistry(new GenericWebApplicationContext(), new MockServletContext());
		registration = registry.addResourceHandler("/resources/**");
		registration.addResourceLocations("classpath:org/springframework/web/servlet/config/annotation/");
		response = new MockHttpServletResponse();
	}

	@Test
	public void noResourceHandlers() throws Exception {
		registry = new ResourceHandlerRegistry(new GenericWebApplicationContext(), new MockServletContext());
		assertNull(registry.getHandlerMapping());
	}

	@Test
	public void mapPathToLocation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/testStylesheet.css");

		ResourceHttpRequestHandler handler = getHandler("/resources/**");
		handler.handleRequest(request, response);

		assertEquals("test stylesheet content", response.getContentAsString());
	}

	@Test
	public void cachePeriod() {
		assertEquals(-1, getHandler("/resources/**").getCacheSeconds());

		registration.setCachePeriod(0);
		assertEquals(0, getHandler("/resources/**").getCacheSeconds());
	}

	@Test
	public void order() {
		assertEquals(Integer.MAX_VALUE -1, registry.getHandlerMapping().getOrder());

		registry.setOrder(0);
		assertEquals(0, registry.getHandlerMapping().getOrder());
	}

	private ResourceHttpRequestHandler getHandler(String pathPattern) {
		SimpleUrlHandlerMapping handlerMapping = (SimpleUrlHandlerMapping) registry.getHandlerMapping();
		return (ResourceHttpRequestHandler) handlerMapping.getUrlMap().get(pathPattern);
	}

}
