/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.StatusController;

import static org.junit.Assert.assertEquals;

/**
 * Test fixture with a {@link StatusControllerRegistry}.
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class StatusControllerRegistryTests {

	private StatusControllerRegistry registry;


	@Before
	public void setUp() {
		this.registry = new StatusControllerRegistry();
	}

	@Test
	public void status() throws Exception {
		registry.addStatusController("/notfound", HttpStatus.NOT_FOUND);
		Map<String, ?> urlMap = getHandlerMapping().getUrlMap();
		StatusController controller = (StatusController)urlMap.get("/notfound");
		assertNotNull(controller);
		assertEquals(StatusController.class, controller.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(controller);
		assertEquals(HttpStatus.NOT_FOUND, accessor.getPropertyValue("statusCode"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void statusWithWrongStatusCode() throws Exception {
		registry.addStatusController("/notfound", HttpStatus.TEMPORARY_REDIRECT);
		fail();
	}

	@Test
	public void redirect() throws Exception {
		registry.addStatusController("/source", HttpStatus.TEMPORARY_REDIRECT,
				"/destination");
		Map<String, ?> urlMap = getHandlerMapping().getUrlMap();
		StatusController controller = (StatusController)urlMap.get("/source");
		assertNotNull(controller);
		assertEquals(StatusController.class, controller.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(controller);
		assertEquals(HttpStatus.TEMPORARY_REDIRECT, accessor.getPropertyValue("statusCode"));
		assertEquals("/destination", accessor.getPropertyValue("redirectPath"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void redirectWithWrongStatusCode() throws Exception {
		registry.addStatusController("/source", HttpStatus.NOT_FOUND, "/destination");
		fail();
	}

	private SimpleUrlHandlerMapping getHandlerMapping() {
		return (SimpleUrlHandlerMapping) registry.getHandlerMapping();
	}

}
