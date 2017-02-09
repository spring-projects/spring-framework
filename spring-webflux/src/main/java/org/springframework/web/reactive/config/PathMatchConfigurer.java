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

package org.springframework.web.reactive.config;

import org.springframework.web.server.support.HttpRequestPathHelper;

/**
 * Assist with configuring {@code HandlerMapping}'s with path matching options.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class PathMatchConfigurer {

	private boolean suffixPatternMatch = false;

	private boolean trailingSlashMatch = true;

	private boolean registeredSuffixPatternMatch = false;

	private HttpRequestPathHelper pathHelper;


	/**
	 * Whether to use suffix pattern match (".*") when matching patterns to
	 * requests. If enabled a method mapped to "/users" also matches to "/users.*".
	 * <p>By default this is set to {@code false}.
	 * @see #registeredSuffixPatternMatch
	 */
	public PathMatchConfigurer setUseSuffixPatternMatch(boolean suffixPatternMatch) {
		this.suffixPatternMatch = suffixPatternMatch;
		return this;
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a method mapped to "/users" also matches to "/users/".
	 * <p>The default value is {@code true}.
	 */
	public PathMatchConfigurer setUseTrailingSlashMatch(boolean trailingSlashMatch) {
		this.trailingSlashMatch = trailingSlashMatch;
		return this;
	}

	/**
	 * Whether suffix pattern matching should work only against path extensions
	 * that are explicitly registered. This is generally recommended to reduce
	 * ambiguity and to avoid issues such as when a "." (dot) appears in the path
	 * for other reasons.
	 * <p>By default this is set to "false".
	 */
	public PathMatchConfigurer setUseRegisteredSuffixPatternMatch(boolean registeredSuffixPatternMatch) {
		this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
		return this;
	}

	/**
	 * Set a {@code HttpRequestPathHelper} for the resolution of lookup paths.
	 * <p>Default is {@code HttpRequestPathHelper}.
	 */
	public PathMatchConfigurer setPathHelper(HttpRequestPathHelper pathHelper) {
		this.pathHelper = pathHelper;
		return this;
	}

	protected boolean isUseSuffixPatternMatch() {
		return this.suffixPatternMatch;
	}

	protected boolean isUseTrailingSlashMatch() {
		return this.trailingSlashMatch;
	}

	protected boolean isUseRegisteredSuffixPatternMatch() {
		return this.registeredSuffixPatternMatch;
	}

	protected HttpRequestPathHelper getPathHelper() {
		return this.pathHelper;
	}

}
