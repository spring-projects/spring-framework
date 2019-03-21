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
package org.springframework.web.reactive.config;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.Ordered;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.reactive.result.view.HttpMessageWriterView;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.reactive.result.view.script.ScriptTemplateConfigurer;
import org.springframework.web.reactive.result.view.script.ScriptTemplateViewResolver;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ViewResolverRegistry}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class ViewResolverRegistryTests {

	private ViewResolverRegistry registry;


	@Before
	public void setup() {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerSingleton("freeMarkerConfigurer", FreeMarkerConfigurer.class);
		context.registerSingleton("scriptTemplateConfigurer", ScriptTemplateConfigurer.class);
		this.registry = new ViewResolverRegistry(context);
	}


	@Test
	public void order() {
		assertEquals(Ordered.LOWEST_PRECEDENCE, this.registry.getOrder());
	}

	@Test
	public void hasRegistrations() {
		assertFalse(this.registry.hasRegistrations());

		this.registry.freeMarker();
		assertTrue(this.registry.hasRegistrations());
	}

	@Test
	public void noResolvers() {
		assertNotNull(this.registry.getViewResolvers());
		assertEquals(0, this.registry.getViewResolvers().size());
		assertFalse(this.registry.hasRegistrations());
	}

	@Test
	public void customViewResolver() {
		UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
		this.registry.viewResolver(viewResolver);

		assertSame(viewResolver, this.registry.getViewResolvers().get(0));
		assertEquals(1, this.registry.getViewResolvers().size());
	}

	@Test
	public void defaultViews() throws Exception {
		View view = new HttpMessageWriterView(new Jackson2JsonEncoder());
		this.registry.defaultViews(view);

		assertEquals(1, this.registry.getDefaultViews().size());
		assertSame(view, this.registry.getDefaultViews().get(0));
	}

	@Test  // SPR-16431
	public void scriptTemplate() {
		this.registry.scriptTemplate().prefix("/").suffix(".html");

		List<ViewResolver> viewResolvers = this.registry.getViewResolvers();
		assertEquals(1, viewResolvers.size());
		assertEquals(ScriptTemplateViewResolver.class, viewResolvers.get(0).getClass());
		DirectFieldAccessor accessor =  new DirectFieldAccessor(viewResolvers.get(0));
		assertEquals("/", accessor.getPropertyValue("prefix"));
		assertEquals(".html", accessor.getPropertyValue("suffix"));
	}

}
