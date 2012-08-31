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

package org.springframework.mock.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.servlet.FilterChain} interface.
 *
 * <p>Used for testing the web framework; also useful for testing
 * custom {@link javax.servlet.Filter} implementations.
 *
 * @author Juergen Hoeller
 * @author Rob Winch
 * @since 2.0.3
 * @see MockFilterConfig
 * @see PassThroughFilterChain
 */
public class MockFilterChain implements FilterChain {

	private ServletRequest request;

	private ServletResponse response;

	private final List<Filter> filters;

	private int index = 0;

	/**
	 * Registers a single do-nothing {@link Filter} implementation. This means multiple invocations of
	 * {@link #doFilter(ServletRequest, ServletResponse)} will result in an {@link IllegalStateException}.
	 */
	public MockFilterChain() {
		this(new FilterAdapter());
	}

	/**
	 * Create a {@link FilterChain} with one or more {@link Filter}s.
	 *
	 * @param filters the {@link Filter}'s to use in this  {@link FilterChain}
	 *
	 * @since 3.2
	 */
	public MockFilterChain(Filter... filters) {
		Assert.notEmpty(filters, "filters cannot be null or empty. Got "+filters);
		this.filters = createFilters(filters);
	}

	/**
	 * Creates a FilterChain with {@link Servlet}
	 *
	 * @param servlet the {@link Servlet} to use in this {@link FilterChain}
	 *
	 * @since 3.2
	 */
	public MockFilterChain(Servlet servlet) {
		this(new ServletFilterProxy(servlet));
	}

	/**
	 * Creates a FilterChain with one or more {@link Filter}s and a {@link Servlet}. The {@link Filter}s will be invoked
	 * first and then the {@link Servlet}.
	 *
	 * @param servlet the {@link Servlet} to use in this {@link FilterChain}
	 * @param filters the {@link Filter}'s to use in this {@link FilterChain}
	 *
	 * @since 3.2
	 */
	public MockFilterChain(Servlet servlet, Filter... filters) {
		this.filters = createFilters(filters);
		this.filters.add(new ServletFilterProxy(servlet));
	}

	/**
	 * Invokes the next registered {@link Filter} or the {@link Servlet}. Records the request and response.
	 *
	 * @throws ServletException
	 * @throws IOException
	 * @throws IllegalStateException if all of the registered {@link Filter} and {@link Servlet} implementations have
	 * been invoked.
	 */
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		if (index == filters.size()) {
			throw new IllegalStateException("Cannot invoke doFilter with index "
					+ index + " due to too many invocations of doFilter. Original chain is " + filters);
		}
		else {
			Filter nextFilter = filters.get(index);
			index++;

			nextFilter.doFilter(request, response, this);
		}

		this.request = request;
		this.response = response;
	}

	/**
	 * Return the most recent request that {@link #doFilter} has been called with.
	 */
	public ServletRequest getRequest() {
		return this.request;
	}

	/**
	 * Return the most recent response that {@link #doFilter} has been called with.
	 */
	public ServletResponse getResponse() {
		return this.response;
	}

	/**
	 * Validates the filters and creates a List<Filter> from them that implements {@link RandomAccess} and can be
	 * modified.
	 * @param filters
	 * @return
	 */
	private static List<Filter> createFilters(Filter...filters) {
		Assert.notNull(filters, "filters cannot be null");
		Assert.noNullElements(filters, "fiters cannot contain null values");
		List<Filter> result = new ArrayList<Filter>(filters.length + 1);
		for(Filter filter : filters) {
			result.add(filter);
		}
		return result;
	}

	/**
	 * A do nothing implementation of {@link Filter}. Subclasses can selectively implement only methods of interest.
	 *
	 * @author Rob Winch
	 *
	 */
	private static class FilterAdapter implements Filter {
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
				ServletException {
		}

		public void init(FilterConfig filterConfig) throws ServletException {
		}

		public void destroy() {
		}
	}

	/**
	 * Adapts a {@link Servlet#service(ServletRequest, ServletResponse)} to be invoked by
	 * {@link Filter#doFilter(ServletRequest, ServletResponse, FilterChain)}. This simplifies the implementation of
	 * {@link #doFilter(ServletRequest, ServletResponse, FilterChain)}.
	 *
	 * @author Rob Winch
	 *
	 */
	private static class ServletFilterProxy extends FilterAdapter {
		private final Servlet delegateServlet;

		private ServletFilterProxy(Servlet servlet) {
			Assert.notNull(servlet, "servlet cannot be null");
			this.delegateServlet = servlet;
		}

		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
				ServletException {
			delegateServlet.service(request, response);
		}

		@Override
		public String toString() {
			return delegateServlet.toString();
		}
	}
}
