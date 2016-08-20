/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.springframework.beans.Mergeable;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <strong>Main entry point for server-side Spring MVC test support.</strong>
 *
 * <h3>Example</h3>
 *
 * <pre class="code">
 * import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 * import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.perform(get("/form"))
 *     .andExpect(status().isOk())
 *     .andExpect(content().mimeType("text/html"))
 *     .andExpect(forwardedUrl("/WEB-INF/layouts/main.jsp"));
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @since 3.2
 */
public final class MockMvc {

	static String MVC_RESULT_ATTRIBUTE = MockMvc.class.getName().concat(".MVC_RESULT_ATTRIBUTE");

	private final TestDispatcherServlet servlet;

	private final Filter[] filters;

	private final ServletContext servletContext;

	private RequestBuilder defaultRequestBuilder;

	private List<ResultMatcher> defaultResultMatchers = new ArrayList<>();

	private List<ResultHandler> defaultResultHandlers = new ArrayList<>();


	/**
	 * Private constructor, not for direct instantiation.
	 * @see org.springframework.test.web.servlet.setup.MockMvcBuilders
	 */
	MockMvc(TestDispatcherServlet servlet, Filter[] filters, ServletContext servletContext) {

		Assert.notNull(servlet, "DispatcherServlet is required");
		Assert.notNull(filters, "filters cannot be null");
		Assert.noNullElements(filters, "filters cannot contain null values");
		Assert.notNull(servletContext, "A ServletContext is required");

		this.servlet = servlet;
		this.filters = filters;
		this.servletContext = servletContext;
	}

	/**
	 * A default request builder merged into every performed request.
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#defaultRequest(RequestBuilder)
	 */
	void setDefaultRequest(RequestBuilder requestBuilder) {
		this.defaultRequestBuilder = requestBuilder;
	}

	/**
	 * Expectations to assert after every performed request.
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#alwaysExpect(ResultMatcher)
	 */
	void setGlobalResultMatchers(List<ResultMatcher> resultMatchers) {
		Assert.notNull(resultMatchers, "resultMatchers is required");
		this.defaultResultMatchers = resultMatchers;
	}

	/**
	 * General actions to apply after every performed request.
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#alwaysDo(ResultHandler)
	 */
	void setGlobalResultHandlers(List<ResultHandler> resultHandlers) {
		Assert.notNull(resultHandlers, "resultHandlers is required");
		this.defaultResultHandlers = resultHandlers;
	}

	/**
	 * Perform a request and return a type that allows chaining further
	 * actions, such as asserting expectations, on the result.
	 *
	 * @param requestBuilder used to prepare the request to execute;
	 * see static factory methods in
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}
	 *
	 * @return an instance of {@link ResultActions}; never {@code null}
	 *
	 * @see org.springframework.test.web.servlet.request.MockMvcRequestBuilders
	 * @see org.springframework.test.web.servlet.result.MockMvcResultMatchers
	 */
	public ResultActions perform(RequestBuilder requestBuilder) throws Exception {

		if (this.defaultRequestBuilder != null) {
			if (requestBuilder instanceof Mergeable) {
				requestBuilder = (RequestBuilder) ((Mergeable) requestBuilder).merge(this.defaultRequestBuilder);
			}
		}

		MockHttpServletRequest request = requestBuilder.buildRequest(this.servletContext);
		MockHttpServletResponse response = new MockHttpServletResponse();

		if (requestBuilder instanceof SmartRequestBuilder) {
			request = ((SmartRequestBuilder) requestBuilder).postProcessRequest(request);
		}

		final MvcResult mvcResult = new DefaultMvcResult(request, response);
		request.setAttribute(MVC_RESULT_ATTRIBUTE, mvcResult);

		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

		MockFilterChain filterChain = new MockFilterChain(this.servlet, this.filters);
		filterChain.doFilter(request, response);

		if (DispatcherType.ASYNC.equals(request.getDispatcherType()) &&
				request.getAsyncContext() != null & !request.isAsyncStarted()) {

			request.getAsyncContext().complete();
		}

		applyDefaultResultActions(mvcResult);

		RequestContextHolder.setRequestAttributes(previousAttributes);

		return new ResultActions() {

			@Override
			public ResultActions andExpect(ResultMatcher matcher) throws Exception {
				matcher.match(mvcResult);
				return this;
			}

			@Override
			public ResultActions andDo(ResultHandler handler) throws Exception {
				handler.handle(mvcResult);
				return this;
			}

			@Override
			public MvcResult andReturn() {
				return mvcResult;
			}
		};
	}

	private void applyDefaultResultActions(MvcResult mvcResult) throws Exception {

		for (ResultMatcher matcher : this.defaultResultMatchers) {
			matcher.match(mvcResult);
		}

		for (ResultHandler handler : this.defaultResultHandlers) {
			handler.handle(mvcResult);
		}
	}

}
