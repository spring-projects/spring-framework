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

}
