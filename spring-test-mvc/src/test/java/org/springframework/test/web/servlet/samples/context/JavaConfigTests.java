/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.web.servlet.samples.context;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.samples.context.JavaConfigTests.WebConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.UrlBasedViewResolver;
import org.springframework.web.servlet.view.tiles2.TilesConfigurer;
import org.springframework.web.servlet.view.tiles2.TilesView;

/**
 * Tests with Java configuration.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration("src/test/resources/META-INF/web-resources")
@ContextConfiguration(classes = WebConfig.class)
public class JavaConfigTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;


	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void tilesDefinitions() throws Exception {
		this.mockMvc.perform(get("/"))//
		.andExpect(status().isOk())//
		.andExpect(forwardedUrl("/WEB-INF/layouts/standardLayout.jsp"));
	}


	@Configuration
	@EnableWebMvc
	static class WebConfig extends WebMvcConfigurerAdapter {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
		}

		@Override
		public void addViewControllers(ViewControllerRegistry registry) {
			registry.addViewController("/").setViewName("home");
		}

		@Override
		public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
			configurer.enable();
		}

		@Bean
		public UrlBasedViewResolver urlBasedViewResolver() {
			UrlBasedViewResolver resolver = new UrlBasedViewResolver();
			resolver.setViewClass(TilesView.class);
			return resolver;
		}

		@Bean
		public TilesConfigurer tilesConfigurer() {
			TilesConfigurer configurer = new TilesConfigurer();
			configurer.setDefinitions(new String[] {"/WEB-INF/**/tiles.xml"});
			return configurer;
		}
	}

}
