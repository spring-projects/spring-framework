/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.servlet.view.groovy;

import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;

import groovy.text.Template;
import groovy.text.TemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Brian Clozel
 */
public class GroovyMarkupViewTests {

	private static final String RESOURCE_LOADER_PATH = "classpath*:org/springframework/web/servlet/view/groovy/";

	private WebApplicationContext webAppContext;

	private ServletContext servletContext;


	@Before
	public void setup() {
		this.webAppContext = mock(WebApplicationContext.class);
		this.servletContext = new MockServletContext();
		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.webAppContext);
	}


	@Test
	public void missingGroovyMarkupConfig() throws Exception {
		GroovyMarkupView view = new GroovyMarkupView();
		given(this.webAppContext.getBeansOfType(GroovyMarkupConfig.class, true, false))
				.willReturn(new HashMap<String, GroovyMarkupConfig>());

		view.setUrl("sampleView");
		try {
			view.setApplicationContext(this.webAppContext);
			fail();
		}
		catch (ApplicationContextException ex) {
			assertTrue(ex.getMessage().contains("GroovyMarkupConfig"));
		}
	}

	@Test
	public void customTemplateEngine() throws Exception {
		GroovyMarkupView view = new GroovyMarkupView();
		view.setTemplateEngine(new TestTemplateEngine());
		view.setApplicationContext(this.webAppContext);

		DirectFieldAccessor accessor = new DirectFieldAccessor(view);
		TemplateEngine engine = (TemplateEngine) accessor.getPropertyValue("engine");
		assertNotNull(engine);
		assertEquals(TestTemplateEngine.class, engine.getClass());
	}

	@Test
	public void detectTemplateEngine() throws Exception {
		GroovyMarkupView view = new GroovyMarkupView();
		view.setTemplateEngine(new TestTemplateEngine());
		view.setApplicationContext(this.webAppContext);

		DirectFieldAccessor accessor = new DirectFieldAccessor(view);
		TemplateEngine engine = (TemplateEngine) accessor.getPropertyValue("engine");
		assertNotNull(engine);
		assertEquals(TestTemplateEngine.class, engine.getClass());
	}

	@Test
	public void checkResource() throws Exception {
		GroovyMarkupView view = createViewWithUrl("test.tpl");
		assertTrue(view.checkResource(Locale.US));
	}

	@Test
	public void checkMissingResource() throws Exception {
		GroovyMarkupView view = createViewWithUrl("missing.tpl");
		assertFalse(view.checkResource(Locale.US));
	}

	@Test
	public void checkI18nResource() throws Exception {
		GroovyMarkupView view = createViewWithUrl("i18n.tpl");
		assertTrue(view.checkResource(Locale.FRENCH));
	}

	@Test
	public void checkI18nResourceMissingLocale() throws Exception {
		GroovyMarkupView view = createViewWithUrl("i18n.tpl");
		assertTrue(view.checkResource(Locale.CHINESE));
	}

	@Test
	public void renderMarkupTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("name", "Spring");
		MockHttpServletResponse response = renderViewWithModel("test.tpl", model, Locale.US);
		assertThat(response.getContentAsString(), Matchers.containsString("<h1>Hello Spring</h1>"));
	}

	@Test
	public void renderI18nTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("name", "Spring");
		MockHttpServletResponse response = renderViewWithModel("i18n.tpl", model, Locale.FRANCE);
		assertEquals("<p>Bonjour Spring</p>", response.getContentAsString());

		response = renderViewWithModel("i18n.tpl", model, Locale.GERMANY);
		assertEquals("<p>Include German</p><p>Hallo Spring</p>", response.getContentAsString());

		response = renderViewWithModel("i18n.tpl", model, new Locale("es"));
		assertEquals("<p>Include Default</p><p>Hola Spring</p>", response.getContentAsString());
	}

	@Test
	public void renderLayoutTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		MockHttpServletResponse response = renderViewWithModel("content.tpl", model, Locale.US);
		assertEquals("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>",
				response.getContentAsString());
	}


	private MockHttpServletResponse renderViewWithModel(String viewUrl, Map<String, Object> model, Locale locale) throws Exception {
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
