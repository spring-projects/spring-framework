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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test fixture with a {@link ViewResolutionRegistry}.
 *
 * @author Sebastien Deleuze
 */
public class ViewResolutionRegistryTests {

	private ViewResolutionRegistry registry;

	@Before
	public void setUp() {
		registry = new ViewResolutionRegistry(new StaticWebApplicationContext());
	}

	@Test
	public void noViewResolution() {
		assertNotNull(registry.getViewResolvers());
		assertEquals(0, registry.getViewResolvers().size());
	}

	@Test
	public void customViewResolution() {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setPrefix("/");
		viewResolver.setSuffix(".jsp");
		registry.addViewResolver(viewResolver);
		assertEquals(InternalResourceViewResolver.class, registry.getViewResolvers().get(0).getClass());
		InternalResourceViewResolver resolver = (InternalResourceViewResolver)registry.getViewResolvers().get(0);
		DirectFieldAccessor resolverDirectFieldAccessor =  new DirectFieldAccessor(resolver);
		assertEquals("/", resolverDirectFieldAccessor.getPropertyValue("prefix"));
		assertEquals(".jsp", resolverDirectFieldAccessor.getPropertyValue("suffix"));
	}

	@Test
	public void beanNameViewResolution() {
		registry.beanName();
		assertNotNull(registry.getViewResolvers());
		assertEquals(1, registry.getViewResolvers().size());
		assertEquals(BeanNameViewResolver.class, registry.getViewResolvers().get(0).getClass());
	}

	@Test
	public void jspViewResolution() {
		registry.jsp("/", ".jsp");
		assertNotNull(registry.getViewResolvers());
		assertEquals(1, registry.getViewResolvers().size());
		assertEquals(InternalResourceViewResolver.class, registry.getViewResolvers().get(0).getClass());
		InternalResourceViewResolver resolver = (InternalResourceViewResolver)registry.getViewResolvers().get(0);
		DirectFieldAccessor resolverDirectFieldAccessor =  new DirectFieldAccessor(resolver);
		assertEquals("/", resolverDirectFieldAccessor.getPropertyValue("prefix"));
		assertEquals(".jsp", resolverDirectFieldAccessor.getPropertyValue("suffix"));
	}

	@Test
	public void defaultJspViewResolution() {
		registry.jsp();
		assertNotNull(registry.getViewResolvers());
		assertEquals(1, registry.getViewResolvers().size());
		assertEquals(InternalResourceViewResolver.class, registry.getViewResolvers().get(0).getClass());
		InternalResourceViewResolver resolver = (InternalResourceViewResolver)registry.getViewResolvers().get(0);
		DirectFieldAccessor resolverDirectFieldAccessor =  new DirectFieldAccessor(resolver);
		assertEquals("/WEB-INF/", resolverDirectFieldAccessor.getPropertyValue("prefix"));
		assertEquals(".jsp", resolverDirectFieldAccessor.getPropertyValue("suffix"));
	}

	@Test
	public void tilesViewResolution() {
		this.registry.tiles();
		assertNotNull(this.registry.getViewResolvers());
		assertEquals(1, this.registry.getViewResolvers().size());
		assertEquals(TilesViewResolver.class, this.registry.getViewResolvers().get(0).getClass());
	}

	@Test
	public void velocityViewResolution() {
		registry.velocity().prefix("/").suffix(".vm").cache(true);
		assertNotNull(registry.getViewResolvers());
		assertEquals(1, registry.getViewResolvers().size());
		assertEquals(VelocityViewResolver.class, registry.getViewResolvers().get(0).getClass());
		VelocityViewResolver resolver = (VelocityViewResolver)registry.getViewResolvers().get(0);
		DirectFieldAccessor resolverDirectFieldAccessor =  new DirectFieldAccessor(resolver);
		assertEquals("/", resolverDirectFieldAccessor.getPropertyValue("prefix"));
		assertEquals(".vm", resolverDirectFieldAccessor.getPropertyValue("suffix"));
		assertEquals(1024, resolverDirectFieldAccessor.getPropertyValue("cacheLimit"));
	}

	@Test
	public void defaultVelocityViewResolution() {
		registry.velocity();
		assertNotNull(registry.getViewResolvers());
		assertEquals(1, registry.getViewResolvers().size());
		assertEquals(VelocityViewResolver.class, registry.getViewResolvers().get(0).getClass());
		VelocityViewResolver resolver = (VelocityViewResolver)registry.getViewResolvers().get(0);
		DirectFieldAccessor resolverDirectFieldAccessor =  new DirectFieldAccessor(resolver);
		assertEquals("", resolverDirectFieldAccessor.getPropertyValue("prefix"));
		assertEquals(".vm", resolverDirectFieldAccessor.getPropertyValue("suffix"));
	}

	@Test
	public void freeMarkerViewResolution() {
		registry.freemarker().prefix("/").suffix(".fmt").cache(false);
		assertNotNull(registry.getViewResolvers());
		assertEquals(1, registry.getViewResolvers().size());
		assertEquals(FreeMarkerViewResolver.class, registry.getViewResolvers().get(0).getClass());
		FreeMarkerViewResolver resolver = (FreeMarkerViewResolver)registry.getViewResolvers().get(0);
		DirectFieldAccessor resolverDirectFieldAccessor =  new DirectFieldAccessor(resolver);
		assertEquals("/", resolverDirectFieldAccessor.getPropertyValue("prefix"));
		assertEquals(".fmt", resolverDirectFieldAccessor.getPropertyValue("suffix"));
		assertEquals(0, resolverDirectFieldAccessor.getPropertyValue("cacheLimit"));
	}

	@Test
	public void defaultFreeMarkerViewResolution() {
		registry.freemarker();
		assertNotNull(registry.getViewResolvers());
		assertEquals(1, registry.getViewResolvers().size());
		assertEquals(FreeMarkerViewResolver.class, registry.getViewResolvers().get(0).getClass());
		FreeMarkerViewResolver resolver = (FreeMarkerViewResolver)registry.getViewResolvers().get(0);
		DirectFieldAccessor resolverDirectFieldAccessor =  new DirectFieldAccessor(resolver);
		assertEquals("", resolverDirectFieldAccessor.getPropertyValue("prefix"));
		assertEquals(".ftl", resolverDirectFieldAccessor.getPropertyValue("suffix"));
	}

	@Test
	public void contentNegotiatingViewResolution() {
		registry.contentNegotiating().useNotAcceptable(false).defaultViews(new MappingJackson2JsonView());
		assertNotNull(registry.getViewResolvers());
		assertEquals(1, registry.getViewResolvers().size());
		assertEquals(ContentNegotiatingViewResolver.class, registry.getViewResolvers().get(0).getClass());
	}

	@Test
	public void multipleViewResolutions() {
		registry.jsp().and().beanName();
		assertNotNull(registry.getViewResolvers());
		assertEquals(2, registry.getViewResolvers().size());
	}

}
