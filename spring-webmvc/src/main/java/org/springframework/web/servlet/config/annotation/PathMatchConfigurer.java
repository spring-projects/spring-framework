/*
 * Copyright 2002-2022 the original author or authors.
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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Configure path matching options. The options are applied to the following:
 * <ul>
 * <li>{@link WebMvcConfigurationSupport#requestMappingHandlerMapping}</li>
 * <li>{@link WebMvcConfigurationSupport#viewControllerHandlerMapping}</li>
 * <li>{@link WebMvcConfigurationSupport#beanNameHandlerMapping}</li>
 * <li>{@link WebMvcConfigurationSupport#routerFunctionMapping}</li>
 * <li>{@link WebMvcConfigurationSupport#resourceHandlerMapping}</li>
 * </ul>
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
public class PathMatchConfigurer {

	private boolean preferPathMatcher = false;

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
	private PathPatternParser defaultPatternParser;

	@Nullable
	private UrlPathHelper defaultUrlPathHelper;

	@Nullable
	private PathMatcher defaultPathMatcher;


	/**
	 * Set the {@link PathPatternParser} to parse {@link PathPattern patterns}
	 * with for URL path matching. Parsed patterns provide a more modern and
	 * efficient alternative to String path matching via {@link AntPathMatcher}.
	 * <p><strong>Note:</strong> This property is mutually exclusive with the
	 * following other, {@code AntPathMatcher} related properties:
	 * <ul>
	 * <li>{@link #setUseSuffixPatternMatch(Boolean)}
	 * <li>{@link #setUseRegisteredSuffixPatternMatch(Boolean)}
	 * <li>{@link #setUrlPathHelper(UrlPathHelper)}
	 * <li>{@link #setPathMatcher(PathMatcher)}
	 * </ul>
	 * <p>By default, as of 6.0, a {@link PathPatternParser} with default
	 * settings is used, which enables parsed {@link PathPattern patterns}.
	 * Set this property to {@code null} to fall back on String path matching via
	 * {@link AntPathMatcher} instead, or alternatively, setting one of the above
	 * listed {@code AntPathMatcher} related properties has the same effect.
	 * @param patternParser the parser to pre-parse patterns with
	 * @since 5.3
	 */
	public PathMatchConfigurer setPatternParser(@Nullable PathPatternParser patternParser) {
		this.patternParser = patternParser;
		this.preferPathMatcher = (patternParser == null);
		return this;
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a method mapped to "/users" also matches to "/users/".
	 * <p>The default was changed in 6.0 from {@code true} to {@code false} in
	 * order to support the deprecation of the property.
	 * @deprecated as of 6.0, see
	 * {@link PathPatternParser#setMatchOptionalTrailingSeparator(boolean)}
	 */
	@Deprecated(since = "6.0")
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
	 * <p><strong>Note:</strong> This property is mutually exclusive with
	 * {@link #setPatternParser(PathPatternParser)}. If set, it enables use of
	 * String path matching, unless a {@code PathPatternParser} is also
	 * explicitly set in which case this property is ignored.
	 * <p>By default this is set to {@code false}.
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link RequestMappingHandlerMapping} on the deprecation of path extension
	 * config options. As there is no replacement for this method, in 5.2.x it is
	 * necessary to set it to {@code false}. In 5.3 the default changes to
	 * {@code false} and use of this property becomes unnecessary.
	 */
	@Deprecated
	public PathMatchConfigurer setUseSuffixPatternMatch(@Nullable Boolean suffixPatternMatch) {
		this.suffixPatternMatch = suffixPatternMatch;
		this.preferPathMatcher |= (suffixPatternMatch != null && suffixPatternMatch);
		return this;
	}

	/**
	 * Whether suffix pattern matching should work only against path extensions
	 * explicitly registered when you
	 * {@link WebMvcConfigurer#configureContentNegotiation configure content
	 * negotiation}. This is generally recommended to reduce ambiguity and to
	 * avoid issues such as when a "." appears in the path for other reasons.
	 * <p><strong>Note:</strong> This property is mutually exclusive with
	 * {@link #setPatternParser(PathPatternParser)}. If set, it enables use of
	 * String path matching, unless a {@code PathPatternParser} is also
	 * explicitly set in which case this property is ignored.
	 * <p>By default this is set to "false".
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link RequestMappingHandlerMapping} on the deprecation of path extension
	 * config options.
	 */
	@Deprecated
	public PathMatchConfigurer setUseRegisteredSuffixPatternMatch(@Nullable Boolean registeredSuffixPatternMatch) {
		this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
		this.preferPathMatcher |= (registeredSuffixPatternMatch != null && registeredSuffixPatternMatch);
		return this;
	}

	/**
	 * Set the UrlPathHelper to use to resolve the mapping path for the application.
	 * <p><strong>Note:</strong> This property is mutually exclusive with
	 * {@link #setPatternParser(PathPatternParser)}. If set, it enables use of
	 * String path matching, unless a {@code PathPatternParser} is also
	 * explicitly set in which case this property is ignored.
	 * <p>By default this is an instance of {@link UrlPathHelper} with default
	 * settings.
	 */
	public PathMatchConfigurer setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
		this.preferPathMatcher = true;
		return this;
	}

	/**
	 * Set the PathMatcher to use for String pattern matching.
	 * <p><strong>Note:</strong> This property is mutually exclusive with
	 * {@link #setPatternParser(PathPatternParser)}. If set, it enables use of
	 * String path matching, unless a {@code PathPatternParser} is also
	 * explicitly set in which case this property is ignored.
	 * <p>By default this is an instance of {@link AntPathMatcher} with default
	 * settings.
	 */
	public PathMatchConfigurer setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		this.preferPathMatcher = true;
		return this;
	}


	/**
	 * Whether to prefer {@link PathMatcher}. This is the case when either is true:
	 * <ul>
	 * <li>{@link PathPatternParser} is explicitly set to {@code null}.
	 * <li>{@link PathPatternParser} is not explicitly set, and a
	 * {@link PathMatcher} related option is explicitly set.
	 * </ul>
	 * @since 6.0
	 */
	protected boolean preferPathMatcher() {
		return (this.patternParser == null && this.preferPathMatcher);
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
	 * Return the configured UrlPathHelper or a default, shared instance otherwise.
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
	 * Return the configured PathMatcher or a default, shared instance otherwise.
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

	/**
	 * Return the configured PathPatternParser or a default, shared instance otherwise.
	 * @since 5.3.4
	 */
	public PathPatternParser getPatternParserOrDefault() {
		if (this.patternParser != null) {
			return this.patternParser;
		}
		if (this.defaultPatternParser == null) {
			this.defaultPatternParser = new PathPatternParser();
		}
		return this.defaultPatternParser;
	}
}
