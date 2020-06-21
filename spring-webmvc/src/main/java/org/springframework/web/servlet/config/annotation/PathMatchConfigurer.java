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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Configure path matching options. The options are applied to the following:
 * <ul>
 * <li>{@link WebMvcConfigurationSupport#requestMappingHandlerMapping}</li>
 * <li>{@link WebMvcConfigurationSupport#viewControllerHandlerMapping}</li>
 * <li>{@link WebMvcConfigurationSupport#resourceHandlerMapping}</li>
 * </ul>
 *
 * @author Brian Clozel
 * @since 4.0.3
 */
public class PathMatchConfigurer {

	@Nullable
	private PathPatternParser patternParser;

	@Nullable
	private Boolean trailingSlashMatch;

	@Nullable
	private Map<String, Predicate<Class<?>>> pathPrefixes;

	@Nullable
	private Boolean suffixPatternMatch;

	@Nullable
	private Boolean registeredSuffixPatternMatch;

	@Nullable
	private UrlPathHelper urlPathHelper;

	@Nullable
	private PathMatcher pathMatcher;

	@Nullable
	private UrlPathHelper defaultUrlPathHelper;

	@Nullable
	private PathMatcher defaultPathMatcher;


	/**
	 * Enable use of parsed {@link PathPattern}s as described in
	 * {@link AbstractHandlerMapping#setPatternParser(PathPatternParser)}.
	 * <p><strong>Note:</strong> This is mutually exclusive with use of
	 * {@link #setUrlPathHelper(UrlPathHelper)} and
	 * {@link #setPathMatcher(PathMatcher)}.
	 * <p>By default this is not enabled.
	 * @param patternParser the parser to pre-parse patterns with
	 * @since 5.3
	 */
	public PathMatchConfigurer setPatternParser(PathPatternParser patternParser) {
		this.patternParser = patternParser;
		return this;
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a method mapped to "/users" also matches to "/users/".
	 * <p>The default value is {@code true}.
	 */
	public PathMatchConfigurer setUseTrailingSlashMatch(Boolean trailingSlashMatch) {
		this.trailingSlashMatch = trailingSlashMatch;
		return this;
	}

	/**
	 * Configure a path prefix to apply to matching controller methods.
	 * <p>Prefixes are used to enrich the mappings of every {@code @RequestMapping}
	 * method whose controller type is matched by the corresponding
	 * {@code Predicate}. The prefix for the first matching predicate is used.
	 * <p>Consider using {@link org.springframework.web.method.HandlerTypePredicate
	 * HandlerTypePredicate} to group controllers.
	 * @param prefix the prefix to apply
	 * @param predicate a predicate for matching controller types
	 * @since 5.1
	 */
	public PathMatchConfigurer addPathPrefix(String prefix, Predicate<Class<?>> predicate) {
		if (this.pathPrefixes == null) {
			this.pathPrefixes = new LinkedHashMap<>();
		}
		this.pathPrefixes.put(prefix, predicate);
		return this;
	}

	/**
	 * Whether to use suffix pattern match (".*") when matching patterns to
	 * requests. If enabled a method mapped to "/users" also matches to "/users.*".
	 * <p>By default this is set to {@code true}.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link RequestMappingHandlerMapping} on the deprecation of path extension
	 * config options. As there is no replacement for this method, in 5.2.x it is
	 * necessary to set it to {@code false}. In 5.3 the default changes to
	 * {@code false} and use of this property becomes unnecessary.
	 */
	@Deprecated
	public PathMatchConfigurer setUseSuffixPatternMatch(Boolean suffixPatternMatch) {
		this.suffixPatternMatch = suffixPatternMatch;
		return this;
	}

	/**
	 * Whether suffix pattern matching should work only against path extensions
	 * explicitly registered when you
	 * {@link WebMvcConfigurer#configureContentNegotiation configure content
	 * negotiation}. This is generally recommended to reduce ambiguity and to
	 * avoid issues such as when a "." appears in the path for other reasons.
	 * <p>By default this is set to "false".
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link RequestMappingHandlerMapping} on the deprecation of path extension
	 * config options.
	 */
	@Deprecated
	public PathMatchConfigurer setUseRegisteredSuffixPatternMatch(Boolean registeredSuffixPatternMatch) {
		this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
		return this;
	}

	/**
	 * Set the UrlPathHelper to use to resolve the mapping path for the application.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 */
	public PathMatchConfigurer setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
		return this;
	}

	/**
	 * Set the PathMatcher to use for String pattern matching.
	 * <p>By default this is {@link AntPathMatcher}.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 */
	public PathMatchConfigurer setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}


	/**
	 * Return the {@link PathPatternParser} to use, if configured.
	 * @since 5.3
	 */
	@Nullable
	public PathPatternParser getPatternParser() {
		return this.patternParser;
	}

	@Nullable
	@Deprecated
	public Boolean isUseTrailingSlashMatch() {
		return this.trailingSlashMatch;
	}

	@Nullable
	protected Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}

	/**
	 * Whether to use registered suffixes for pattern matching.
	 * @deprecated as of 5.2.4, see deprecation note on
	 * {@link #setUseRegisteredSuffixPatternMatch(Boolean)}.
	 */
	@Nullable
	@Deprecated
	public Boolean isUseRegisteredSuffixPatternMatch() {
		return this.registeredSuffixPatternMatch;
	}

	/**
	 * Whether to use registered suffixes for pattern matching.
	 * @deprecated as of 5.2.4, see deprecation note on
	 * {@link #setUseSuffixPatternMatch(Boolean)}.
	 */
	@Nullable
	@Deprecated
	public Boolean isUseSuffixPatternMatch() {
		return this.suffixPatternMatch;
	}

	@Nullable
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	@Nullable
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * Return the configure UrlPathHelper instance or a default (shared) instance.
	 * @since 5.3
	 */
	protected UrlPathHelper getUrlPathHelperOrDefault() {
		if (this.urlPathHelper != null) {
			return this.urlPathHelper;
		}
		if (this.defaultUrlPathHelper == null) {
			this.defaultUrlPathHelper = new UrlPathHelper();
		}
		return this.defaultUrlPathHelper;
	}

	/**
	 * Return the configure PathMatcher instance or a default (shared) instance.
	 * @since 5.3
	 */
	protected PathMatcher getPathMatcherOrDefault() {
		if (this.pathMatcher != null) {
			return this.pathMatcher;
		}
		if (this.defaultPathMatcher == null) {
			this.defaultPathMatcher = new AntPathMatcher();
		}
		return this.defaultPathMatcher;
	}

}
