/*
 * Copyright 2002-2017 the original author or authors.
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

import javax.servlet.Filter;

import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Defines common methods for building a {@code MockMvc}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface ConfigurableMockMvcBuilder<B extends ConfigurableMockMvcBuilder<B>>
		extends MockMvcBuilder {

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
	<T extends B> T addFilters(Filter... filters);

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
	<T extends B> T addFilter(Filter filter, String... urlPatterns);

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
	<T extends B> T defaultRequest(RequestBuilder requestBuilder);

	/**
	 * Define a global expectation that should <em>always</em> be applied to
	 * every response. For example, status code 200 (OK), content type
	 * {@code "application/json"}, etc.
	 *
	 * @param resultMatcher a ResultMatcher; see static factory methods in
	 * {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers}
	 */
	<T extends B> T alwaysExpect(ResultMatcher resultMatcher);

	/**
	 * Define a global action that should <em>always</em> be applied to every
	 * response. For example, writing detailed information about the performed
	 * request and resulting response to {@code System.out}.
	 *
	 * @param resultHandler a ResultHandler; see static factory methods in
	 * {@link org.springframework.test.web.servlet.result.MockMvcResultHandlers}
	 */
	<T extends B> T alwaysDo(ResultHandler resultHandler);

	/**
	 * Whether to enable the DispatcherServlet property
	 * {@link org.springframework.web.servlet.DispatcherServlet#setDispatchOptionsRequest
	 * dispatchOptionsRequest} which allows processing of HTTP OPTIONS requests.
	 */
	<T extends B> T dispatchOptions(boolean dispatchOptions);

	/**
	 * Add a {@code MockMvcConfigurer} that automates MockMvc setup and
	 * configures it for some specific purpose (e.g. security).
	 *
	 * <p>There is a built-in {@link SharedHttpSessionConfigurer} that can be
	 * used to re-use the HTTP session across requests. 3rd party frameworks
	 * like Spring Security also use this mechanism to provide configuration
	 * shortcuts.
	 *
	 * @see SharedHttpSessionConfigurer
	 */
	<T extends B> T apply(MockMvcConfigurer configurer);

}
