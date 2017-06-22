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
package org.springframework.http.server.reactive;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Container for 0..N path segments.
 *
 * <p>Typically consumed via {@link ServerHttpRequest#getPath()} but can also
 * be created by parsing a path value via {@link #parse(String, Charset)}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see RequestPath
 */
public interface PathSegmentContainer {

	/**
	 * The original, raw (encoded) path value including path parameters.
	 */
	String value();

	/**
	 * Whether the path (encoded or decoded) is empty meaning that it has
	 * {@link Character#isWhitespace whitespace} characters or none.
	 */
	boolean isEmpty();

	/**
	 * Whether the path {@link #value()} starts with "/".
	 */
	boolean isAbsolute();

	/**
	 * The list of path segments contained.
	 */
	List<PathSegment> pathSegments();

	/**
	 * Whether the path {@link #value()} ends with "/".
	 */
	boolean hasTrailingSlash();


	/**
	 * Parse the given path value into a {@link PathSegmentContainer}.
	 * @param path the value to parse
	 * @param encoding the charset to use for decoded path segment values
	 * @return the parsed path
	 */
	static PathSegmentContainer parse(String path, Charset encoding) {
		return DefaultPathSegmentContainer.parsePath(path, encoding);
	}

	/**
	 * Extract a sub-path starting at the given offset into the path segment list.
	 * @param path the path to extract from
	 * @param pathSegmentIndex the start index (inclusive)
	 * @return the sub-path
	 */
	static PathSegmentContainer subPath(PathSegmentContainer path, int pathSegmentIndex) {
		return DefaultPathSegmentContainer.subPath(path, pathSegmentIndex, path.pathSegments().size());
	}

}
