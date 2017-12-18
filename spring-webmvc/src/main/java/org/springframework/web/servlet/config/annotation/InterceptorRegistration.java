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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * Assists with the creation of a {@link MappedInterceptor}.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class InterceptorRegistration {

	private final HandlerInterceptor interceptor;

	private final List<String> includePatterns = new ArrayList<>();

	private final List<String> excludePatterns = new ArrayList<>();

	@Nullable
	private PathMatcher pathMatcher;

	private int order = 0;


	/**
	 * Creates an {@link InterceptorRegistration} instance.
	 */
	public InterceptorRegistration(HandlerInterceptor interceptor) {
		Assert.notNull(interceptor, "Interceptor is required");
		this.interceptor = interceptor;
	}

	/**
	 * Add URL patterns to which the registered interceptor should apply to.
	 */
	public InterceptorRegistration addPathPatterns(String... patterns) {
		this.includePatterns.addAll(Arrays.asList(patterns));
		return this;
	}

	/**
	 * Add URL patterns to which the registered interceptor should not apply to.
	 */
	public InterceptorRegistration excludePathPatterns(String... patterns) {
		this.excludePatterns.addAll(Arrays.asList(patterns));
		return this;
	}

	/**
	 * A PathMatcher implementation to use with this interceptor. This is an optional,
	 * advanced property required only if using custom PathMatcher implementations
	 * that support mapping metadata other than the Ant path patterns supported
	 * by default.
	 */
	public InterceptorRegistration pathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}

	/**
	 * Specify an order position to be used. Default is 0.
	 * @since 5.0
	 */
	public InterceptorRegistration order(int order){
		this.order = order;
		return this;
	}

	/**
	 * Return the order position to be used.
	 * @since 5.0
	 */
	protected int getOrder() {
		return this.order;
	}

	/**
	 * Returns the underlying interceptor. If URL patterns are provided the returned type is
	 * {@link MappedInterceptor}; otherwise {@link HandlerInterceptor}.
	 */
	protected Object getInterceptor() {
		if (this.includePatterns.isEmpty() && this.excludePatterns.isEmpty()) {
			return this.interceptor;
		}

		String[] include = StringUtils.toStringArray(this.includePatterns);
		String[] exclude = StringUtils.toStringArray(this.excludePatterns);
		MappedInterceptor mappedInterceptor = new MappedInterceptor(include, exclude, this.interceptor);

		if (this.pathMatcher != null) {
			mappedInterceptor.setPathMatcher(this.pathMatcher);
		}

		return mappedInterceptor;
	}

}
