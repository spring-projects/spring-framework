/*
 * Copyright 2002-2022 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ViewResolverRegistry}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class ViewResolverRegistryTests {

	private ViewResolverRegistry registry;


	@BeforeEach
	public void setup() {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerSingleton("freeMarkerConfigurer", FreeMarkerConfigurer.class);
		context.registerSingleton("scriptTemplateConfigurer", ScriptTemplateConfigurer.class);
		this.registry = new ViewResolverRegistry(context);
	}


	@Test
	public void order() {
		assertThat(this.registry.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
	}

	@Test
	public void hasRegistrations() {
		assertThat(this.registry.hasRegistrations()).isFalse();

		this.registry.freeMarker();
		assertThat(this.registry.hasRegistrations()).isTrue();
	}

	@Test
	public void noResolvers() {
		assertThat(this.registry.getViewResolvers()).isNotNull();
		assertThat(this.registry.getViewResolvers()).isEmpty();
		assertThat(this.registry.hasRegistrations()).isFalse();
	}

	@Test
	public void customViewResolver() {
		UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
		this.registry.viewResolver(viewResolver);

		assertThat(this.registry.getViewResolvers().get(0)).isSameAs(viewResolver);
		assertThat(this.registry.getViewResolvers()).hasSize(1);
	}

	@Test
	public void defaultViews() throws Exception {
		View view = new HttpMessageWriterView(new Jackson2JsonEncoder());
		this.registry.defaultViews(view);

		assertThat(this.registry.getDefaultViews()).hasSize(1);
		assertThat(this.registry.getDefaultViews().get(0)).isSameAs(view);
	}

	@Test  // SPR-16431
	public void scriptTemplate() {
		this.registry.scriptTemplate().prefix("/").suffix(".html");

		List<ViewResolver> viewResolvers = this.registry.getViewResolvers();
		assertThat(viewResolvers).hasSize(1);
		assertThat(viewResolvers.get(0).getClass()).isEqualTo(ScriptTemplateViewResolver.class);
		DirectFieldAccessor accessor =  new DirectFieldAccessor(viewResolvers.get(0));
		assertThat(accessor.getPropertyValue("prefix")).isEqualTo("/");
		assertThat(accessor.getPropertyValue("suffix")).isEqualTo(".html");
	}

}
