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

package org.springframework.web.servlet.handler;

import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Holds information about a HandlerInterceptor mapped to a path into the application.
 * Provides a method to match a request path to the mapped path patterns.
 *
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public final class MappedInterceptor {

	private final String[] includePatterns;

	private final String[] excludePatterns;

	private final HandlerInterceptor interceptor;

	private PathMatcher pathMatcher;


	/**
	 * Create a new MappedInterceptor instance.
	 * @param includePatterns the path patterns to map with a {@code null} value matching to all paths
	 * @param interceptor the HandlerInterceptor instance to map to the given patterns
	 */
	public MappedInterceptor(String[] includePatterns, HandlerInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * Create a new MappedInterceptor instance.
	 * @param includePatterns the path patterns to map with a {@code null} value matching to all paths
	 * @param excludePatterns the path patterns to exclude
	 * @param interceptor the HandlerInterceptor instance to map to the given patterns
	 */
	public MappedInterceptor(String[] includePatterns, String[] excludePatterns, HandlerInterceptor interceptor) {
		this.includePatterns = includePatterns;
		this.excludePatterns = excludePatterns;
		this.interceptor = interceptor;
	}


	/**
	 * Create a new MappedInterceptor instance.
	 * @param includePatterns the path patterns to map with a {@code null} value matching to all paths
	 * @param interceptor the WebRequestInterceptor instance to map to the given patterns
	 */
	public MappedInterceptor(String[] includePatterns, WebRequestInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * Create a new MappedInterceptor instance.
	 * @param includePatterns the path patterns to map with a {@code null} value matching to all paths
	 * @param interceptor the WebRequestInterceptor instance to map to the given patterns
	 */
	public MappedInterceptor(String[] includePatterns, String[] excludePatterns, WebRequestInterceptor interceptor) {
		this(includePatterns, excludePatterns, new WebRequestHandlerInterceptorAdapter(interceptor));
	}


	/**
	 * Configure a PathMatcher to use with this MappedInterceptor instead of the
	 * one passed by default to the {@link #matches(String, org.springframework.util.PathMatcher)}
	 * method. This is an advanced property that is only required when using custom
	 * PathMatcher implementations that support mapping metadata other than the
	 * Ant-style path patterns supported by default.
	 *
	 * @param pathMatcher the path matcher to use
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * The configured PathMatcher, or {@code null}.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * The path into the application the interceptor is mapped to.
	 */
	public String[] getPathPatterns() {
		return this.includePatterns;
	}

	/**
	 * The actual Interceptor reference.
	 */
	public HandlerInterceptor getInterceptor() {
		return this.interceptor;
	}

	/**
	 * Returns {@code true} if the interceptor applies to the given request path.
	 * @param lookupPath the current request path
	 * @param pathMatcher a path matcher for path pattern matching
	 */
	public boolean matches(String lookupPath, PathMatcher pathMatcher) {
		PathMatcher pathMatcherToUse = (this.pathMatcher != null) ? this.pathMatcher : pathMatcher;
		if (this.excludePatterns != null) {
			for (String pattern : this.excludePatterns) {
				if (pathMatcherToUse.match(pattern, lookupPath)) {
					return false;
				}
			}
		}
		if (this.includePatterns == null) {
			return true;
		}
		else {
			for (String pattern : this.includePatterns) {
				if (pathMatcherToUse.match(pattern, lookupPath)) {
					return true;
				}
			}
			return false;
		}
	}
}
