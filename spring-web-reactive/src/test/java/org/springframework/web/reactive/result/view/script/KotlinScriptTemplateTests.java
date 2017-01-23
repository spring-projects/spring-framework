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

package org.springframework.web.reactive.result.view.script;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Unit tests for Kotlin script templates running on Kotlin JSR 223 support
 *
 * @author Sebastien Deleuze
 */
public class KotlinScriptTemplateTests {

	private StaticApplicationContext context;

	@Before
	public void setup() {
		this.context = new StaticApplicationContext();
	}

	@Test
	public void renderTemplateWithFrenchLocale() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("foo", "Foo");
		MockServerHttpResponse response = renderViewWithModel("org/springframework/web/reactive/result/view/script/kotlin/template.kts",
				model, Locale.FRENCH, ScriptTemplatingConfiguration.class);
		assertEquals("<html><body>\n<p>Bonjour Foo</p>\n</body></html>",
				response.getBodyAsString().block());
	}

	@Test
	public void renderTemplateWithEnglishLocale() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("foo", "Foo");
		MockServerHttpResponse response = renderViewWithModel("org/springframework/web/reactive/result/view/script/kotlin/template.kts",
				model, Locale.ENGLISH, ScriptTemplatingConfiguration.class);
		assertEquals("<html><body>\n<p>Hello Foo</p>\n</body></html>",
				response.getBodyAsString().block());
	}

	private MockServerHttpResponse renderViewWithModel(String viewUrl, Map<String, Object> model, Locale locale, Class<?> configuration) throws Exception {
		ScriptTemplateView view = createViewWithUrl(viewUrl, configuration);
		view.setLocale(locale);
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		WebSessionManager manager = new DefaultWebSessionManager();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, response, manager);
		view.renderInternal(model, MediaType.TEXT_HTML, exchange).block();
		return response;
	}

	private ScriptTemplateView createViewWithUrl(String viewUrl, Class<?> configuration) throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(configuration);
		ctx.refresh();

		ScriptTemplateView view = new ScriptTemplateView();
		view.setApplicationContext(ctx);
		view.setUrl(viewUrl);
		view.afterPropertiesSet();
		return view;
	}


	@Configuration
	static class ScriptTemplatingConfiguration {

		@Bean
		public ScriptTemplateConfigurer kotlinScriptConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setEngineName("kotlin");
			configurer.setScripts("org/springframework/web/reactive/result/view/script/kotlin/render.kts");
			configurer.setRenderFunction("render");
			return configurer;
		}

		@Bean
		public ResourceBundleMessageSource messageSource() {
			ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
			messageSource.setBasename("org/springframework/web/reactive/result/view/script/messages");
			return messageSource;
		}
	}

}
