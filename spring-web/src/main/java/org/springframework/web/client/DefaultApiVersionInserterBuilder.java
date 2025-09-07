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

	private @Nullable String mediaTypeParam;

	private @Nullable Integer pathSegmentIndex;

	private @Nullable ApiVersionFormatter versionFormatter;


	DefaultApiVersionInserterBuilder(
			@Nullable String header, @Nullable String queryParam, @Nullable String mediaTypeParam,
			@Nullable Integer pathSegmentIndex) {

		this.header = header;
		this.queryParam = queryParam;
		this.mediaTypeParam = mediaTypeParam;
		this.pathSegmentIndex = pathSegmentIndex;
	}

	/**
	 * Configure the inserter to set a header.
	 * @param header the name of the header to hold the version
	 */
	@Override
	public ApiVersionInserter.Builder useHeader(@Nullable String header) {
		this.header = header;
		return this;
	}

	@Override
	public ApiVersionInserter.Builder useQueryParam(@Nullable String queryParam) {
		this.queryParam = queryParam;
		return this;
	}

	@Override
	public ApiVersionInserter.Builder useMediaTypeParam(@Nullable String param) {
		this.mediaTypeParam = param;
		return this;
	}

	@Override
	public ApiVersionInserter.Builder usePathSegment(@Nullable Integer pathSegmentIndex) {
		this.pathSegmentIndex = pathSegmentIndex;
		return this;
	}

	@Override
	public ApiVersionInserter.Builder withVersionFormatter(ApiVersionFormatter versionFormatter) {
		this.versionFormatter = versionFormatter;
		return this;
	}

	public ApiVersionInserter build() {
		return new DefaultApiVersionInserter(
				this.header, this.queryParam, this.mediaTypeParam, this.pathSegmentIndex,
				this.versionFormatter);
	}

}
