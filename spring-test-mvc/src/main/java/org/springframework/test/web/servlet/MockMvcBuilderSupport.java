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

package org.springframework.test.web.servlet;

import java.util.List;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.core.NestedRuntimeException;
import org.springframework.mock.web.MockFilterChain;
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
 * @since 3.2
 */
public abstract class MockMvcBuilderSupport {

	protected final MockMvc createMockMvc(Filter[] filters, MockServletConfig servletConfig,
			WebApplicationContext webAppContext, RequestBuilder defaultRequestBuilder,
			List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers, Boolean dispatchOptions) {

		ServletContext servletContext = webAppContext.getServletContext();

		TestDispatcherServlet dispatcherServlet = new TestDispatcherServlet(webAppContext);
		dispatcherServlet.setDispatchOptionsRequest(dispatchOptions);
		try {
			dispatcherServlet.init(servletConfig);
		}
		catch (ServletException ex) {
			// should never happen..
			throw new MockMvcBuildException("Failed to initialize TestDispatcherServlet", ex);
		}

		MockFilterChain filterChain = new MockFilterChain(dispatcherServlet, filters);

		MockMvc mockMvc = new MockMvc(filterChain, servletContext);
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
