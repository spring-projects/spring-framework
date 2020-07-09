/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Assists with the creation of a {@link MappedInterceptor}.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class InterceptorRegistration {

	private final HandlerInterceptor interceptor;

	@Nullable
	private List<String> includePatterns;

	@Nullable
	private List<String> excludePatterns;

	@Nullable
	private PathMatcher pathMatcher;

	private int order = 0;


	/**
	 * Create an {@link InterceptorRegistration} instance.
	 */
	public InterceptorRegistration(HandlerInterceptor interceptor) {
		Assert.notNull(interceptor, "Interceptor is required");
		this.interceptor = interceptor;
	}


	/**
	 * Add patterns for URLs the interceptor should be included in.
	 * <p>For pattern syntax see {@link PathPattern} when parsed patterns
	 * are {@link PathMatchConfigurer#setPatternParser enabled} or
	 * {@link AntPathMatcher} otherwise. The syntax is largely the same with
	 * {@link PathPattern} more tailored for web usage and more efficient.
	 */
	public InterceptorRegistration addPathPatterns(String... patterns) {
		return addPathPatterns(Arrays.asList(patterns));
	}

	/**
	 * List-based variant of {@link #addPathPatterns(String...)}.
	 * @since 5.0.3
	 */
	public InterceptorRegistration addPathPatterns(List<String> patterns) {
		this.includePatterns = (this.includePatterns != null ?
				this.includePatterns : new ArrayList<>(patterns.size()));
		this.includePatterns.addAll(patterns);
		return this;
	}

	/**
	 * Add patterns for URLs the interceptor should be excluded from.
	 * <p>For pattern syntax see {@link PathPattern} when parsed patterns
	 * are {@link PathMatchConfigurer#setPatternParser enabled} or
	 * {@link AntPathMatcher} otherwise. The syntax is largely the same with
	 * {@link PathPattern} more tailored for web usage and more efficient.
	 */
	public InterceptorRegistration excludePathPatterns(String... patterns) {
		return excludePathPatterns(Arrays.asList(patterns));
	}

	/**
	 * List-based variant of {@link #excludePathPatterns(String...)}.
	 * @since 5.0.3
	 */
	public InterceptorRegistration excludePathPatterns(List<String> patterns) {
		this.excludePatterns = (this.excludePatterns != null ?
				this.excludePatterns : new ArrayList<>(patterns.size()));
		this.excludePatterns.addAll(patterns);
		return this;
	}

	/**
	 * Configure the PathMatcher to use to match URL paths with against include
	 * and exclude patterns.
	 * <p>This is an advanced property that should be used only when a
	 * customized {@link AntPathMatcher} or a custom PathMatcher is required.
	 * <p>By default this is {@link AntPathMatcher}.
	 * <p><strong>Note:</strong> Setting {@code PathMatcher} enforces use of
	 * String pattern matching even when a
	 * {@link ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}
	 * is available.
	 */
	public InterceptorRegistration pathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}

	/**
	 * Specify an order position to be used. Default is 0.
	 * @since 4.3.23
	 */
	public InterceptorRegistration order(int order){
		this.order = order;
		return this;
	}

	/**
	 * Return the order position to be used.
	 */
	protected int getOrder() {
		return this.order;
	}

	/**
	 * Build the underlying interceptor. If URL patterns are provided, the returned
	 * type is {@link MappedInterceptor}; otherwise {@link HandlerInterceptor}.
	 */
	protected Object getInterceptor() {

		if (this.includePatterns == null && this.excludePatterns == null) {
			return this.interceptor;
		}

		MappedInterceptor mappedInterceptor = new MappedInterceptor(
				StringUtils.toStringArray(this.includePatterns),
				StringUtils.toStringArray(this.excludePatterns),
				this.interceptor);

		if (this.pathMatcher != null) {
			mappedInterceptor.setPathMatcher(this.pathMatcher);
		}

		return mappedInterceptor;
	}

}
