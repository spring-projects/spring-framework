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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for API versioning.
 * @author Rossen Stoyanchev
 */
public class RequestMappingVersionHandlerMethodTests {

	private DispatcherServlet dispatcherServlet;


	@BeforeEach
	void setUp() throws ServletException {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletConfig(new MockServletConfig());
		context.register(WebConfig.class, TestController.class);
		context.afterPropertiesSet();

		this.dispatcherServlet = new DispatcherServlet(context);
		this.dispatcherServlet.init(new MockServletConfig());
	}


	@Test
	void mapVersion() throws Exception {
		assertThat(requestWithVersion("1.0").getContentAsString()).isEqualTo("none");
		assertThat(requestWithVersion("1.1").getContentAsString()).isEqualTo("none");
		assertThat(requestWithVersion("1.2").getContentAsString()).isEqualTo("1.2");
		assertThat(requestWithVersion("1.3").getContentAsString()).isEqualTo("1.2");
		assertThat(requestWithVersion("1.5").getContentAsString()).isEqualTo("1.5");

		MockHttpServletResponse response = requestWithVersion("1.6");
		assertThat(response.getStatus())
				.as("Should reject if highest supported below request version is fixed")
				.isEqualTo(400);
	}

	@Test
	void deprecation() throws Exception {
		assertThat(requestWithVersion("1").getHeader("Link"))
				.isEqualTo("<https://example.org/deprecation>; rel=\"deprecation\"; type=\"text/html\"");
	}

	private MockHttpServletResponse requestWithVersion(String version) throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("X-API-VERSION", version);
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.dispatcherServlet.service(request, response);
		return response;
	}


	@EnableWebMvc
	private static class WebConfig implements WebMvcConfigurer {

		@Override
		public void configureApiVersioning(ApiVersionConfigurer configurer) {

			StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
			handler.configureVersion("1").setDeprecationLink(URI.create("https://example.org/deprecation"));

			configurer.useRequestHeader("X-API-Version")
					.addSupportedVersions("1", "1.1", "1.3", "1.6")
					.setDeprecationHandler(handler);
		}
	}


	@RestController
	private static class TestController {

		@GetMapping
		String noVersion() {
			return getBody("none");
		}

		@GetMapping(version = "1.2+")
		String version1_2() {
			return getBody("1.2");
		}

		@GetMapping(version = "1.5")
		String version1_5() {
			return getBody("1.5");
		}

		private static String getBody(String version) {
			return version;
		}
	}

}
