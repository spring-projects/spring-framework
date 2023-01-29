/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Map;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for ERB templates running on JRuby.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class JRubyScriptTemplateTests {

	private WebApplicationContext webAppContext = mock();

	private ServletContext servletContext = new MockServletContext();


	@BeforeEach
	void setup() {
		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.webAppContext);
	}

	@Test
	void renderTemplate() throws Exception {
		Map<String, Object> model = Map.of(
			"title", "Layout example",
			"body", "This is the body"
		);
		String url = "org/springframework/web/servlet/view/script/jruby/template.erb";
		MockHttpServletResponse response = render(url, model);
		assertThat(response.getContentAsString())
			.isEqualTo("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>");
	}

	private static MockHttpServletResponse render(String viewUrl, Map<String, Object> model) throws Exception {
		ScriptTemplateView view = createViewWithUrl(viewUrl);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		view.renderMergedOutputModel(model, request, response);
		return response;
	}

	private static ScriptTemplateView createViewWithUrl(String viewUrl) throws Exception {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(ScriptTemplatingConfiguration.class);

		ScriptTemplateView view = new ScriptTemplateView();
		view.setApplicationContext(ctx);
		view.setUrl(viewUrl);
		view.afterPropertiesSet();
		return view;
	}


	@Configuration
	static class ScriptTemplatingConfiguration {

		@Bean
		ScriptTemplateConfigurer jRubyConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setScripts("org/springframework/web/servlet/view/script/jruby/render.rb");
			configurer.setEngineName("jruby");
			configurer.setRenderFunction("render");
			return configurer;
		}
	}

}
