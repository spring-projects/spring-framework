/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * Tests for String templates running on Jython.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class JythonScriptTemplateTests {

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
		String url = "org/springframework/web/servlet/view/script/jython/template.html";
		MockHttpServletResponse response = render(url, model);
		assertThat(response.getContentAsString())
			.isEqualTo("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>");
	}

	private static MockHttpServletResponse render(String viewUrl, Map<String, Object> model) throws Exception {
		ScriptTemplateView view = createViewWithUrl(viewUrl);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest();
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
		ScriptTemplateConfigurer jythonConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setScripts("org/springframework/web/servlet/view/script/jython/render.py");
			configurer.setEngineName("jython");
			configurer.setRenderFunction("render");
			return configurer;
		}
	}

}
