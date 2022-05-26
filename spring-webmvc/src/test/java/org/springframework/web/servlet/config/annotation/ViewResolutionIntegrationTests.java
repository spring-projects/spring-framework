/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.io.IOException;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for view resolution with {@code @EnableWebMvc}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ViewResolutionIntegrationTests {

	@Test
	public void freemarker() throws Exception {
		MockHttpServletResponse response = runTest(FreeMarkerWebConfig.class);
		assertThat(response.getContentAsString()).isEqualTo("<html><body>Hello World!</body></html>");
	}

	@Test
	public void groovyMarkup() throws Exception {
		MockHttpServletResponse response = runTest(GroovyMarkupWebConfig.class);
		assertThat(response.getContentAsString()).isEqualTo("<html><body>Hello World!</body></html>");
	}

	@Test
	public void freemarkerInvalidConfig() throws Exception {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
				runTest(InvalidFreeMarkerWebConfig.class))
			.withMessageContaining("In addition to a FreeMarker view resolver ");
	}

	@Test
	public void groovyMarkupInvalidConfig() throws Exception {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
				runTest(InvalidGroovyMarkupWebConfig.class))
			.withMessageContaining("In addition to a Groovy markup view resolver ");
	}

	// SPR-12013

	@Test
	public void existingViewResolver() throws Exception {
		MockHttpServletResponse response = runTest(ExistingViewResolverConfig.class);
		assertThat(response.getContentAsString()).isEqualTo("<html><body>Hello World!</body></html>");
	}


	private MockHttpServletResponse runTest(Class<?> configClass) throws ServletException, IOException {
		String basePath = "org/springframework/web/servlet/config/annotation";
		MockServletContext servletContext = new MockServletContext(basePath);
		MockServletConfig servletConfig = new MockServletConfig(servletContext);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();

		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(configClass);
		context.setServletContext(servletContext);
		context.refresh();
		DispatcherServlet servlet = new DispatcherServlet(context);
		servlet.init(servletConfig);
		servlet.service(request, response);
		return response;
	}


	@Controller
	static class SampleController {

		@RequestMapping(value = "/", method = RequestMethod.GET)
		public String sample(ModelMap model) {
			model.addAttribute("hello", "Hello World!");
			return "index";
		}
	}

	@EnableWebMvc
	static abstract class AbstractWebConfig implements WebMvcConfigurer {

		@Bean
		public SampleController sampleController() {
			return new SampleController();
		}
	}

	@Configuration
	static class FreeMarkerWebConfig extends AbstractWebConfig {

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.freeMarker();
		}

		@Bean
		public FreeMarkerConfigurer freeMarkerConfigurer() {
			FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
			configurer.setTemplateLoaderPath("/WEB-INF/");
			return configurer;
		}
	}

	@Configuration
	static class GroovyMarkupWebConfig extends AbstractWebConfig {

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.groovy();
		}

		@Bean
		public GroovyMarkupConfigurer groovyMarkupConfigurer() {
			GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
			configurer.setResourceLoaderPath("/WEB-INF/");
			return configurer;
		}
	}

	@Configuration
	static class InvalidFreeMarkerWebConfig extends WebMvcConfigurationSupport {

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.freeMarker();
		}
	}

	@Configuration
	static class InvalidGroovyMarkupWebConfig extends WebMvcConfigurationSupport {

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.groovy();
		}
	}

	/**
	 * Test @EnableWebMvc in the presence of pre-existing ViewResolver.
	 */
	@Configuration
	static class ExistingViewResolverConfig extends AbstractWebConfig {

		@Bean
		public FreeMarkerViewResolver freeMarkerViewResolver() {
			return new FreeMarkerViewResolver("", ".ftl");
		}

		@Bean
		public FreeMarkerConfigurer freeMarkerConfigurer() {
			FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
			configurer.setTemplateLoaderPath("/WEB-INF/");
			return configurer;
		}
	}

}
