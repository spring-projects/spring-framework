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

package org.springframework.mock.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Mock implementation of the {@link javax.servlet.FilterChain} interface.
 *
 * <p>A {@link MockFilterChain} can be configured with one or more filters and a
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

	@Nullable
	private ServletRequest request;

	@Nullable
	private ServletResponse response;

	private final List<Filter> filters;

	@Nullable
	private Iterator<Filter> iterator;


	/**
	 * Register a single do-nothing {@link Filter} implementation. The first
	 * invocation saves the request and response. Subsequent invocations raise
	 * an {@link IllegalStateException} unless {@link #reset()} is called.
	 */
	public MockFilterChain() {
		this.filters = Collections.emptyList();
	}

	/**
	 * Create a FilterChain with a Servlet.
	 * @param servlet the Servlet to invoke
	 * @since 3.2
	 */
	public MockFilterChain(Servlet servlet) {
		this.filters = initFilterList(servlet);
	}

	/**
	 * Create a {@code FilterChain} with Filter's and a Servlet.
	 * @param servlet the {@link Servlet} to invoke in this {@link FilterChain}
	 * @param filters the {@link Filter}'s to invoke in this {@link FilterChain}
	 * @since 3.2
	 */
	public MockFilterChain(Servlet servlet, Filter... filters) {
		Assert.notNull(filters, "filters cannot be null");
		Assert.noNullElements(filters, "filters cannot contain null values");
		this.filters = initFilterList(servlet, filters);
	}

	private static List<Filter> initFilterList(Servlet servlet, Filter... filters) {
		Filter[] allFilters = ObjectUtils.addObjectToArray(filters, new ServletFilterProxy(servlet));
		return Arrays.asList(allFilters);
	}


	/**
	 * Return the request that {@link #doFilter} has been called with.
	 */
	@Nullable
	public ServletRequest getRequest() {
		return this.request;
	}

	/**
	 * Return the response that {@link #doFilter} has been called with.
	 */
	@Nullable
	public ServletResponse getResponse() {
		return this.response;
	}

	/**
	 * Invoke registered {@link Filter}s and/or {@link Servlet} also saving the
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
	 * Reset the {@link MockFilterChain} allowing it to be invoked again.
	 */
	public void reset() {
		this.request = null;
		this.response = null;
		this.iterator = null;
	}


	/**
	 * A filter that simply delegates to a Servlet.
	 */
	private static class ServletFilterProxy implements Filter {

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
