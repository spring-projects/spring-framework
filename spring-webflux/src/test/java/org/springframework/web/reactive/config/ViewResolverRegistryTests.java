/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.config;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.Ordered;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.reactive.result.view.HttpMessageWriterView;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.reactive.result.view.script.ScriptTemplateConfigurer;
import org.springframework.web.reactive.result.view.script.ScriptTemplateViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ViewResolverRegistry}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class ViewResolverRegistryTests {

	private ViewResolverRegistry registry;


	@BeforeEach
	void setup() {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerSingleton("freeMarkerConfigurer", FreeMarkerConfigurer.class);
		context.registerSingleton("scriptTemplateConfigurer", ScriptTemplateConfigurer.class);
		this.registry = new ViewResolverRegistry(context);
	}


	@Test
	void order() {
		assertThat(this.registry.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
	}

	@Test
	void hasRegistrations() {
		assertThat(this.registry.hasRegistrations()).isFalse();

		this.registry.freeMarker();
		assertThat(this.registry.hasRegistrations()).isTrue();
	}

	@Test
	void noResolvers() {
		assertThat(this.registry.getViewResolvers()).isNotNull();
		assertThat(this.registry.getViewResolvers()).isEmpty();
		assertThat(this.registry.hasRegistrations()).isFalse();
	}

	@Test
	void customViewResolver() {
		UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
		this.registry.viewResolver(viewResolver);

		assertThat(this.registry.getViewResolvers()).containsExactly(viewResolver);
	}

	@Test
	void defaultViews() {
		View view = new HttpMessageWriterView(new JacksonJsonEncoder());
		this.registry.defaultViews(view);

		assertThat(this.registry.getDefaultViews()).containsExactly(view);
	}

	@Test  // SPR-16431
	void scriptTemplate() {
		this.registry.scriptTemplate().prefix("/").suffix(".html");

		List<ViewResolver> viewResolvers = this.registry.getViewResolvers();
		assertThat(viewResolvers).hasSize(1);
		assertThat(viewResolvers.get(0).getClass()).isEqualTo(ScriptTemplateViewResolver.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(viewResolvers.get(0));
		assertThat(accessor.getPropertyValue("prefix")).isEqualTo("/");
		assertThat(accessor.getPropertyValue("suffix")).isEqualTo(".html");
	}

}
