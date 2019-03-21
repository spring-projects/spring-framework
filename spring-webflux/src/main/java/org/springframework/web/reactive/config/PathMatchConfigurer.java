/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.config;

import org.springframework.lang.Nullable;

/**
 * Assist with configuring {@code HandlerMapping}'s with path matching options.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class PathMatchConfigurer {

	@Nullable
	private Boolean trailingSlashMatch;


	@Nullable
	private Boolean caseSensitiveMatch;


	/**
	 * Whether to match to URLs irrespective of their case.
	 * If enabled a method mapped to "/users" won't match to "/Users/".
	 * <p>The default value is {@code false}.
	 */
	public PathMatchConfigurer setUseCaseSensitiveMatch(Boolean caseSensitiveMatch) {
		this.caseSensitiveMatch = caseSensitiveMatch;
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

	@Nullable
	protected Boolean isUseTrailingSlashMatch() {
		return this.trailingSlashMatch;
	}

	@Nullable
	protected Boolean isUseCaseSensitiveMatch() {
		return this.caseSensitiveMatch;
	}

}
