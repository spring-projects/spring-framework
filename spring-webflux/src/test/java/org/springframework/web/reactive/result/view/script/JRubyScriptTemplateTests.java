/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.result.view.script;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ERB templates running on JRuby.
 *
 * @author Sebastien Deleuze
 */
class JRubyScriptTemplateTests {

	@Test
	void renderTemplate() throws Exception {
		Map<String, Object> model = Map.of(
			"title", "Layout example",
			"body", "This is the body"
		);
		String url = "org/springframework/web/reactive/result/view/script/jruby/template.erb";
		MockServerHttpResponse response = renderViewWithModel(url, model);
		assertThat(response.getBodyAsString().block())
			.isEqualTo("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>");
	}


	private static MockServerHttpResponse renderViewWithModel(String viewUrl, Map<String, Object> model) throws Exception {
		ScriptTemplateView view = createViewWithUrl(viewUrl);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		view.renderInternal(model, MediaType.TEXT_HTML, exchange).block();
		return exchange.getResponse();
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
			configurer.setScripts("org/springframework/web/reactive/result/view/script/jruby/render.rb");
			configurer.setEngineName("jruby");
			configurer.setRenderFunction("render");
			return configurer;
		}
	}

}
