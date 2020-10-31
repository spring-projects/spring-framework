/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import org.springframework.web.servlet.view.xml.MarshallingView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with a {@link ViewResolverRegistry}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class ViewResolverRegistryTests {

	private ViewResolverRegistry registry;


	@BeforeEach
	public void setup() {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerSingleton("freeMarkerConfigurer", FreeMarkerConfigurer.class);
		context.registerSingleton("tilesConfigurer", TilesConfigurer.class);
		context.registerSingleton("groovyMarkupConfigurer", GroovyMarkupConfigurer.class);
		context.registerSingleton("scriptTemplateConfigurer", ScriptTemplateConfigurer.class);

		this.registry = new ViewResolverRegistry(new ContentNegotiationManager(), context);
	}


	@Test
	public void order() {
		assertThat(this.registry.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
		this.registry.enableContentNegotiation();
		assertThat(this.registry.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
	}

	@Test
	public void hasRegistrations() {
		assertThat(this.registry.hasRegistrations()).isFalse();
		this.registry.freeMarker();
		assertThat(this.registry.hasRegistrations()).isTrue();
	}

	@Test
	public void hasRegistrationsWhenContentNegotiationEnabled() {
		assertThat(this.registry.hasRegistrations()).isFalse();
		this.registry.enableContentNegotiation();
		assertThat(this.registry.hasRegistrations()).isTrue();
	}

	@Test
	public void noResolvers() {
		assertThat(this.registry.getViewResolvers()).isNotNull();
		assertThat(this.registry.getViewResolvers().size()).isEqualTo(0);
		assertThat(this.registry.hasRegistrations()).isFalse();
	}

	@Test
	public void customViewResolver() {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/", ".jsp");
		this.registry.viewResolver(viewResolver);
		assertThat(this.registry.getViewResolvers().get(0)).isSameAs(viewResolver);
	}

	@Test
	public void beanName() {
		this.registry.beanName();
		assertThat(this.registry.getViewResolvers().size()).isEqualTo(1);
		assertThat(registry.getViewResolvers().get(0).getClass()).isEqualTo(BeanNameViewResolver.class);
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
		assertThat(this.registry.getViewResolvers()).isNotNull();
		assertThat(this.registry.getViewResolvers().size()).isEqualTo(2);
		assertThat(this.registry.getViewResolvers().get(0).getClass()).isEqualTo(InternalResourceViewResolver.class);
		assertThat(this.registry.getViewResolvers().get(1).getClass()).isEqualTo(InternalResourceViewResolver.class);
	}

	@Test
	public void tiles() {
		this.registry.tiles();
		checkAndGetResolver(TilesViewResolver.class);
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
		assertThat(resolver.getDefaultViews()).isEqualTo(Arrays.asList(view));
		assertThat(this.registry.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
	}

	@Test
	public void contentNegotiationAddsDefaultViewRegistrations() {
		MappingJackson2JsonView view1 = new MappingJackson2JsonView();
		this.registry.enableContentNegotiation(view1);

		ContentNegotiatingViewResolver resolver1 = checkAndGetResolver(ContentNegotiatingViewResolver.class);
		assertThat(resolver1.getDefaultViews()).isEqualTo(Arrays.asList(view1));

		MarshallingView view2 = new MarshallingView();
		this.registry.enableContentNegotiation(view2);

		ContentNegotiatingViewResolver resolver2 = checkAndGetResolver(ContentNegotiatingViewResolver.class);
		assertThat(resolver2.getDefaultViews()).isEqualTo(Arrays.asList(view1, view2));
		assertThat(resolver2).isSameAs(resolver1);
	}


	@SuppressWarnings("unchecked")
	private <T extends ViewResolver> T checkAndGetResolver(Class<T> resolverType) {
		assertThat(this.registry.getViewResolvers()).isNotNull();
		assertThat(this.registry.getViewResolvers().size()).isEqualTo(1);
		assertThat(this.registry.getViewResolvers().get(0).getClass()).isEqualTo(resolverType);
		return (T) registry.getViewResolvers().get(0);
	}

	private void checkPropertyValues(ViewResolver resolver, Object... nameValuePairs) {
		DirectFieldAccessor accessor =  new DirectFieldAccessor(resolver);
		for (int i = 0; i < nameValuePairs.length ; i++, i++) {
			Object expected = nameValuePairs[i + 1];
			Object actual = accessor.getPropertyValue((String) nameValuePairs[i]);
			assertThat(actual).isEqualTo(expected);
		}
	}

}
