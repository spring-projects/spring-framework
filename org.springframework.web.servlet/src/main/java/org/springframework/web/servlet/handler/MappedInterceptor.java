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

import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Holds information about a HandlerInterceptor mapped to a path into the application.
 * @author Keith Donald
 * @since 3.0
 */
public final class MappedInterceptor {
	
	private final String pathPattern;
	
	private final HandlerInterceptor interceptor;

	/**
	 * Creates a new mapped interceptor.
	 * @param pathPattern the path pattern
	 * @param interceptor the interceptor
	 */
	public MappedInterceptor(String pathPattern, HandlerInterceptor interceptor) {
		this.pathPattern = pathPattern;
		this.interceptor = interceptor;
	}

	/**
	 * Creates a new mapped interceptor.
	 * @param pathPattern the path pattern
	 * @param interceptor the interceptor
	 */
	public MappedInterceptor(String pathPattern, WebRequestInterceptor interceptor) {
		this.pathPattern = pathPattern;
		this.interceptor = new WebRequestHandlerInterceptorAdapter(interceptor);
	}

	/**
	 * The path into the application the interceptor is mapped to.
	 */
	public String getPathPattern() {
		return pathPattern;
	}

	/**
	 * The actual Interceptor reference.
	 */
	public HandlerInterceptor getInterceptor() {
		return interceptor;
	}
		
}