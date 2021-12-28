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

package org.springframework.web.servlet.view.groovy;

import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import groovy.text.Template;
import groovy.text.TemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Brian Clozel
 */
public class GroovyMarkupViewTests {

	private static final String RESOURCE_LOADER_PATH = "classpath*:org/springframework/web/servlet/view/groovy/";

	private WebApplicationContext webAppContext;

	private ServletContext servletContext;


	@BeforeEach
	public void setup() {
		this.webAppContext = mock(WebApplicationContext.class);
		this.servletContext = new MockServletContext();
		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.webAppContext);
	}


	@Test
	public void missingGroovyMarkupConfig() throws Exception {
		GroovyMarkupView view = new GroovyMarkupView();
		given(this.webAppContext.getBeansOfType(GroovyMarkupConfig.class, true, false))
				.willReturn(new HashMap<>());

		view.setUrl("sampleView");
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() ->
				view.setApplicationContext(this.webAppContext))
			.withMessageContaining("GroovyMarkupConfig");
	}

	@Test
	public void customTemplateEngine() throws Exception {
		GroovyMarkupView view = new GroovyMarkupView();
		view.setTemplateEngine(new TestTemplateEngine());
		view.setApplicationContext(this.webAppContext);

		DirectFieldAccessor accessor = new DirectFieldAccessor(view);
		TemplateEngine engine = (TemplateEngine) accessor.getPropertyValue("engine");
		assertThat(engine).isNotNull();
		assertThat(engine.getClass()).isEqualTo(TestTemplateEngine.class);
	}

	@Test
	public void detectTemplateEngine() throws Exception {
		GroovyMarkupView view = new GroovyMarkupView();
		view.setTemplateEngine(new TestTemplateEngine());
		view.setApplicationContext(this.webAppContext);

		DirectFieldAccessor accessor = new DirectFieldAccessor(view);
		TemplateEngine engine = (TemplateEngine) accessor.getPropertyValue("engine");
		assertThat(engine).isNotNull();
		assertThat(engine.getClass()).isEqualTo(TestTemplateEngine.class);
	}

	@Test
	public void checkResource() throws Exception {
		GroovyMarkupView view = createViewWithUrl("test.tpl");
		assertThat(view.checkResource(Locale.US)).isTrue();
	}

	@Test
	public void checkMissingResource() throws Exception {
		GroovyMarkupView view = createViewWithUrl("missing.tpl");
		assertThat(view.checkResource(Locale.US)).isFalse();
	}

	@Test
	public void checkI18nResource() throws Exception {
		GroovyMarkupView view = createViewWithUrl("i18n.tpl");
		assertThat(view.checkResource(Locale.FRENCH)).isTrue();
	}

	@Test
	public void checkI18nResourceMissingLocale() throws Exception {
		GroovyMarkupView view = createViewWithUrl("i18n.tpl");
		assertThat(view.checkResource(Locale.CHINESE)).isTrue();
	}

	@Test
	public void renderMarkupTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("name", "Spring");
		MockHttpServletResponse response = renderViewWithModel("test.tpl", model, Locale.US);
		assertThat(response.getContentAsString()).contains("<h1>Hello Spring</h1>");
	}

	@Test
	public void renderI18nTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("name", "Spring");
		MockHttpServletResponse response = renderViewWithModel("i18n.tpl", model, Locale.FRANCE);
		assertThat(response.getContentAsString()).isEqualTo("<p>Bonjour Spring</p>");

		response = renderViewWithModel("i18n.tpl", model, Locale.GERMANY);
		assertThat(response.getContentAsString()).isEqualTo("<p>Include German</p><p>Hallo Spring</p>");

		response = renderViewWithModel("i18n.tpl", model, new Locale("es"));
		assertThat(response.getContentAsString()).isEqualTo("<p>Include Default</p><p>Hola Spring</p>");
	}

	@Test
	public void renderLayoutTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		MockHttpServletResponse response = renderViewWithModel("content.tpl", model, Locale.US);
		assertThat(response.getContentAsString()).isEqualTo("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>");
	}


	private MockHttpServletResponse renderViewWithModel(String viewUrl, Map<String,
			Object> model, Locale locale) throws Exception {

		GroovyMarkupView view = createViewWithUrl(viewUrl);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(locale);
		LocaleContextHolder.setLocale(locale);
		view.renderMergedTemplateModel(model, request, response);
		return response;
	}

	private GroovyMarkupView createViewWithUrl(String viewUrl) throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(GroovyMarkupConfiguration.class);
		ctx.refresh();

		GroovyMarkupView view = new GroovyMarkupView();
		view.setUrl(viewUrl);
		view.setApplicationContext(ctx);
		view.afterPropertiesSet();
		return view;
	}


	public class TestTemplateEngine extends MarkupTemplateEngine {

		public TestTemplateEngine() {
			super(new TemplateConfiguration());
		}

		@Override
		public Template createTemplate(Reader reader) {
			return null;
		}
	}


	@Configuration
	static class GroovyMarkupConfiguration {

		@Bean
		public GroovyMarkupConfig groovyMarkupConfigurer() {
			GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
			configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH);
			return configurer;
		}
	}

}
