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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Test fixture with a {@link ViewControllerRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class ViewControllerRegistryTests {

	private ViewControllerRegistry registry;

	@Before
	public void setUp() {
		registry = new ViewControllerRegistry();
	}

	@Test
	public void noViewControllers() throws Exception {
		assertNull(registry.getHandlerMapping());
	}

	@Test
	public void addViewController() {
		registry.addViewController("/path");
		Map<String, ?> urlMap = getHandlerMapping().getUrlMap();
		ParameterizableViewController controller = (ParameterizableViewController) urlMap.get("/path");
		assertNotNull(controller);
		assertNull(controller.getViewName());
	}

	@Test
	public void addViewControllerWithViewName() {
		registry.addViewController("/path").setViewName("viewName");
		Map<String, ?> urlMap = getHandlerMapping().getUrlMap();
		ParameterizableViewController controller = (ParameterizableViewController) urlMap.get("/path");
		assertNotNull(controller);
		assertEquals("viewName", controller.getViewName());
	}

	@Test
	public void order() {
		registry.addViewController("/path");
		SimpleUrlHandlerMapping handlerMapping = getHandlerMapping();
		assertEquals(1, handlerMapping.getOrder());

		registry.setOrder(2);
		handlerMapping = getHandlerMapping();
		assertEquals(2, handlerMapping.getOrder());
	}

	private SimpleUrlHandlerMapping getHandlerMapping() {
		return (SimpleUrlHandlerMapping) registry.getHandlerMapping();
	}

}
