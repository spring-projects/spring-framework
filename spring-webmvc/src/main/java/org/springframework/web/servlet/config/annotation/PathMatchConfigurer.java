/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.util.PathMatcher;
import org.springframework.web.util.UrlPathHelper;

/**
 * Helps with configuring {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}
 * path matching options such as trailing slash match, suffix registration or path matcher/helper.
 *
 * @author Brian Clozel
 * @since 4.0.3
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
 */
public class PathMatchConfigurer {

	private Boolean useSuffixPatternMatch;

	private Boolean useTrailingSlashMatch;

	private Boolean useRegisteredSuffixPatternMatch;

	private UrlPathHelper urlPathHelper;

	private PathMatcher pathMatcher;


	/**
	 * Whether to use suffix pattern match (".*") when matching patterns to
	 * requests. If enabled a method mapped to "/users" also matches to "/users.*".
	 * <p>The default value is {@code true}.
	 */
	public PathMatchConfigurer setUseSuffixPatternMatch(Boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		return this;
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a method mapped to "/users" also matches to "/users/".
	 * <p>The default value is {@code true}.
	 */
	public PathMatchConfigurer setUseTrailingSlashMatch(Boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
		return this;
	}

	/**
	 * Whether to use suffix pattern match for registered file extensions only
	 * when matching patterns to requests.
	 * <p>If enabled, a controller method mapped to "/users" also matches to
	 * "/users.json" assuming ".json" is a file extension registered with the
	 * provided {@link org.springframework.web.accept.ContentNegotiationManager}.</p>
	 * <p>The {@link org.springframework.web.accept.ContentNegotiationManager} can be customized
	 * using a {@link ContentNegotiationConfigurer}.</p>
	 * <p>If enabled, this flag also enables
	 * {@link #setUseSuffixPatternMatch(Boolean) useSuffixPatternMatch}. The
	 * default value is {@code false}.</p>
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
	 * @see ContentNegotiationConfigurer
	 *
	 */
	public PathMatchConfigurer setUseRegisteredSuffixPatternMatch(Boolean useRegisteredSuffixPatternMatch) {
		this.useRegisteredSuffixPatternMatch = useRegisteredSuffixPatternMatch;
		return this;
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths.
	 * <p>Use this to override the default UrlPathHelper with a custom subclass,
	 * or to share common UrlPathHelper settings across multiple HandlerMappings
	 * and MethodNameResolvers.
	 */
	public PathMatchConfigurer setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
		return this;
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns. Default is AntPathMatcher.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public PathMatchConfigurer setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}


	public Boolean isUseSuffixPatternMatch() {
		return this.useSuffixPatternMatch;
	}

	public Boolean isUseTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	public Boolean isUseRegisteredSuffixPatternMatch() {
		return this.useRegisteredSuffixPatternMatch;
	}

	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

}
