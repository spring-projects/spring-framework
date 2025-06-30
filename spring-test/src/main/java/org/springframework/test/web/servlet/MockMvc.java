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
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.Mergeable;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.DispatcherServlet;

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
 *     .andExpectAll(
 *         status().isOk(),
 *         content().contentType("text/html"),
 *         forwardedUrl("/WEB-INF/layouts/main.jsp")
 *     );
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @since 3.2
 */
public final class MockMvc {

	static final String MVC_RESULT_ATTRIBUTE = MockMvc.class.getName().concat(".MVC_RESULT_ATTRIBUTE");

	private final TestDispatcherServlet servlet;

	private final Filter[] filters;

	private final ServletContext servletContext;

	private @Nullable RequestBuilder defaultRequestBuilder;

	private @Nullable Charset defaultResponseCharacterEncoding;

	private List<ResultMatcher> defaultResultMatchers = new ArrayList<>();

	private List<ResultHandler> defaultResultHandlers = new ArrayList<>();


	/**
	 * Private constructor, not for direct instantiation.
	 * @see org.springframework.test.web.servlet.setup.MockMvcBuilders
	 */
	MockMvc(TestDispatcherServlet servlet, Filter... filters) {
		Assert.notNull(servlet, "DispatcherServlet is required");
		Assert.notNull(filters, "Filters cannot be null");
		Assert.noNullElements(filters, "Filters cannot contain null values");

		this.servlet = servlet;
		this.filters = filters;
		this.servletContext = servlet.getServletContext();
	}


	/**
	 * A default request builder merged into every performed request.
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#defaultRequest(RequestBuilder)
	 */
	void setDefaultRequest(@Nullable RequestBuilder requestBuilder) {
		this.defaultRequestBuilder = requestBuilder;
	}

	/**
	 * The default character encoding to be applied to every response.
	 * @see org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder#defaultResponseCharacterEncoding(Charset)
	 */
	void setDefaultResponseCharacterEncoding(@Nullable Charset defaultResponseCharacterEncoding) {
		this.defaultResponseCharacterEncoding = defaultResponseCharacterEncoding;
	}

	/**
	 * Expectations to assert after every performed request.
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#alwaysExpect(ResultMatcher)
	 */
	void setGlobalResultMatchers(List<ResultMatcher> resultMatchers) {
		Assert.notNull(resultMatchers, "ResultMatcher List is required");
		this.defaultResultMatchers = resultMatchers;
	}

	/**
	 * General actions to apply after every performed request.
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#alwaysDo(ResultHandler)
	 */
	void setGlobalResultHandlers(List<ResultHandler> resultHandlers) {
		Assert.notNull(resultHandlers, "ResultHandler List is required");
		this.defaultResultHandlers = resultHandlers;
	}

	/**
	 * Return the underlying {@link DispatcherServlet} instance that this
	 * {@code MockMvc} was initialized with.
	 * <p>This is intended for use in custom request processing scenario where a
	 * request handling component happens to delegate to the {@code DispatcherServlet}
	 * at runtime and therefore needs to be injected with it.
	 * <p>For most processing scenarios, simply use {@link MockMvc#perform},
	 * or if you need to configure the {@code DispatcherServlet}, provide a
	 * {@link DispatcherServletCustomizer} to the {@code MockMvcBuilder}.
	 * @since 5.1
	 */
	public DispatcherServlet getDispatcherServlet() {
		return this.servlet;
	}


	/**
	 * Perform a request and return a type that allows chaining further
	 * actions, such as asserting expectations, on the result.
	 * @param requestBuilder used to prepare the request to execute;
	 * see static factory methods in
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}
	 * @return an instance of {@link ResultActions} (never {@code null})
	 * @see org.springframework.test.web.servlet.request.MockMvcRequestBuilders
	 * @see org.springframework.test.web.servlet.result.MockMvcResultMatchers
	 */
	public ResultActions perform(RequestBuilder requestBuilder) throws Exception {
		if (this.defaultRequestBuilder != null && requestBuilder instanceof Mergeable mergeable) {
			requestBuilder = (RequestBuilder) mergeable.merge(this.defaultRequestBuilder);
		}

		MockHttpServletRequest request = requestBuilder.buildRequest(this.servletContext);

		AsyncContext asyncContext = request.getAsyncContext();
		MockHttpServletResponse mockResponse;
		HttpServletResponse servletResponse;
		if (asyncContext != null) {
			servletResponse = (HttpServletResponse) asyncContext.getResponse();
			mockResponse = unwrapResponseIfNecessary(servletResponse);
		}
		else {
			mockResponse = new MockHttpServletResponse();
			servletResponse = mockResponse;
		}

		if (this.defaultResponseCharacterEncoding != null) {
			mockResponse.setDefaultCharacterEncoding(this.defaultResponseCharacterEncoding.name());
		}

		if (requestBuilder instanceof SmartRequestBuilder smartRequestBuilder) {
			request = smartRequestBuilder.postProcessRequest(request);
		}

		MvcResult mvcResult = new DefaultMvcResult(request, mockResponse);
		request.setAttribute(MVC_RESULT_ATTRIBUTE, mvcResult);

		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, servletResponse));

		MockFilterChain filterChain = new MockFilterChain(this.servlet, this.filters);
		filterChain.doFilter(request, servletResponse);

		if (DispatcherType.ASYNC.equals(request.getDispatcherType()) &&
				asyncContext != null && !request.isAsyncStarted()) {
			asyncContext.complete();
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

	private MockHttpServletResponse unwrapResponseIfNecessary(ServletResponse servletResponse) {
		while (servletResponse instanceof HttpServletResponseWrapper wrapper) {
			servletResponse = wrapper.getResponse();
		}
		Assert.isInstanceOf(MockHttpServletResponse.class, servletResponse);
		return (MockHttpServletResponse) servletResponse;
	}

	private void applyDefaultResultActions(MvcResult mvcResult) throws Exception {
		for (ResultHandler handler : this.defaultResultHandlers) {
			handler.handle(mvcResult);
		}
		for (ResultMatcher matcher : this.defaultResultMatchers) {
			matcher.match(mvcResult);
		}
	}

}
