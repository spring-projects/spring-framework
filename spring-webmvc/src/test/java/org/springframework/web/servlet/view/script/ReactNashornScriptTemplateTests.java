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
 * Unit tests for React templates running on Nashorn Javascript engine.
 *
 * @author Sebastien Deleuze
 */
public class ReactNashornScriptTemplateTests {

	private WebApplicationContext webAppContext;

	private ServletContext servletContext;

	@Before
	public void setup() {
		this.webAppContext = mock(WebApplicationContext.class);
		this.servletContext = new MockServletContext();
		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.webAppContext);
	}

	@Test
	public void renderJavascriptTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("title", "Layout example");
		model.put("body", "This is the body");
		MockHttpServletResponse response = renderViewWithModel("org/springframework/web/servlet/view/script/react/template.js", model);
		assertEquals("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>",
				response.getContentAsString());
	}

	@Test
	public void renderJsxTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("title", "Layout example");
		model.put("body", "This is the body");
		MockHttpServletResponse response = renderViewWithModel("org/springframework/web/servlet/view/script/react/template.jsx", model);
		assertEquals("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>",
				response.getContentAsString());
	}

	private MockHttpServletResponse renderViewWithModel(String viewUrl, Map<String, Object> model) throws Exception {
		ScriptTemplateView view = createViewWithUrl(viewUrl);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest();
		view.renderMergedOutputModel(model, request, response);
		return response;
	}

	private ScriptTemplateView createViewWithUrl(String viewUrl) throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (viewUrl.endsWith(".jsx")) {
			ctx.register(JsxTemplatingConfiguration.class);
		}
		else {
			ctx.register(JavascriptTemplatingConfiguration.class);
		}
		ctx.refresh();

		ScriptTemplateView view = new ScriptTemplateView();
		view.setApplicationContext(ctx);
		view.setUrl(viewUrl);
		view.afterPropertiesSet();
		return view;
	}

	@Configuration
	static class JavascriptTemplatingConfiguration {

		@Bean
		public ScriptTemplateConfigurer reactConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setEngineName("nashorn");
			configurer.setScripts(  "org/springframework/web/servlet/view/script/react/polyfill.js",
									"/META-INF/resources/webjars/react/0.12.2/react.js",
									"/META-INF/resources/webjars/react/0.12.2/JSXTransformer.js",
									"org/springframework/web/servlet/view/script/react/render.js");
			configurer.setRenderFunction("render");
			return configurer;
		}
	}

	@Configuration
	static class JsxTemplatingConfiguration {

		@Bean
		public ScriptTemplateConfigurer reactConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setEngineName("nashorn");
			configurer.setScripts(  "org/springframework/web/servlet/view/script/react/polyfill.js",
					"/META-INF/resources/webjars/react/0.12.2/react.js",
					"/META-INF/resources/webjars/react/0.12.2/JSXTransformer.js",
					"org/springframework/web/servlet/view/script/react/render.js");
			configurer.setRenderFunction("renderJsx");
			return configurer;
		}
	}

}
