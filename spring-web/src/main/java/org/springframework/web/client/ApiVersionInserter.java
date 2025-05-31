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

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;

/**
 * Contract to determine how to insert an API version into the URI or headers
 * of a request.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface ApiVersionInserter {

	/**
	 * Insert the version into the URI.
	 * <p>The default implementation returns the supplied URI unmodified.
	 * @param version the version to insert
	 * @param uri the URI for the request
	 * @return the updated URI, or the original URI unmodified
	 */
	default URI insertVersion(Object version, URI uri) {
		return uri;
	}

	/**
	 * Insert the version into the request headers.
	 * <p>The default implementation does not modify the supplied headers.
	 * @param version the version to insert
	 * @param headers the request headers
	 */
	default void insertVersion(Object version, HttpHeaders headers) {
	}


	/**
	 * Create an inserter that sets a header.
	 * @param header the name of a header to hold the version
	 */
	static ApiVersionInserter useHeader(@Nullable String header) {
		return new DefaultApiVersionInserterBuilder(header, null, null).build();
	}

	/**
	 * Create an inserter that sets a query parameter.
	 * @param queryParam the name of a query parameter to hold the version
	 */
	static ApiVersionInserter useQueryParam(@Nullable String queryParam) {
		return new DefaultApiVersionInserterBuilder(null, queryParam, null).build();
	}

	/**
	 * Create an inserter that inserts a path segment.
	 * @param pathSegmentIndex the index of the path segment to hold the version
	 */
	static ApiVersionInserter usePathSegment(@Nullable Integer pathSegmentIndex) {
		return new DefaultApiVersionInserterBuilder(null, null, pathSegmentIndex).build();
	}

	/**
	 * Create a builder for an {@link ApiVersionInserter}.
	 */
	static Builder builder() {
		return new DefaultApiVersionInserterBuilder(null, null, null);
	}


	/**
	 * Builder for {@link ApiVersionInserter}.
	 */
	interface Builder {

		/**
		 * Configure the inserter to set a header.
		 * @param header the name of the header to hold the version
		 */
		Builder useHeader(@Nullable String header);

		/**
		 * Configure the inserter to set a query parameter.
		 * @param queryParam the name of the query parameter to hold the version
		 */
		Builder useQueryParam(@Nullable String queryParam);

		/**
		 * Configure the inserter to insert a path segment.
		 * @param pathSegmentIndex the index of the path segment to hold the version
		 */
		Builder usePathSegment(@Nullable Integer pathSegmentIndex);

		/**
		 * Format the version Object into a String using the given {@link ApiVersionFormatter}.
		 * <p>By default, the version is formatted with {@link Object#toString()}.
		 * @param versionFormatter the formatter to use
		 */
		Builder withVersionFormatter(ApiVersionFormatter versionFormatter);

		/**
		 * Build the {@link ApiVersionInserter} instance.
		 */
		ApiVersionInserter build();

	}

}
