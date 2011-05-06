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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptors;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

/**
 * Helps with configuring an ordered set of Spring MVC interceptors of type {@link HandlerInterceptor} or
 * {@link WebRequestInterceptor}. Registered interceptors will generally be detected by all {@link HandlerMapping}
 * instances in a Spring MVC web application context. Interceptors can be added with a set of path patterns to
 * which they should apply.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class InterceptorConfigurer {

	private final List<MappedInterceptor> mappedInterceptors = new ArrayList<MappedInterceptor>();

	/**
	 * Add a {@link HandlerInterceptor} that should apply to any request.
	 */
	public void addInterceptor(HandlerInterceptor interceptor) {
		register(null, interceptor);
	}

	/**
	 * Add a {@link WebRequestInterceptor} that should apply to any request.
	 */
	public void addInterceptor(WebRequestInterceptor interceptor) {
		register(null, asHandlerInterceptorArray(interceptor));
	}

	/**
	 * Add {@link HandlerInterceptor}s that should apply to any request.
	 */
	public void addInterceptors(HandlerInterceptor... interceptors) {
		register(null, interceptors);
	}

	/**
	 * Add {@link WebRequestInterceptor}s that should apply to any request.
	 */
	public void addInterceptors(WebRequestInterceptor... interceptors) {
		register(null, asHandlerInterceptorArray(interceptors));
	}

	/**
	 * Add a {@link HandlerInterceptor} with a set of URL path patterns it should apply to.
	 */
	public void mapInterceptor(String[] pathPatterns, HandlerInterceptor interceptor) {
		register(pathPatterns, interceptor);
	}

	/**
	 * Add a {@link WebRequestInterceptor} with a set of URL path patterns it should apply to.
	 */
	public void mapInterceptor(String[] pathPatterns, WebRequestInterceptor interceptors) {
		register(pathPatterns, asHandlerInterceptorArray(interceptors));
	}

	/**
	 * Add {@link HandlerInterceptor}s with a set of URL path patterns they should apply to.
	 */
	public void mapInterceptors(String[] pathPatterns, HandlerInterceptor... interceptors) {
		register(pathPatterns, interceptors);
	}

	/**
	 * Add {@link WebRequestInterceptor}s with a set of URL path patterns they should apply to.
	 */
	public void mapInterceptors(String[] pathPatterns, WebRequestInterceptor... interceptors) {
		register(pathPatterns, asHandlerInterceptorArray(interceptors));
	}

	private static HandlerInterceptor[] asHandlerInterceptorArray(WebRequestInterceptor...interceptors) {
		HandlerInterceptor[] result = new HandlerInterceptor[interceptors.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new WebRequestHandlerInterceptorAdapter(interceptors[i]);
		}
		return result;
	}

	/**
	 * Stores the given set of {@link HandlerInterceptor}s and path patterns internally.
	 * @param pathPatterns path patterns or {@code null}
	 * @param interceptors one or more interceptors to be stored
	 */
	protected void register(String[] pathPatterns, HandlerInterceptor...interceptors) {
		Assert.notEmpty(interceptors, "At least one interceptor must be provided");
		for (HandlerInterceptor interceptor : interceptors) {
			mappedInterceptors.add(new MappedInterceptor(pathPatterns, interceptor));
		}
	}

	/**
	 * Returns a {@link MappedInterceptors} instance with all registered interceptors.
	 */
	protected MappedInterceptors getMappedInterceptors() {
		return new MappedInterceptors(mappedInterceptors.toArray(new MappedInterceptor[mappedInterceptors.size()]));
	}

}
