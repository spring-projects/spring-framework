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

import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Feature;
import org.springframework.context.annotation.FeatureConfiguration;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

/**
 * Test fixture for {@link MvcDefaultServletHandler} feature specification.
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class MvcDefaultServletHandlerTests {

	@Test
	public void testDefaultServletHandler() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MvcDefaultServletHandlerFeature.class);
		ctx.refresh();
		HttpRequestHandlerAdapter adapter = ctx.getBean(HttpRequestHandlerAdapter.class);
		assertNotNull(adapter);
		DefaultServletHttpRequestHandler handler = ctx.getBean(DefaultServletHttpRequestHandler.class);
		assertNotNull(handler);
		String defaultServletHandlerName = (String) new DirectFieldAccessor(handler)
				.getPropertyValue("defaultServletName");
		assertEquals("foo", defaultServletHandlerName);
	}

	@FeatureConfiguration
	private static class MvcDefaultServletHandlerFeature {

		@SuppressWarnings("unused")
		@Feature
		public MvcDefaultServletHandler defaultServletHandler() {
			return new MvcDefaultServletHandler("foo");
		}

	}

}
