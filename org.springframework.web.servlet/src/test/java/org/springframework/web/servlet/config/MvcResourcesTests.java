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
package org.springframework.web.servlet.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Feature;
import org.springframework.context.annotation.FeatureConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Test fixture for {@link MvcResources} feature specification.
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class MvcResourcesTests {

	@Test
	public void testResources() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MvcResourcesFeature.class);
		ctx.refresh();
		HttpRequestHandlerAdapter adapter = ctx.getBean(HttpRequestHandlerAdapter.class);
		assertNotNull(adapter);
		ResourceHttpRequestHandler handler = ctx.getBean(ResourceHttpRequestHandler.class);
		assertNotNull(handler);
		@SuppressWarnings("unchecked")
		List<Resource> locations = (List<Resource>) new DirectFieldAccessor(handler).getPropertyValue("locations");
		assertNotNull(locations);
		assertEquals(2, locations.size());
		assertEquals("foo", locations.get(0).getFilename());
		assertEquals("bar", locations.get(1).getFilename());
		SimpleUrlHandlerMapping mapping = ctx.getBean(SimpleUrlHandlerMapping.class);
		assertEquals(1, mapping.getOrder());
		assertSame(handler, mapping.getHandlerMap().get("/resources/**"));
	}

	@Test
	public void testInvalidResources() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(InvalidMvcResourcesFeature.class);
		try {
			ctx.refresh();
			fail("Invalid feature spec should not validate");
		} catch (RuntimeException e) {
			assertTrue(e.getCause().getMessage().contains("Mapping is required"));
			// TODO : should all problems be in the message ? 
		}
	}

	@FeatureConfiguration
	private static class MvcResourcesFeature {

		@SuppressWarnings("unused")
		@Feature
		public MvcResources resources() {
			return new MvcResources("/resources/**", new String[] { "/foo", "/bar" }).cachePeriod(86400).order(1);
		}

	}

	@FeatureConfiguration
	private static class InvalidMvcResourcesFeature {

		@SuppressWarnings("unused")
		@Feature
		public MvcResources resources() {
			return new MvcResources(" ", new String[] {});
		}

	}

}
