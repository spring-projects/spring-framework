/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.result.view.groovy;

import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import groovy.text.Template;
import groovy.text.TemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;

import static org.junit.Assert.*;


/**
 * @author Jason Yu
 */
public class GroovyMarkupViewTests {

	private static final String RESOURCE_LOADER_PATH = "classpath*:org/springframework/web/reactive/result/view/groovy/";

	private GenericApplicationContext context;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.refresh();
	}


	@Test
	public void missingGroovyMarkupConfig() throws Exception {
		this.exception.expect(ApplicationContextException.class);
		this.exception.expectMessage("Expected a single GroovyMarkupConfig bean in");

		GroovyMarkupView view = new GroovyMarkupView();
		view.setApplicationContext(this.context);
		view.setUrl("sampleView");
		view.afterPropertiesSet();
		fail();
	}

	@Test
	public void customTemplateEngine() throws Exception {
		GroovyMarkupView view = new GroovyMarkupView();
		view.setTemplateEngine(new TestTemplateEngine());
		view.setApplicationContext(this.context);

		DirectFieldAccessor accessor = new DirectFieldAccessor(view);
		TemplateEngine engine = (TemplateEngine)accessor.getPropertyValue("engine");
		assertNotNull(engine);
		assertEquals(TestTemplateEngine.class, engine.getClass());
	}

	@Test
	public void checkResource() throws Exception {
		GroovyMarkupView view = createViewWithUrl("test.tpl");
		assertTrue(view.checkResourceExists(Locale.US));
	}

	@Test
	public void checkMissingResource() throws Exception {
		GroovyMarkupView view = createViewWithUrl("missing.tpl");
		assertFalse(view.checkResourceExists(Locale.US));
	}

	@Test
	public void checkI18nResource() throws Exception {
		GroovyMarkupView view = createViewWithUrl("i18n.tpl");
		assertTrue(view.checkResourceExists(Locale.FRENCH));
	}

	@Test
	public void checkI18nResourceMissingLocale() throws Exception {
		GroovyMarkupView view = createViewWithUrl("i18n.tpl");
		assertTrue(view.checkResourceExists(Locale.CHINESE));
	}

	@Test
	public void renderMarkupTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("name", "Spring");
		MockServerWebExchange exchange = renderViewWithModel("test.tpl", model, Locale.US);
		DataBuffer buf = exchange.getResponse().getBody().blockFirst();
		assertThat(asString(buf), Matchers.containsString("<h1>Hello Spring</h1>"));
	}

	@Test
	public void renderI18nTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("name", "Spring");
		MockServerWebExchange exchange = renderViewWithModel("i18n.tpl", model, Locale.FRANCE);
		DataBuffer buf = exchange.getResponse().getBody().blockFirst();
		assertEquals("<p>Bonjour Spring</p>", asString(buf));

		exchange = renderViewWithModel("i18n.tpl", model, Locale.GERMANY);
		buf = exchange.getResponse().getBody().blockFirst();
		assertEquals("<p>Include German</p><p>Hallo Spring</p>", asString(buf));

		exchange = renderViewWithModel("i18n.tpl", model, new Locale("es"));
		buf = exchange.getResponse().getBody().blockFirst();
		assertEquals("<p>Include Default</p><p>Hola Spring</p>", asString(buf));
	}

	@Test
	public void renderLayoutTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		MockServerWebExchange exchange = renderViewWithModel("content.tpl", model, Locale.US);
		DataBuffer buf = exchange.getResponse().getBody().blockFirst();
		assertEquals("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>",
					 asString(buf));
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

	private MockServerWebExchange renderViewWithModel(
			String viewUrl, Map<String, Object> model, Locale locale) throws Exception {
		GroovyMarkupView view = createViewWithUrl(viewUrl);
		MockServerWebExchange exchange = MockServerHttpRequest.get("/path")
				.acceptLanguageAsLocales(locale).toExchange();
		LocaleContextHolder.setLocale(locale);
		view.render(model, null, exchange).block(Duration.ofSeconds(5));
		return exchange;
	}

	private static String asString(DataBuffer dataBuffer) {
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
		final byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
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
