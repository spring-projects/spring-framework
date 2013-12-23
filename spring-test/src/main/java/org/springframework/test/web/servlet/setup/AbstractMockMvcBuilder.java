/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.web.servlet.*;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstract implementation of {@link org.springframework.test.web.servlet.MockMvcBuilder}
 * with common methods for configuring filters, default request properties, global
 * expectations and global result actions.
 * <p>
 * Sub-classes can use different strategies to prepare a WebApplicationContext to
 * pass to the DispatcherServlet.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractMockMvcBuilder<B extends AbstractMockMvcBuilder<B>>
		extends MockMvcBuilderSupport implements MockMvcBuilder {

	private List<Filter> filters = new ArrayList<Filter>();

	private RequestBuilder defaultRequestBuilder;

	private final List<ResultMatcher> globalResultMatchers = new ArrayList<ResultMatcher>();

	private final List<ResultHandler> globalResultHandlers = new ArrayList<ResultHandler>();

	private Boolean dispatchOptions = Boolean.FALSE;



	/**
	 * Add filters mapped to any request (i.e. "/*"). For example:
	 *
	 * <pre class="code">
	 * mockMvcBuilder.addFilters(springSecurityFilterChain);
	 * </pre>
	 *
	 * <p>is the equivalent of the following web.xml configuration:
	 *
	 * <pre class="code">
	 * &lt;filter-mapping&gt;
	 *     &lt;filter-name&gt;springSecurityFilterChain&lt;/filter-name&gt;
	 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
	 * &lt;/filter-mapping&gt;
	 * </pre>
	 *
	 * <p>Filters will be invoked in the order in which they are provided.
	 *
	 * @param filters the filters to add
	 */
	@SuppressWarnings("unchecked")
	public final <T extends B> T addFilters(Filter... filters) {
		Assert.notNull(filters, "filters cannot be null");

		for(Filter f : filters) {
			Assert.notNull(f, "filters cannot contain null values");
			this.filters.add(f);
		}
		return (T) this;
	}

	/**
	 * Add a filter mapped to a specific set of patterns. For example:
	 *
	 * <pre class="code">
	 * mockMvcBuilder.addFilters(myResourceFilter, "/resources/*");
	 * </pre>
	 *
	 * <p>is the equivalent of:
	 *
	 * <pre class="code">
	 * &lt;filter-mapping&gt;
	 *     &lt;filter-name&gt;myResourceFilter&lt;/filter-name&gt;
	 *     &lt;url-pattern&gt;/resources/*&lt;/url-pattern&gt;
	 * &lt;/filter-mapping&gt;
	 * </pre>
	 *
	 * <p>Filters will be invoked in the order in which they are provided.
	 *
	 * @param filter the filter to add
	 * @param urlPatterns URL patterns to map to; if empty, "/*" is used by default
	 */
	@SuppressWarnings("unchecked")
	public final <T extends B> T addFilter(Filter filter, String... urlPatterns) {

		Assert.notNull(filter, "filter cannot be null");
		Assert.notNull(urlPatterns, "urlPatterns cannot be null");

		if(urlPatterns.length > 0) {
			filter = new PatternMappingFilterProxy(filter, urlPatterns);
		}

		this.filters.add(filter);
		return (T) this;
	}

	/**
	 * Define default request properties that should be merged into all
	 * performed requests. In effect this provides a mechanism for defining
	 * common initialization for all requests such as the content type, request
	 * parameters, session attributes, and any other request property.
	 *
	 * <p>Properties specified at the time of performing a request override the
	 * default properties defined here.
	 *
	 * @param requestBuilder a RequestBuilder; see static factory methods in
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}
	 * .
	 */
	@SuppressWarnings("unchecked")
	public final <T extends B> T defaultRequest(RequestBuilder requestBuilder) {
		this.defaultRequestBuilder = requestBuilder;
		return (T) this;
	}

	/**
	 * Define a global expectation that should <em>always</em> be applied to
	 * every response. For example, status code 200 (OK), content type
	 * {@code "application/json"}, etc.
	 *
	 * @param resultMatcher a ResultMatcher; see static factory methods in
	 * {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers}
	 */
	@SuppressWarnings("unchecked")
	public final <T extends B> T alwaysExpect(ResultMatcher resultMatcher) {
		this.globalResultMatchers.add(resultMatcher);
		return (T) this;
	}

	/**
	 * Define a global action that should <em>always</em> be applied to every
	 * response. For example, writing detailed information about the performed
	 * request and resulting response to {@code System.out}.
	 *
	 * @param resultHandler a ResultHandler; see static factory methods in
	 * {@link org.springframework.test.web.servlet.result.MockMvcResultHandlers}
	 */
	@SuppressWarnings("unchecked")
	public final <T extends B> T alwaysDo(ResultHandler resultHandler) {
		this.globalResultHandlers.add(resultHandler);
		return (T) this;
	}

	/**
	 * Should the {@link org.springframework.web.servlet.DispatcherServlet} dispatch OPTIONS request to controllers.
	 * @param dispatchOptions
	 * @see org.springframework.web.servlet.DispatcherServlet#setDispatchOptionsRequest(boolean)
	 */
	@SuppressWarnings("unchecked")
	public final <T extends B> T dispatchOptions(boolean dispatchOptions) {
		this.dispatchOptions = dispatchOptions;
		return (T) this;
	}

	/**
	 * Build a {@link org.springframework.test.web.servlet.MockMvc} instance.
	 */
	@Override
	public final MockMvc build() {

		WebApplicationContext wac = initWebAppContext();

		ServletContext servletContext = wac.getServletContext();
		MockServletConfig mockServletConfig = new MockServletConfig(servletContext);

		Filter[] filterArray = this.filters.toArray(new Filter[this.filters.size()]);

		return super.createMockMvc(filterArray, mockServletConfig, wac, this.defaultRequestBuilder,
				this.globalResultMatchers, this.globalResultHandlers, this.dispatchOptions);
	}

	/**
	 * A method to obtain the WebApplicationContext to be passed to the DispatcherServlet.
	 * Invoked from {@link #build()} before the
	 * {@link org.springframework.test.web.servlet.MockMvc} instance is created.
	 */
	protected abstract WebApplicationContext initWebAppContext();

}
