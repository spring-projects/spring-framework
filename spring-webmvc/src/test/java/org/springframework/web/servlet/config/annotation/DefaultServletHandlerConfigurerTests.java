/*
 * Copyright 2002-2017 the original author or authors.
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

import javax.servlet.RequestDispatcher;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockRequestDispatcher;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

import static org.junit.Assert.*;

/**
 * Test fixture with a {@link DefaultServletHandlerConfigurer}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultServletHandlerConfigurerTests {

	private DefaultServletHandlerConfigurer configurer;

	private DispatchingMockServletContext servletContext;

	private MockHttpServletResponse response;


	@Before
	public void setup() {
		response = new MockHttpServletResponse();
		servletContext = new DispatchingMockServletContext();
		configurer = new DefaultServletHandlerConfigurer(servletContext);
	}


	@Test
	public void notEnabled() {
		assertNull(configurer.buildHandlerMapping());
	}

	@Test
	public void enable() throws Exception {
		configurer.enable();
		SimpleUrlHandlerMapping handlerMapping = configurer.buildHandlerMapping();
		DefaultServletHttpRequestHandler handler = (DefaultServletHttpRequestHandler) handlerMapping.getUrlMap().get("/**");

		assertNotNull(handler);
		assertEquals(Integer.MAX_VALUE, handlerMapping.getOrder());

		handler.handleRequest(new MockHttpServletRequest(), response);

		String expected = "default";
		assertEquals("The ServletContext was not called with the default servlet name", expected, servletContext.url);
		assertEquals("The request was not forwarded", expected, response.getForwardedUrl());
	}

	@Test
	public void enableWithServletName() throws Exception {
		configurer.enable("defaultServlet");
		SimpleUrlHandlerMapping handlerMapping = configurer.buildHandlerMapping();
		DefaultServletHttpRequestHandler handler = (DefaultServletHttpRequestHandler) handlerMapping.getUrlMap().get("/**");

		assertNotNull(handler);
		assertEquals(Integer.MAX_VALUE, handlerMapping.getOrder());

		handler.handleRequest(new MockHttpServletRequest(), response);

		String expected = "defaultServlet";
		assertEquals("The ServletContext was not called with the default servlet name", expected, servletContext.url);
		assertEquals("The request was not forwarded", expected, response.getForwardedUrl());
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
