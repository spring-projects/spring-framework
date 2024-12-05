/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for rendering through {@link DefaultFragmentsRendering}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class DefaultFragmentsRenderingTests {

	@Test
	void render() throws Exception {

		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ScriptTemplatingConfiguration.class);

		String prefix = "org/springframework/web/servlet/view/script/jython/";
		ScriptTemplateViewResolver viewResolver = new ScriptTemplateViewResolver(prefix, ".html");
		viewResolver.setApplicationContext(context);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		FragmentsRendering view = FragmentsRendering
				.fragment("fragment1", Map.of("foo", "Foo"))
				.fragment("fragment2", Map.of("bar", "Bar"))
				.header("headerName", "headerValue")
				.build();

		view.resolveNestedViews(viewResolver, Locale.ENGLISH);
		view.render(Collections.emptyMap(), request, response);

		assertThat(response.getHeader("headerName")).isEqualTo("headerValue");
		assertThat(response.getContentAsString()).isEqualTo("""
				<p>
					Hello Foo
				</p>\
				<p>
					Hello Bar
				</p>""");
	}


	@Configuration
	static class ScriptTemplatingConfiguration {

		@Bean
		ScriptTemplateConfigurer kotlinScriptConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setEngineName("jython");
			configurer.setScripts("org/springframework/web/servlet/view/script/jython/render.py");
			configurer.setRenderFunction("render");
			return configurer;
		}

	}

}
