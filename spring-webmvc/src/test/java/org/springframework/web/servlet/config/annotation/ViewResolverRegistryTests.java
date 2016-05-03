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

package org.springframework.web.servlet.config.annotation;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.Ordered;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;
import org.springframework.web.servlet.view.xml.MarshallingView;

import static org.junit.Assert.*;

/**
 * Test fixture with a {@link ViewResolverRegistry}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class ViewResolverRegistryTests {

	private ViewResolverRegistry registry;


	@Before
	public void setUp() {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerSingleton("freeMarkerConfigurer", FreeMarkerConfigurer.class);
		context.registerSingleton("velocityConfigurer", VelocityConfigurer.class);
		context.registerSingleton("tilesConfigurer", TilesConfigurer.class);
		context.registerSingleton("groovyMarkupConfigurer", GroovyMarkupConfigurer.class);
		context.registerSingleton("scriptTemplateConfigurer", ScriptTemplateConfigurer.class);
		this.registry = new ViewResolverRegistry();
		this.registry.setApplicationContext(context);
		this.registry.setContentNegotiationManager(new ContentNegotiationManager());
	}


	@Test
	public void order() {
		assertEquals(Ordered.LOWEST_PRECEDENCE, this.registry.getOrder());
		this.registry.enableContentNegotiation();
		assertEquals(Ordered.HIGHEST_PRECEDENCE, this.registry.getOrder());
	}

	@Test
	public void hasRegistrations() {
		assertFalse(this.registry.hasRegistrations());
		this.registry.velocity();
		assertTrue(this.registry.hasRegistrations());
	}

	@Test
	public void hasRegistrationsWhenContentNegotiationEnabled() {
		assertFalse(this.registry.hasRegistrations());
		this.registry.enableContentNegotiation();
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
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/", ".jsp");
		this.registry.viewResolver(viewResolver);
		assertSame(viewResolver, this.registry.getViewResolvers().get(0));
	}

	@Test
	public void beanName() {
		this.registry.beanName();
		assertEquals(1, this.registry.getViewResolvers().size());
		assertEquals(BeanNameViewResolver.class, registry.getViewResolvers().get(0).getClass());
	}

	@Test
	public void jspDefaultValues() {
		this.registry.jsp();
		InternalResourceViewResolver resolver = checkAndGetResolver(InternalResourceViewResolver.class);
		checkPropertyValues(resolver, "prefix", "/WEB-INF/", "suffix", ".jsp");
	}

	@Test
	public void jsp() {
		this.registry.jsp("/", ".jsp");
		InternalResourceViewResolver resolver = checkAndGetResolver(InternalResourceViewResolver.class);
		checkPropertyValues(resolver, "prefix", "/", "suffix", ".jsp");
	}

	@Test
	public void jspMultipleResolvers() {
		this.registry.jsp().viewNames("view1", "view2");
		this.registry.jsp().viewNames("view3", "view4");
		assertNotNull(this.registry.getViewResolvers());
		assertEquals(2, this.registry.getViewResolvers().size());
		assertEquals(InternalResourceViewResolver.class, this.registry.getViewResolvers().get(0).getClass());
		assertEquals(InternalResourceViewResolver.class, this.registry.getViewResolvers().get(1).getClass());
	}

	@Test
	public void tiles() {
		this.registry.tiles();
		checkAndGetResolver(TilesViewResolver.class);
	}

	@Test
	public void velocity() {
		this.registry.velocity().prefix("/").suffix(".vm").cache(true);
		VelocityViewResolver resolver = checkAndGetResolver(VelocityViewResolver.class);
		checkPropertyValues(resolver, "prefix", "/", "suffix", ".vm", "cacheLimit", 1024);
	}

	@Test
	public void velocityDefaultValues() {
		this.registry.velocity();
		VelocityViewResolver resolver = checkAndGetResolver(VelocityViewResolver.class);
		checkPropertyValues(resolver, "prefix", "", "suffix", ".vm");
	}

	@Test
	public void freeMarker() {
		this.registry.freeMarker().prefix("/").suffix(".fmt").cache(false);
		FreeMarkerViewResolver resolver = checkAndGetResolver(FreeMarkerViewResolver.class);
		checkPropertyValues(resolver, "prefix", "/", "suffix", ".fmt", "cacheLimit", 0);
	}

	@Test
	public void freeMarkerDefaultValues() {
		this.registry.freeMarker();
		FreeMarkerViewResolver resolver = checkAndGetResolver(FreeMarkerViewResolver.class);
		checkPropertyValues(resolver, "prefix", "", "suffix", ".ftl");
	}

	@Test
	public void groovyMarkup() {
		this.registry.groovy().prefix("/").suffix(".groovy").cache(true);
		GroovyMarkupViewResolver resolver = checkAndGetResolver(GroovyMarkupViewResolver.class);
		checkPropertyValues(resolver, "prefix", "/", "suffix", ".groovy", "cacheLimit", 1024);
	}

	@Test
	public void groovyMarkupDefaultValues() {
		this.registry.groovy();
		GroovyMarkupViewResolver resolver = checkAndGetResolver(GroovyMarkupViewResolver.class);
		checkPropertyValues(resolver, "prefix", "", "suffix", ".tpl");
	}

	@Test
	public void scriptTemplate() {
		this.registry.scriptTemplate().prefix("/").suffix(".html").cache(true);
		ScriptTemplateViewResolver resolver = checkAndGetResolver(ScriptTemplateViewResolver.class);
		checkPropertyValues(resolver, "prefix", "/", "suffix", ".html", "cacheLimit", 1024);
	}

	@Test
	public void scriptTemplateDefaultValues() {
		this.registry.scriptTemplate();
		ScriptTemplateViewResolver resolver = checkAndGetResolver(ScriptTemplateViewResolver.class);
		checkPropertyValues(resolver, "prefix", "", "suffix", "");
	}

	@Test
	public void contentNegotiation() {
		MappingJackson2JsonView view = new MappingJackson2JsonView();
		this.registry.enableContentNegotiation(view);
		ContentNegotiatingViewResolver resolver = checkAndGetResolver(ContentNegotiatingViewResolver.class);
		assertEquals(Arrays.asList(view), resolver.getDefaultViews());
		assertEquals(Ordered.HIGHEST_PRECEDENCE, this.registry.getOrder());
	}

	@Test
	public void contentNegotiationAddsDefaultViewRegistrations() {
		MappingJackson2JsonView view1 = new MappingJackson2JsonView();
		this.registry.enableContentNegotiation(view1);

		ContentNegotiatingViewResolver resolver1 = checkAndGetResolver(ContentNegotiatingViewResolver.class);
		assertEquals(Arrays.asList(view1), resolver1.getDefaultViews());

		MarshallingView view2 = new MarshallingView();
		this.registry.enableContentNegotiation(view2);

		ContentNegotiatingViewResolver resolver2 = checkAndGetResolver(ContentNegotiatingViewResolver.class);
		assertEquals(Arrays.asList(view1, view2), resolver2.getDefaultViews());
		assertSame(resolver1, resolver2);
	}


	@SuppressWarnings("unchecked")
	private <T extends ViewResolver> T checkAndGetResolver(Class<T> resolverType) {
		assertNotNull(this.registry.getViewResolvers());
		assertEquals(1, this.registry.getViewResolvers().size());
		assertEquals(resolverType, this.registry.getViewResolvers().get(0).getClass());
		return (T) registry.getViewResolvers().get(0);
	}

	private void checkPropertyValues(ViewResolver resolver, Object... nameValuePairs) {
		DirectFieldAccessor accessor =  new DirectFieldAccessor(resolver);
		for (int i = 0; i < nameValuePairs.length ; i++, i++) {
			Object expected = nameValuePairs[i + 1];
			Object actual = accessor.getPropertyValue((String) nameValuePairs[i]);
			assertEquals(expected, actual);
		}
	}

}
