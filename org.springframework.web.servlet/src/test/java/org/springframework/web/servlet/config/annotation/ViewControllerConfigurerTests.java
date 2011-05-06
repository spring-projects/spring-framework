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

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Test fixture with a {@link ViewControllerConfigurer}.
 *
 * @author Rossen Stoyanchev
 */
public class ViewControllerConfigurerTests {

	private ViewControllerConfigurer configurer;

	@Before
	public void setUp() {
		configurer = new ViewControllerConfigurer();
	}

	@Test
	public void noMappings() throws Exception {
		Map<String, ?> urlMap = configurer.getHandlerMapping().getUrlMap();
		assertTrue(urlMap.isEmpty());
	}

	@Test
	public void mapViewName() {
		configurer.mapViewName("/path", "viewName");
		Map<String, ?> urlMap = configurer.getHandlerMapping().getUrlMap();
		ParameterizableViewController controller = (ParameterizableViewController) urlMap.get("/path");
		assertNotNull(controller);
		assertEquals("viewName", controller.getViewName());
	}

	@Test
	public void mapViewNameByConvention() {
		configurer.mapViewNameByConvention("/path");
		Map<String, ?> urlMap = configurer.getHandlerMapping().getUrlMap();
		ParameterizableViewController controller = (ParameterizableViewController) urlMap.get("/path");
		assertNotNull(controller);
		assertNull(controller.getViewName());
	}

	@Test
	public void order() {
		SimpleUrlHandlerMapping handlerMapping = configurer.getHandlerMapping();
		assertEquals(1, handlerMapping.getOrder());

		configurer.setOrder(2);
		handlerMapping = configurer.getHandlerMapping();
		assertEquals(2, handlerMapping.getOrder());
	}
}
