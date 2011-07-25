/*
 * Copyright 2002-2011 the original author or authors.
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

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.servlet.FilterConfig} interface.
 *
 * <p>Used for testing the web framework; also useful for testing
 * custom {@link javax.servlet.Filter} implementations.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see MockFilterConfig
 * @see PassThroughFilterChain
 */
public class MockFilterChain implements FilterChain {

	private ServletRequest request;

	private ServletResponse response;


	/**
	 * Records the request and response.
	 */
	public void doFilter(ServletRequest request, ServletResponse response) {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		if (this.request != null) {
			throw new IllegalStateException("This FilterChain has already been called!");
		}
		this.request = request;
		this.response = response;
	}

	/**
	 * Return the request that {@link #doFilter} has been called with.
	 */
	public ServletRequest getRequest() {
		return this.request;
	}

	/**
	 * Return the response that {@link #doFilter} has been called with.
	 */
	public ServletResponse getResponse() {
		return this.response;
	}

}
