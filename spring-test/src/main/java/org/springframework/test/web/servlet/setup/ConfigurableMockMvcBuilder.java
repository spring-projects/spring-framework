/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Defines common methods for building a {@code MockMvc}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 * @param <B> a self reference to the builder type
 */
public interface ConfigurableMockMvcBuilder<B extends ConfigurableMockMvcBuilder<B>> extends MockMvcBuilder {

	/**
	 * Add filters mapped to all requests. Filters are invoked in the same order.
	 * <p>Note: if you need the filter to be initialized with {@link Filter#init(FilterConfig)},
	 * please use {@link #addFilter(Filter, String, Map, EnumSet, String...)} instead.
	 * @param filters the filters to add
	 */
	<T extends B> T addFilters(Filter... filters);

	/**
	 * Add a filter mapped to specific patterns.
	 * <p>Note: if you need the filter to be initialized with {@link Filter#init(FilterConfig)},
	 * please use {@link #addFilter(Filter, String, Map, EnumSet, String...)} instead.
	 * @param filter the filter to add
	 * @param urlPatterns the URL patterns to map to; if empty, matches all requests
	 */
	<T extends B> T addFilter(Filter filter, String... urlPatterns);

	/**
	 * Add a filter that will be initialized via {@link Filter#init(FilterConfig)}
	 * with the given init parameters, and will also apply only to requests that
	 * match the given dispatcher types and URL patterns.
	 * @param filter the filter to add
	 * @param filterName the name to use for the filter; if {@code null}, then
	 * {@link org.springframework.mock.web.MockFilterConfig} is created without
	 * a name, which defaults to an empty String for the name
	 * @param initParams the init parameters to initialize the filter with
	 * @param dispatcherTypes dispatcher types the filter applies to
	 * @param urlPatterns the URL patterns to map to; if empty, matches all requests
	 * @since 6.1
	 * @see org.springframework.mock.web.MockFilterConfig
	 */
	<T extends B> T addFilter(
			Filter filter, @Nullable String filterName, Map<String, String> initParams,
			EnumSet<DispatcherType> dispatcherTypes, String... urlPatterns);

	/**
	 * Define default request properties that should be merged into all
	 * performed requests. In effect this provides a mechanism for defining
	 * common initialization for all requests such as the content type, request
	 * parameters, session attributes, and any other request property.
	 *
	 * <p>Properties specified at the time of performing a request override the
	 * default properties defined here.
	 * @param requestBuilder a RequestBuilder; see static factory methods in
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}
	 */
	<T extends B> T defaultRequest(RequestBuilder requestBuilder);

	/**
	 * Define the default character encoding to be applied to every response.
	 * <p>The default implementation of this method throws an
	 * {@link UnsupportedOperationException}. Concrete implementations are therefore
	 * encouraged to override this method.
	 * @param defaultResponseCharacterEncoding the default response character encoding
	 * @since 5.3.10
	 */
	default <T extends B> T defaultResponseCharacterEncoding(Charset defaultResponseCharacterEncoding) {
		throw new UnsupportedOperationException("defaultResponseCharacterEncoding is not supported by this MockMvcBuilder");
	}

	/**
	 * Define a global expectation that should <em>always</em> be applied to
	 * every response. For example, status code 200 (OK), content type
	 * {@code "application/json"}, etc.
	 * @param resultMatcher a ResultMatcher; see static factory methods in
	 * {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers}
	 */
	<T extends B> T alwaysExpect(ResultMatcher resultMatcher);

	/**
	 * Define a global action that should <em>always</em> be applied to every
	 * response. For example, writing detailed information about the performed
	 * request and resulting response to {@code System.out}.
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
	 * A more advanced variant of {@link #dispatchOptions(boolean)} that allows
	 * customizing any {@link org.springframework.web.servlet.DispatcherServlet}
	 * property.
	 * @since 5.3
	 */
	<T extends B> T addDispatcherServletCustomizer(DispatcherServletCustomizer customizer);

	/**
	 * Add a {@code MockMvcConfigurer} that automates MockMvc setup and
	 * configures it for some specific purpose (e.g. security).
	 * <p>There is a built-in {@link SharedHttpSessionConfigurer} that can be
	 * used to re-use the HTTP session across requests. 3rd party frameworks
	 * like Spring Security also use this mechanism to provide configuration
	 * shortcuts.
	 * @see SharedHttpSessionConfigurer
	 */
	<T extends B> T apply(MockMvcConfigurer configurer);

}
