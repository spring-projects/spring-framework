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

package org.springframework.web.servlet.function.support;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.function.RequestPredicates.version;

/**
 * {@link RouterFunctionMapping} integration tests for API versioning.
 * @author Rossen Stoyanchev
 */
public class RouterFunctionMappingVersionTests {

	private final MockServletContext servletContext = new MockServletContext();

	private RouterFunctionMapping mapping;


	@BeforeEach
	void setUp() {
		AnnotationConfigWebApplicationContext wac = new AnnotationConfigWebApplicationContext();
		wac.setServletContext(this.servletContext);
		wac.register(WebConfig.class);
		wac.refresh();

		this.mapping = wac.getBean(RouterFunctionMapping.class);
	}


	@Test
	void mapVersion() throws Exception {
		testGetHandler("1.0", "none");
		testGetHandler("1.1", "none");
		testGetHandler("1.2", "1.2");
		testGetHandler("1.3", "1.2");
		testGetHandler("1.5", "1.5");
	}

	private void testGetHandler(String version, String expectedBody) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("X-API-Version", version);
		HandlerFunction<?> handler = (HandlerFunction<?>) this.mapping.getHandler(request).getHandler();
		assertThat(((TestHandler) handler).body()).isEqualTo(expectedBody);
	}

	@Test
	void deprecation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("X-API-Version", "1");

		HandlerExecutionChain chain = this.mapping.getHandler(request);
		assertThat(chain).isNotNull();

		MockHttpServletResponse response = new MockHttpServletResponse();
		for (HandlerInterceptor interceptor : chain.getInterceptorList()) {
			interceptor.preHandle(request, response, chain.getHandler());
		}

		assertThat(((TestHandler) chain.getHandler()).body()).isEqualTo("none");
		assertThat(response.getHeader("Link"))
				.isEqualTo("<https://example.org/deprecation>; rel=\"deprecation\"; type=\"text/html\"");
	}


	@EnableWebMvc
	private static class WebConfig implements WebMvcConfigurer {

		@Override
		public void configureApiVersioning(ApiVersionConfigurer configurer) {

			StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
			handler.configureVersion("1").setDeprecationLink(URI.create("https://example.org/deprecation"));

			configurer.useRequestHeader("X-API-Version")
					.addSupportedVersions("1", "1.1", "1.3")
					.setDeprecationHandler(handler);
		}

		@Bean
		RouterFunction<?> routerFunction() {
			return RouterFunctions.route()
					.path("/", builder -> builder
							.GET(version("1.5"), new TestHandler("1.5"))
							.GET(version("1.2+"), new TestHandler("1.2"))
							.GET(new TestHandler("none")))
					.build();
		}
	}


	private record TestHandler(String body) implements HandlerFunction<ServerResponse> {

		@Override
		public ServerResponse handle(ServerRequest request) {
			return ServerResponse.ok().body(body);
		}
	}

}
