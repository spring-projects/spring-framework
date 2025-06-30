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

package org.springframework.web.testfixture.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Mock implementation of the {@link jakarta.servlet.FilterChain} interface.
 *
 * <p>A {@code MockFilterChain} can be configured with one or more filters and a
 * Servlet to invoke. The first time the chain is called, it invokes all filters
 * and the Servlet, and saves the request and response. Subsequent invocations
 * raise an {@link IllegalStateException} unless {@link #reset()} is called.
 *
 * @author Juergen Hoeller
 * @author Rob Winch
 * @author Rossen Stoyanchev
 * @since 2.0.3
 * @see MockFilterConfig
 * @see PassThroughFilterChain
 */
public class MockFilterChain implements FilterChain {

	private @Nullable ServletRequest request;

	private @Nullable ServletResponse response;

	private final List<Filter> filters;

	private @Nullable Iterator<Filter> iterator;


	/**
	 * Create an empty {@code MockFilterChain} without any {@linkplain Filter filters}.
	 */
	public MockFilterChain() {
		this.filters = Collections.emptyList();
	}

	/**
	 * Create a {@code MockFilterChain} with a {@link Servlet}.
	 * @param servlet the {@code Servlet} to invoke
	 * @since 3.2
	 */
	public MockFilterChain(Servlet servlet) {
		this.filters = initFilterList(servlet);
	}

	/**
	 * Create a {@code MockFilterChain} with a {@link Servlet} and {@linkplain Filter
	 * filters}.
	 * @param servlet the {@code Servlet} to invoke in this {@code MockFilterChain}
	 * @param filters the filters to invoke in this {@code MockFilterChain}
	 * @since 3.2
	 */
	public MockFilterChain(Servlet servlet, Filter... filters) {
		Assert.notNull(filters, "filters cannot be null");
		Assert.noNullElements(filters, "filters cannot contain null values");
		this.filters = initFilterList(servlet, filters);
	}

	private static List<Filter> initFilterList(Servlet servlet, Filter... filters) {
		Filter[] allFilters = ObjectUtils.addObjectToArray(filters, new ServletFilterProxy(servlet));
		return List.of(allFilters);
	}


	/**
	 * Return the request that {@link #doFilter} has been called with.
	 */
	public @Nullable ServletRequest getRequest() {
		return this.request;
	}

	/**
	 * Return the response that {@link #doFilter} has been called with.
	 */
	public @Nullable ServletResponse getResponse() {
		return this.response;
	}

	/**
	 * Invoke registered {@link Filter Filters} and/or {@link Servlet} also saving the
	 * request and response.
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		Assert.state(this.request == null, "This FilterChain has already been called!");

		if (this.iterator == null) {
			this.iterator = this.filters.iterator();
		}

		if (this.iterator.hasNext()) {
			Filter nextFilter = this.iterator.next();
			nextFilter.doFilter(request, response, this);
		}

		this.request = request;
		this.response = response;
	}

	/**
	 * Reset this {@code MockFilterChain} allowing it to be invoked again.
	 */
	public void reset() {
		this.request = null;
		this.response = null;
		this.iterator = null;
	}


	/**
	 * A filter that simply delegates to a Servlet.
	 */
	private static final class ServletFilterProxy implements Filter {

		private final Servlet delegateServlet;

		private ServletFilterProxy(Servlet servlet) {
			Assert.notNull(servlet, "servlet cannot be null");
			this.delegateServlet = servlet;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {

			this.delegateServlet.service(request, response);
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void destroy() {
		}

		@Override
		public String toString() {
			return this.delegateServlet.toString();
		}
	}

}
