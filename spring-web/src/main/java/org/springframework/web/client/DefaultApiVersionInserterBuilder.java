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

package org.springframework.web.client;

import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link ApiVersionInserter.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @see ApiVersionInserter#useHeader(String)
 * @see ApiVersionInserter#useQueryParam(String)
 * @see ApiVersionInserter#usePathSegment(Integer)
 */
final class DefaultApiVersionInserterBuilder implements ApiVersionInserter.Builder {

	private @Nullable String header;

	private @Nullable String queryParam;

	private @Nullable Integer pathSegmentIndex;

	private @Nullable ApiVersionFormatter versionFormatter;


	DefaultApiVersionInserterBuilder(
			@Nullable String header, @Nullable String queryParam, @Nullable Integer pathSegmentIndex) {

		this.header = header;
		this.queryParam = queryParam;
		this.pathSegmentIndex = pathSegmentIndex;
	}

	/**
	 * Configure the inserter to set a header.
	 * @param header the name of the header to hold the version
	 */
	public ApiVersionInserter.Builder useHeader(@Nullable String header) {
		this.header = header;
		return this;
	}

	/**
	 * Configure the inserter to set a query parameter.
	 * @param queryParam the name of the query parameter to hold the version
	 */
	public ApiVersionInserter.Builder useQueryParam(@Nullable String queryParam) {
		this.queryParam = queryParam;
		return this;
	}

	/**
	 * Configure the inserter to insert a path segment.
	 * @param pathSegmentIndex the index of the path segment to hold the version
	 */
	public ApiVersionInserter.Builder usePathSegment(@Nullable Integer pathSegmentIndex) {
		this.pathSegmentIndex = pathSegmentIndex;
		return this;
	}

	/**
	 * Format the version Object into a String using the given {@link ApiVersionFormatter}.
	 * <p>By default, the version is formatted with {@link Object#toString()}.
	 * @param versionFormatter the formatter to use
	 */
	public ApiVersionInserter.Builder withVersionFormatter(ApiVersionFormatter versionFormatter) {
		this.versionFormatter = versionFormatter;
		return this;
	}

	/**
	 * Build the inserter.
	 */
	public ApiVersionInserter build() {
		return new DefaultApiVersionInserter(
				this.header, this.queryParam, this.pathSegmentIndex, this.versionFormatter);
	}

}
