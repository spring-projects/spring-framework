/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.view.script;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_15;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for pure JavaScript templates running on Nashorn engine.
 *
 * @author Sebastien Deleuze
 */
@DisabledForJreRange(min = JAVA_15) // Nashorn JavaScript engine removed in Java 15
public class NashornScriptTemplateTests {

	private WebApplicationContext webAppContext;

	private ServletContext servletContext;


	@BeforeEach
	public void setup() {
		this.webAppContext = mock(WebApplicationContext.class);
		this.servletContext = new MockServletContext();
		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.webAppContext);
	}

	@Test
	public void renderTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("title", "Layout example");
		model.put("body", "This is the body");
		String url = "org/springframework/web/servlet/view/script/nashorn/template.html";
		MockHttpServletResponse response = render(url, model, ScriptTemplatingConfiguration.class);
		assertThat(response.getContentAsString()).isEqualTo("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>");
	}

	@Test  // SPR-13453
	public void renderTemplateWithUrl() throws Exception {
		String url = "org/springframework/web/servlet/view/script/nashorn/template.html";
		MockHttpServletResponse response = render(url, null, ScriptTemplatingWithUrlConfiguration.class);
		assertThat(response.getContentAsString()).isEqualTo(("<html><head><title>Check url parameter</title></head><body><p>" + url + "</p></body></html>"));
	}

	private MockHttpServletResponse render(String viewUrl, Map<String, Object> model,
			Class<?> configuration) throws Exception {

		ScriptTemplateView view = createViewWithUrl(viewUrl, configuration);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest();
		view.renderMergedOutputModel(model, request, response);
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
		public ScriptTemplateConfigurer nashornConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setEngineName("nashorn");
			configurer.setScripts("org/springframework/web/servlet/view/script/nashorn/render.js");
			configurer.setRenderFunction("render");
			return configurer;
		}
	}


	@Configuration
	static class ScriptTemplatingWithUrlConfiguration {

		@Bean
		public ScriptTemplateConfigurer nashornConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setEngineName("nashorn");
			configurer.setScripts("org/springframework/web/servlet/view/script/nashorn/render.js");
			configurer.setRenderFunction("renderWithUrl");
			return configurer;
		}
	}

}
