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

package org.springframework.test.web.servlet;

import java.nio.charset.Charset;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for MockMvc builder implementations, providing the capability to
 * create a {@link MockMvc} instance.
 *
 * <p>{@link org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder},
 * which derives from this class, provides a concrete {@code build} method,
 * and delegates to abstract methods to obtain a {@link WebApplicationContext}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.2
 */
public abstract class MockMvcBuilderSupport {

	/**
	 * Delegates to {@link #createMockMvc(Filter[], MockServletConfig, WebApplicationContext, RequestBuilder, List, List, List)}
	 * for creation of the {@link MockMvc} instance and configures that instance
	 * with the supplied {@code defaultResponseCharacterEncoding}.
	 * @since 5.3.10
	 */
	protected final MockMvc createMockMvc(Filter[] filters, MockServletConfig servletConfig,
			WebApplicationContext webAppContext, @Nullable RequestBuilder defaultRequestBuilder,
			@Nullable Charset defaultResponseCharacterEncoding,
			List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers,
			@Nullable List<DispatcherServletCustomizer> dispatcherServletCustomizers) {

		MockMvc mockMvc = createMockMvc(
				filters, servletConfig, webAppContext, defaultRequestBuilder,
				globalResultMatchers, globalResultHandlers, dispatcherServletCustomizers);

		mockMvc.setDefaultResponseCharacterEncoding(defaultResponseCharacterEncoding);
		return mockMvc;
	}

	protected final MockMvc createMockMvc(Filter[] filters, MockServletConfig servletConfig,
			WebApplicationContext webAppContext, @Nullable RequestBuilder defaultRequestBuilder,
			List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers,
			@Nullable List<DispatcherServletCustomizer> dispatcherServletCustomizers) {

		TestDispatcherServlet dispatcherServlet = new TestDispatcherServlet(webAppContext);
		if (dispatcherServletCustomizers != null) {
			for (DispatcherServletCustomizer customizers : dispatcherServletCustomizers) {
				customizers.customize(dispatcherServlet);
			}
		}
		try {
			dispatcherServlet.init(servletConfig);
		}
		catch (ServletException ex) {
			// should never happen...
			throw new MockMvcBuildException("Failed to initialize TestDispatcherServlet", ex);
		}

		MockMvc mockMvc = new MockMvc(dispatcherServlet, filters);
		mockMvc.setDefaultRequest(defaultRequestBuilder);
		mockMvc.setGlobalResultMatchers(globalResultMatchers);
		mockMvc.setGlobalResultHandlers(globalResultHandlers);

		return mockMvc;
	}


	@SuppressWarnings("serial")
	private static class MockMvcBuildException extends NestedRuntimeException {

		public MockMvcBuildException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}

}
