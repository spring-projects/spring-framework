/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

/**
 * Wraps a {@link HandlerInterceptor} and uses URL patterns to determine whether
 * it applies to a given request.
 *
 * <p>Pattern matching can be done with a {@link PathMatcher} or with a parsed
 * {@link PathPattern}. The syntax is largely the same with the latter being more
 * tailored for web usage and more efficient. The choice is driven by the
 * presence of a {@linkplain UrlPathHelper#resolveAndCacheLookupPath resolved}
 * {@code String} lookupPath or a {@linkplain ServletRequestPathUtils#parseAndCache
 * parsed} {@code RequestPath} which in turn depends on the {@link HandlerMapping}
 * that matched the current request.
 *
 * <p>{@code MappedInterceptor} is supported by subclasses of
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
 * AbstractHandlerMethodMapping} which detect beans of type {@code MappedInterceptor}
 * and also check if interceptors directly registered with it are of this type.
 *
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.0
 */
public final class MappedInterceptor implements HandlerInterceptor {

	private static final PathMatcher defaultPathMatcher = new AntPathMatcher();


	@Nullable
	private final PatternAdapter[] includePatterns;

	@Nullable
	private final PatternAdapter[] excludePatterns;

	private PathMatcher pathMatcher = defaultPathMatcher;

	private final HandlerInterceptor interceptor;


	/**
	 * Create an instance with the given include and exclude patterns along with
	 * the target interceptor for the mappings.
	 * @param includePatterns patterns to which requests must match, or null to
	 * match all paths
	 * @param excludePatterns patterns to which requests must not match
	 * @param interceptor the target interceptor
	 * @param parser a parser to use to pre-parse patterns into {@link PathPattern};
	 * when not provided, {@link PathPatternParser#defaultInstance} is used.
	 * @since 5.3
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
			HandlerInterceptor interceptor, @Nullable PathPatternParser parser) {

		this.includePatterns = PatternAdapter.initPatterns(includePatterns, parser);
		this.excludePatterns = PatternAdapter.initPatterns(excludePatterns, parser);
		this.interceptor = interceptor;
	}


	/**
	 * Variant of
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * with include patterns only.
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, HandlerInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * Variant of
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * without a provided parser.
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
			HandlerInterceptor interceptor) {

		this(includePatterns, excludePatterns, interceptor, null);
	}

	/**
	 * Variant of
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * with a {@link WebRequestInterceptor} as the target.
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, WebRequestInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * Variant of
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * with a {@link WebRequestInterceptor} as the target.
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
			WebRequestInterceptor interceptor) {

		this(includePatterns, excludePatterns, new WebRequestHandlerInterceptorAdapter(interceptor));
	}


	/**
	 * Get the include path patterns this interceptor is mapped to.
	 * @see #getIncludePathPatterns()
	 * @see #getExcludePathPatterns()
	 * @deprecated since 6.1 in favor of {@link #getIncludePathPatterns()}
	 */
	@Nullable
	@Deprecated(since = "6.1", forRemoval = true)
	public String[] getPathPatterns() {
		return getIncludePathPatterns();
	}

	/**
	 * Get the include path patterns this interceptor is mapped to.
	 * @since 6.1
	 * @see #getExcludePathPatterns()
	 */
	@Nullable
	public String[] getIncludePathPatterns() {
		return (!ObjectUtils.isEmpty(this.includePatterns) ?
				Arrays.stream(this.includePatterns).map(PatternAdapter::getPatternString).toArray(String[]::new) :
				null);
	}

	/**
	 * Get the exclude path patterns this interceptor is mapped to.
	 * @since 6.1
	 * @see #getIncludePathPatterns()
	 */
	@Nullable
	public String[] getExcludePathPatterns() {
		return (!ObjectUtils.isEmpty(this.excludePatterns) ?
				Arrays.stream(this.excludePatterns).map(PatternAdapter::getPatternString).toArray(String[]::new) :
				null);
	}

	/**
	 * The target {@link HandlerInterceptor} to invoke in case of a match.
	 */
	public HandlerInterceptor getInterceptor() {
		return this.interceptor;
	}

	/**
	 * Configure the PathMatcher to use to match URL paths with against include
	 * and exclude patterns.
	 * <p>This is an advanced property that should be used only when a
	 * customized {@link AntPathMatcher} or a custom PathMatcher is required.
	 * <p>By default this is {@link AntPathMatcher}.
	 * <p><strong>Note:</strong> Setting {@code PathMatcher} enforces use of
	 * String pattern matching even when a
	 * {@linkplain ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}
	 * is available.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Get the {@linkplain #setPathMatcher(PathMatcher) configured} PathMatcher.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	/**
	 * Check whether this interceptor is mapped to the request.
	 * <p>The request mapping path is expected to have been resolved externally.
	 * See also class-level Javadoc.
	 * @param request the request to match to
	 * @return {@code true} if the interceptor should be applied to the request
	 */
	public boolean matches(HttpServletRequest request) {
		Object path = ServletRequestPathUtils.getCachedPath(request);
		if (this.pathMatcher != defaultPathMatcher) {
			path = path.toString();
		}
		boolean isPathContainer = (path instanceof PathContainer);
		if (!ObjectUtils.isEmpty(this.excludePatterns)) {
			for (PatternAdapter adapter : this.excludePatterns) {
				if (adapter.match(path, isPathContainer, this.pathMatcher)) {
					return false;
				}
			}
		}
		if (ObjectUtils.isEmpty(this.includePatterns)) {
			return true;
		}
		for (PatternAdapter adapter : this.includePatterns) {
			if (adapter.match(path, isPathContainer, this.pathMatcher)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if there is a match for the given lookup path.
	 * @param lookupPath the current request path
	 * @param pathMatcher a path matcher for path pattern matching
	 * @return {@code true} if the interceptor applies to the given request path
	 * @deprecated as of 5.3 in favor of {@link #matches(HttpServletRequest)}
	 */
	@Deprecated(since = "5.3")
	public boolean matches(String lookupPath, PathMatcher pathMatcher) {
		pathMatcher = (this.pathMatcher != defaultPathMatcher ? this.pathMatcher : pathMatcher);
		if (!ObjectUtils.isEmpty(this.excludePatterns)) {
			for (PatternAdapter adapter : this.excludePatterns) {
				if (pathMatcher.match(adapter.getPatternString(), lookupPath)) {
					return false;
				}
			}
		}
		if (ObjectUtils.isEmpty(this.includePatterns)) {
			return true;
		}
		for (PatternAdapter adapter : this.includePatterns) {
			if (pathMatcher.match(adapter.getPatternString(), lookupPath)) {
				return true;
			}
		}
		return false;
	}


	// HandlerInterceptor delegation

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return this.interceptor.preHandle(request, response, handler);
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {

		this.interceptor.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {

		this.interceptor.afterCompletion(request, response, handler, ex);
	}


	/**
	 * Contains both the parsed {@link PathPattern} and the raw String pattern,
	 * and uses the former when the cached path is {@link PathContainer} or the
	 * latter otherwise. If the pattern cannot be parsed due to unsupported
	 * syntax, then {@link PathMatcher} is used for all requests.
	 * @since 5.3.6
	 */
	private static class PatternAdapter {

		private final String patternString;

		@Nullable
		private final PathPattern pathPattern;


		public PatternAdapter(String pattern, @Nullable PathPatternParser parser) {
			this.patternString = pattern;
			this.pathPattern = initPathPattern(pattern, parser);
		}

		@Nullable
		private static PathPattern initPathPattern(String pattern, @Nullable PathPatternParser parser) {
			try {
				return (parser != null ? parser : PathPatternParser.defaultInstance).parse(pattern);
			}
			catch (PatternParseException ex) {
				return null;
			}
		}

		public String getPatternString() {
			return this.patternString;
		}

		public boolean match(Object path, boolean isPathContainer, PathMatcher pathMatcher) {
			if (isPathContainer) {
				PathContainer pathContainer = (PathContainer) path;
				if (this.pathPattern != null) {
					return this.pathPattern.matches(pathContainer);
				}
				String lookupPath = pathContainer.value();
				path = UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
			}
			return pathMatcher.match(this.patternString, (String) path);
		}

		@Nullable
		public static PatternAdapter[] initPatterns(
				@Nullable String[] patterns, @Nullable PathPatternParser parser) {

			if (ObjectUtils.isEmpty(patterns)) {
				return null;
			}
			return Arrays.stream(patterns)
					.map(pattern -> new PatternAdapter(pattern, parser))
					.toArray(PatternAdapter[]::new);
		}
	}

}
