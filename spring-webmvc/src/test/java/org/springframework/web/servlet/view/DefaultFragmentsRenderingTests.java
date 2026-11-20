/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class DefaultFragmentsRenderingTests {

	@Test
	void render() throws Exception {

		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ScriptTemplatingConfiguration.class);

		String prefix = "org/springframework/web/servlet/view/script/jython/";
		ScriptTemplateViewResolver viewResolver = new ScriptTemplateViewResolver(prefix, ".html");
		viewResolver.setApplicationContext(context);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		FragmentsRendering view = FragmentsRendering.fragment("fragment1", Map.of("foo", "Foo"))
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
