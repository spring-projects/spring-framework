/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config.annotation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletConfig;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for view resolution with {@code @EnableWebMvc}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ViewResolutionIntegrationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Test
	public void minimalFreemarkerConfig() throws Exception {
		MockHttpServletResponse response = runTest(MinimalFreeMarkerWebConfig.class);
		assertEquals("<html><body>Hello World!</body></html>", response.getContentAsString());
	}

	@Test
	public void minimalVelocityConfig() throws Exception {
		MockHttpServletResponse response = runTest(MinimalVelocityWebConfig.class);
		assertEquals("<html><body>Hello World!</body></html>", response.getContentAsString());
	}

	@Test
	public void minimalTilesConfig() throws Exception {
		MockHttpServletResponse response = runTest(MinimalTilesWebConfig.class);
		assertEquals("/WEB-INF/index.jsp", response.getForwardedUrl());
	}

	@Test
	public void freemarker() throws Exception {
		MockHttpServletResponse response = runTest(FreeMarkerWebConfig.class);
		assertEquals("<html><body>Hello World!</body></html>", response.getContentAsString());
	}

	@Test
	public void velocity() throws Exception {
		MockHttpServletResponse response = runTest(VelocityWebConfig.class);
		assertEquals("<html><body>Hello World!</body></html>", response.getContentAsString());
	}

	@Test
	public void tiles() throws Exception {
		MockHttpServletResponse response = runTest(TilesWebConfig.class);
		assertEquals("/WEB-INF/index.jsp", response.getForwardedUrl());
	}

	@Test
	public void freemarkerInvalidConfig() throws Exception {
		this.thrown.expectMessage("It looks like you're trying to configure FreeMarker view resolution.");
		runTest(InvalidFreeMarkerWebConfig.class);
	}

	@Test
	public void velocityInvalidConfig() throws Exception {
		this.thrown.expectMessage("It looks like you're trying to configure Velocity view resolution.");
		runTest(InvalidVelocityWebConfig.class);
	}

	@Test
	public void tilesInvalidConfig() throws Exception {
		this.thrown.expectMessage("It looks like you're trying to configure Tiles view resolution.");
		runTest(InvalidTilesWebConfig.class);
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
		public String tiles(@ModelAttribute("model") ModelMap model) {
			model.addAttribute("hello", "Hello World!");
			return "index";
		}
	}

	@EnableWebMvc
	static abstract class AbstractWebConfig extends WebMvcConfigurerAdapter {
		@Bean
		public SampleController sampleController() {
			return new SampleController();
		}
	}

	@Configuration
	static class MinimalFreeMarkerWebConfig extends AbstractWebConfig {
		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.freeMarker();
		}
	}

	@Configuration
	static class MinimalVelocityWebConfig extends AbstractWebConfig {
		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.velocity();
		}
	}

	@Configuration
	static class MinimalTilesWebConfig extends AbstractWebConfig {
		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.tiles();
		}
	}

	@Configuration
	static class FreeMarkerWebConfig extends AbstractWebConfig implements FreeMarkerWebMvcConfigurer {
		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.freeMarker();
		}
		@Override
		public void configureFreeMarker(FreeMarkerConfigurer configurer) {
			configurer.setTemplateLoaderPath("/WEB-INF/");
		}
	}

	@Configuration
	static class VelocityWebConfig extends AbstractWebConfig implements VelocityWebMvcConfigurer {
		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.velocity();
		}
		@Override
		public void configureVelocity(VelocityConfigurer configurer) {
			configurer.setResourceLoaderPath("/WEB-INF/");
		}
	}

	@Configuration
	static class TilesWebConfig extends AbstractWebConfig implements TilesWebMvcConfigurer {
		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.tiles();
		}
		@Override
		public void configureTiles(TilesConfigurer configurer) {
			configurer.setDefinitions("/WEB-INF/tiles.xml");
		}
	}
	@Configuration
	static class InvalidFreeMarkerWebConfig extends WebMvcConfigurationSupport {

		// No @EnableWebMvc and no FreeMarkerConfigurer bean

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.freeMarker();
		}
	}

	@Configuration
	static class InvalidVelocityWebConfig extends WebMvcConfigurationSupport {

		// No @EnableWebMvc and no VelocityConfigurer bean

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.velocity();
		}
	}

	@Configuration
	static class InvalidTilesWebConfig extends WebMvcConfigurationSupport {

		// No @EnableWebMvc and no TilesConfigurer bean

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.tiles();
		}
	}

}
