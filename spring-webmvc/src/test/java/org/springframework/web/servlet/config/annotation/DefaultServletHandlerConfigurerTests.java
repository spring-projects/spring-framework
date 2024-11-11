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

package org.springframework.web.servlet.config.annotation;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockRequestDispatcher;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with a {@link DefaultServletHandlerConfigurer}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultServletHandlerConfigurerTests {

	private DefaultServletHandlerConfigurer configurer;

	private DispatchingMockServletContext servletContext;

	private MockHttpServletResponse response;


	@BeforeEach
	void setup() {
		response = new MockHttpServletResponse();
		servletContext = new DispatchingMockServletContext();
		configurer = new DefaultServletHandlerConfigurer(servletContext);
	}


	@Test
	void notEnabled() {
		assertThat(configurer.buildHandlerMapping()).isNull();
	}

	@Test
	void enable() throws Exception {
		configurer.enable();
		SimpleUrlHandlerMapping mapping = configurer.buildHandlerMapping();
		HttpRequestHandler handler = (DefaultServletHttpRequestHandler) mapping.getUrlMap().get("/**");

		assertThat(handler).isNotNull();
		assertThat(mapping.getOrder()).isEqualTo(Integer.MAX_VALUE);

		handler.handleRequest(new MockHttpServletRequest(), response);

		assertThat(servletContext.url)
				.as("The ServletContext was not called with the default servlet name").isEqualTo("default");

		assertThat(response.getForwardedUrl())
				.as("The request was not forwarded").isEqualTo("default");
	}

	@Test
	void enableWithServletName() throws Exception {
		configurer.enable("defaultServlet");
		SimpleUrlHandlerMapping mapping = configurer.buildHandlerMapping();
		HttpRequestHandler handler = (DefaultServletHttpRequestHandler) mapping.getUrlMap().get("/**");

		assertThat(handler).isNotNull();
		assertThat(mapping.getOrder()).isEqualTo(Integer.MAX_VALUE);

		handler.handleRequest(new MockHttpServletRequest(), response);

		assertThat(servletContext.url)
				.as("The ServletContext was not called with the default servlet name").isEqualTo("defaultServlet");

		assertThat(response.getForwardedUrl())
				.as("The request was not forwarded").isEqualTo("defaultServlet");
	}

	@Test // gh-30113
	public void handleIncludeRequest() throws Exception {
		configurer.enable();
		SimpleUrlHandlerMapping mapping = configurer.buildHandlerMapping();
		HttpRequestHandler handler = (DefaultServletHttpRequestHandler) mapping.getUrlMap().get("/**");

		assertThat(handler).isNotNull();
		assertThat(mapping.getOrder()).isEqualTo(Integer.MAX_VALUE);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setDispatcherType(DispatcherType.INCLUDE);
		handler.handleRequest(request, response);

		assertThat(servletContext.url)
				.as("The ServletContext was not called with the default servlet name").isEqualTo("default");

		assertThat(response.getIncludedUrl())
				.as("The request was not included").isEqualTo("default");
	}


	private static class DispatchingMockServletContext extends MockServletContext {

		private String url;

		@Override
		public RequestDispatcher getNamedDispatcher(String url) {
			this.url = url;
			return new MockRequestDispatcher(url);
		}
	}

}
