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

package org.springframework.web.servlet.view.script;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * Unit tests for pure Javascript templates running on Nashorn engine.
 *
 * @author Sebastien Deleuze
 */
public class NashornScriptTemplateTests {

	private WebApplicationContext webAppContext;

	private ServletContext servletContext;


	@Before
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
		MockHttpServletResponse response = renderViewWithModel("org/springframework/web/servlet/view/script/nashorn/template.html",
				model, ScriptTemplatingConfiguration.class);
		assertEquals("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>",
				response.getContentAsString());
	}

	@Test  // SPR-13453
	public void renderTemplateWithUrl() throws Exception {
		MockHttpServletResponse response = renderViewWithModel("org/springframework/web/servlet/view/script/nashorn/template.html",
				null, ScriptTemplatingWithUrlConfiguration.class);
		assertEquals("<html><head><title>Check url parameter</title></head><body><p>org/springframework/web/servlet/view/script/nashorn/template.html</p></body></html>",
				response.getContentAsString());
	}

	private MockHttpServletResponse renderViewWithModel(String viewUrl, Map<String, Object> model, Class<?> configuration) throws Exception {
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
