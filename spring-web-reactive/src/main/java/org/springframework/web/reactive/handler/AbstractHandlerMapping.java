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
package org.springframework.web.reactive.handler;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.util.HttpRequestPathHelper;

/**
 * Abstract base class for {@link org.springframework.web.reactive.HandlerMapping}
 * implementations.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractHandlerMapping extends ApplicationObjectSupport
		implements HandlerMapping, Ordered {

	private int order = Integer.MAX_VALUE;  // default: same as non-Ordered

	private HttpRequestPathHelper pathHelper = new HttpRequestPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();


	// TODO: CORS

	/**
	 * Specify the order value for this HandlerMapping bean.
	 * <p>Default value is {@code Integer.MAX_VALUE}, meaning that it's non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public final void setOrder(int order) {
		this.order = order;
	}

	@Override
	public final int getOrder() {
		return this.order;
	}

	/**
	 * Set if the path should be URL-decoded. This sets the same property on the
	 * underlying path helper.
	 * @see HttpRequestPathHelper#setUrlDecode(boolean)
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.pathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Set the {@link HttpRequestPathHelper} to use for resolution of lookup
	 * paths. Use this to override the default implementation with a custom
	 * subclass or to share common path helper settings across multiple
	 * HandlerMappings.
	 */
	public void setPathHelper(HttpRequestPathHelper pathHelper) {
		this.pathHelper = pathHelper;
	}

	/**
	 * Return the {@link HttpRequestPathHelper} implementation to use for
	 * resolution of lookup paths.
	 */
	public HttpRequestPathHelper getPathHelper() {
		return this.pathHelper;
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns. Default is AntPathMatcher.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		// this.corsConfigSource.setPathMatcher(pathMatcher);
	}

	/**
	 * Return the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

}
