/*
 * Copyright 2002-2025 the original author or authors.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Default implementation of {@link ApiVersionInserter} to insert the version
 * into a request header, query parameter, or the URL path.
 *
 * <p>Use {@link #builder()} to create an instance.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public final class DefaultApiVersionInserter implements ApiVersionInserter {

	private final @Nullable String header;

	private final @Nullable String queryParam;

	private final @Nullable Integer pathSegmentIndex;

	private final ApiVersionFormatter versionFormatter;


	private DefaultApiVersionInserter(
			@Nullable String header, @Nullable String queryParam, @Nullable Integer pathSegmentIndex,
			@Nullable ApiVersionFormatter formatter) {

		Assert.isTrue(header != null || queryParam != null || pathSegmentIndex != null,
				"Expected 'header', 'queryParam', or 'pathSegmentIndex' to be configured");

		this.header = header;
		this.queryParam = queryParam;
		this.pathSegmentIndex = pathSegmentIndex;
		this.versionFormatter = (formatter != null ? formatter : Object::toString);
	}


	@Override
	public URI insertVersion(Object version, URI uri) {
		if (this.queryParam == null && this.pathSegmentIndex == null) {
			return uri;
		}
		String formattedVersion = this.versionFormatter.formatVersion(version);
		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(uri);
		if (this.queryParam != null) {
			builder.queryParam(this.queryParam, formattedVersion);
		}
		if (this.pathSegmentIndex != null) {
			List<String> pathSegments = new ArrayList<>(builder.build().getPathSegments());
			assertPathSegmentIndex(this.pathSegmentIndex, pathSegments.size(), uri);
			pathSegments.add(this.pathSegmentIndex, formattedVersion);
			builder.replacePath(null);
			pathSegments.forEach(builder::pathSegment);
		}
		return builder.build().toUri();
	}

	private void assertPathSegmentIndex(Integer index, int pathSegmentsSize, URI uri) {
		Assert.state(index <= pathSegmentsSize,
				"Cannot insert version into '" + uri.getPath() + "' at path segment index " + index);
	}

	@Override
	public void insertVersion(Object version, HttpHeaders headers) {
		if (this.header != null) {
			headers.set(this.header, this.versionFormatter.formatVersion(version));
		}
	}


	/**
	 * Create a builder for an inserter that sets a header.
	 * @param header the name of a header to hold the version
	 */
	public static Builder fromHeader(@Nullable String header) {
		return new Builder(header, null, null);
	}

	/**
	 * Create a builder for an inserter that sets a query parameter.
	 * @param queryParam the name of a query parameter to hold the version
	 */
	public static Builder fromQueryParam(@Nullable String queryParam) {
		return new Builder(null, queryParam, null);
	}

	/**
	 * Create a builder for an inserter that inserts a path segment.
	 * @param pathSegmentIndex the index of the path segment to hold the version
	 */
	public static Builder fromPathSegment(@Nullable Integer pathSegmentIndex) {
		return new Builder(null, null, pathSegmentIndex);
	}

	/**
	 * Create a builder.
	 */
	public static Builder builder() {
		return new Builder(null, null, null);
	}


	/**
	 * A builder for {@link DefaultApiVersionInserter}.
	 */
	public static final class Builder {

		private @Nullable String header;

		private @Nullable String queryParam;

		private @Nullable Integer pathSegmentIndex;

		private @Nullable ApiVersionFormatter versionFormatter;

		private Builder(@Nullable String header, @Nullable String queryParam, @Nullable Integer pathSegmentIndex) {
			this.header = header;
			this.queryParam = queryParam;
			this.pathSegmentIndex = pathSegmentIndex;
		}

		/**
		 * Configure the inserter to set a header.
		 * @param header the name of the header to hold the version
		 */
		public Builder fromHeader(@Nullable String header) {
			this.header = header;
			return this;
		}

		/**
		 * Configure the inserter to set a query parameter.
		 * @param queryParam the name of the query parameter to hold the version
		 */
		public Builder fromQueryParam(@Nullable String queryParam) {
			this.queryParam = queryParam;
			return this;
		}

		/**
		 * Configure the inserter to insert a path segment.
		 * @param pathSegmentIndex the index of the path segment to hold the version
		 */
		public Builder fromPathSegment(@Nullable Integer pathSegmentIndex) {
			this.pathSegmentIndex = pathSegmentIndex;
			return this;
		}

		/**
		 * Format the version Object into a String using the given {@link ApiVersionFormatter}.
		 * <p>By default, the version is formatted with {@link Object#toString()}.
		 * @param versionFormatter the formatter to use
		 */
		public Builder withVersionFormatter(ApiVersionFormatter versionFormatter) {
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

}
