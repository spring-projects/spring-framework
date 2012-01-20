/*
 * Copyright 2002-2009 the original author or authors.
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
	
	private final String[] pathPatterns;
	
	private final HandlerInterceptor interceptor;


	/**
	 * Create a new MappedInterceptor instance.
	 * @param pathPatterns the path patterns to map with a {@code null} value matching to all paths
	 * @param interceptor the HandlerInterceptor instance to map to the given patterns
	 */
	public MappedInterceptor(String[] pathPatterns, HandlerInterceptor interceptor) {
		this.pathPatterns = pathPatterns;
		this.interceptor = interceptor;
	}

	/**
	 * Create a new MappedInterceptor instance.
	 * @param pathPatterns the path patterns to map with a {@code null} value matching to all paths
	 * @param interceptor the WebRequestInterceptor instance to map to the given patterns
	 */
	public MappedInterceptor(String[] pathPatterns, WebRequestInterceptor interceptor) {
		this.pathPatterns = pathPatterns;
		this.interceptor = new WebRequestHandlerInterceptorAdapter(interceptor);
	}


	/**
	 * The path into the application the interceptor is mapped to.
	 */
	public String[] getPathPatterns() {
		return this.pathPatterns;
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
		if (pathPatterns == null) {
			return true;
		}
		else {
			for (String pathPattern : pathPatterns) {
				if (pathMatcher.match(pathPattern, lookupPath)) {
					return true;
				}
			}
			return false;
		}
	}
}
