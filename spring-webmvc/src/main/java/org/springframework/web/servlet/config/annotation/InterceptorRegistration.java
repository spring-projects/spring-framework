/*
 * Copyright 2002-present the original author or authors.
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
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.MethodInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Assists with the creation of a {@link MappedInterceptor}.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class InterceptorRegistration {

	private final HandlerInterceptor interceptor;

	private @Nullable List<String> includePatterns;

	private @Nullable List<String> excludePatterns;

	private @Nullable PathMatcher pathMatcher;

	private @Nullable Set<String> allowedMethods;

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
	 * <p>
	 * For pattern syntax see {@link PathPattern} when parsed patterns
	 * are {@link PathMatchConfigurer#setPatternParser enabled} or
	 * {@link AntPathMatcher} otherwise. The syntax is largely the same with
	 * {@link PathPattern} more tailored for web usage and more efficient.
	 */
	public InterceptorRegistration addPathPatterns(String... patterns) {
		return addPathPatterns(Arrays.asList(patterns));
	}

	/**
	 * List-based variant of {@link #addPathPatterns(String...)}.
	 * 
	 * @since 5.0.3
	 */
	public InterceptorRegistration addPathPatterns(List<String> patterns) {
		this.includePatterns = (this.includePatterns != null ? this.includePatterns : new ArrayList<>(patterns.size()));
		this.includePatterns.addAll(patterns);
		return this;
	}

	/**
	 * Add patterns for URLs the interceptor should be excluded from.
	 * <p>
	 * For pattern syntax see {@link PathPattern} when parsed patterns
	 * are {@link PathMatchConfigurer#setPatternParser enabled} or
	 * {@link AntPathMatcher} otherwise. The syntax is largely the same with
	 * {@link PathPattern} more tailored for web usage and more efficient.
	 */
	public InterceptorRegistration excludePathPatterns(String... patterns) {
		return excludePathPatterns(Arrays.asList(patterns));
	}

	/**
	 * List-based variant of {@link #excludePathPatterns(String...)}.
	 * 
	 * @since 5.0.3
	 */
	public InterceptorRegistration excludePathPatterns(List<String> patterns) {
		this.excludePatterns = (this.excludePatterns != null ? this.excludePatterns : new ArrayList<>(patterns.size()));
		this.excludePatterns.addAll(patterns);
		return this;
	}

	/**
	 * Configure the PathMatcher to use to match URL paths with against include
	 * and exclude patterns.
	 * <p>
	 * This is an advanced property that should be used only when a
	 * customized {@link AntPathMatcher} or a custom PathMatcher is required.
	 * <p>
	 * By default this is {@link AntPathMatcher}.
	 * <p>
	 * <strong>Note:</strong> Setting {@code PathMatcher} enforces use of
	 * String pattern matching even when a
	 * {@link ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}
	 * is available.
	 * 
	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is
	 *             deprecated
	 *             for use at runtime in web modules in favor of parsed patterns
	 *             with
	 *             {@link PathPatternParser}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public InterceptorRegistration pathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}

	/**
	 * Specify an order position to be used. Default is 0.
	 * 
	 * @since 4.3.23
	 */
	public InterceptorRegistration order(int order) {
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
	 * Build and return the configured interceptor instance.
	 *
	 * <p>
	 * If no include or exclude path patterns are configured, this method returns
	 * the
	 * original {@link HandlerInterceptor} (or a decorated one if method filtering
	 * is applied).
	 * If path patterns are provided, a {@link MappedInterceptor} is returned with
	 * the patterns and optional {@link PathMatcher} applied.
	 *
	 * <p>
	 * If {@link #allowedMethods(HttpMethod...)} is used, the interceptor is wrapped
	 * with a {@link MethodInterceptor} that restricts its invocation based on HTTP
	 * method.
	 *
	 * @return a {@link HandlerInterceptor} or {@link MappedInterceptor} instance
	 *         depending
	 *         on path and method configuration
	 */
	@SuppressWarnings("removal")
	protected Object getInterceptor() {

		HandlerInterceptor methodInterceptor = this.interceptor;

		if (allowedMethods != null) {
			methodInterceptor = new MethodInterceptor(methodInterceptor, allowedMethods);
		}

		if (this.includePatterns == null && this.excludePatterns == null) {
			return methodInterceptor;
		}

		MappedInterceptor mappedInterceptor = new MappedInterceptor(
				StringUtils.toStringArray(this.includePatterns),
				StringUtils.toStringArray(this.excludePatterns),
				methodInterceptor);

		if (this.pathMatcher != null) {
			mappedInterceptor.setPathMatcher(this.pathMatcher);
		}

		return mappedInterceptor;
	}

	/**
	 * Specify the HTTP methods that the interceptor should be invoked for.
	 *
	 * <p>
	 * When this method is called, the underlying {@link HandlerInterceptor} will be
	 * wrapped in a {@link MethodInterceptor}, which conditionally delegates based
	 * on
	 * the HTTP method of the request.
	 *
	 * <p>
	 * This allows interceptors to be configured declaratively, e.g., to run only
	 * for {@code POST}, {@code PUT}, etc., without requiring manual method checks
	 * in the interceptor implementation.
	 *
	 * <p>
	 * Must specify at least one method; an {@link IllegalArgumentException} will be
	 * thrown otherwise.
	 *
	 * <pre class="code">
	 * registry.addInterceptor(new AuditInterceptor())
	 * 		.addPathPatterns("/api/**")
	 * 		.allowedMethods(HttpMethod.POST, HttpMethod.PUT);
	 * </pre>
	 *
	 * @param methods one or more HTTP methods to allow
	 * @return this registration for chained calls
	 * @throws IllegalArgumentException if no methods are specified
	 */
	public InterceptorRegistration allowedMethods(HttpMethod... methods) {
		Assert.isTrue(methods.length > 0, "At least one HTTP method must be specified");
		this.allowedMethods = Arrays.stream(methods)
				.map(HttpMethod::name)
				.collect(Collectors.toSet());
		return this;
	}
}
