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

package org.springframework.http.server;

import java.util.List;

import org.springframework.util.MultiValueMap;

/**
 * Structured representation of a URI path whose elements have been pre-parsed
 * into a sequence of {@link Separator Separator} and {@link PathSegment
 * PathSegment} elements.
 *
 * <p>An instance of this class can be created via {@link #parsePath(String)}.
 * Each {@link PathSegment PathSegment} exposes its structure decoded
 * safely without the risk of encoded reserved characters altering the path or
 * segment structure and without path parameters for path matching purposes.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface PathContainer {

	/**
	 * The original (raw, encoded) path that this instance was parsed from.
	 */
	String value();

	/**
	 * The list of path elements, either {@link Separator} or {@link PathSegment}.
	 */
	List<Element> elements();

	/**
	 * Extract a sub-path from the given offset into the elements list.
	 * @param index the start element index (inclusive)
	 * @return the sub-path
	 */
	default PathContainer subPath(int index) {
		return subPath(index, elements().size());
	}

	/**
	 * Extract a sub-path from the given start offset (inclusive) into the
	 * element list and to the end offset (exclusive).
	 * @param startIndex the start element index (inclusive)
	 * @param endIndex the end element index (exclusive)
	 * @return the sub-path
	 */
	default PathContainer subPath(int startIndex, int endIndex) {
		return DefaultPathContainer.subPath(this, startIndex, endIndex);
	}


	/**
	 * Parse the path value into a sequence of {@link Separator Separator} and
	 * {@link PathSegment PathSegment} elements.
	 * @param path the encoded, raw URL path value to parse
	 * @return the parsed path
	 */
	static PathContainer parsePath(String path) {
		return DefaultPathContainer.createFromUrlPath(path);
	}


	/**
	 * Common representation of a path element, e.g. separator or segment.
	 */
	interface Element {

		/**
		 * Return the original (raw, encoded) value of this path element.
		 */
		String value();
	}


	/**
	 * Path separator element.
	 */
	interface Separator extends Element {
	}


	/**
	 * Path segment element.
	 */
	interface PathSegment extends Element {

		/**
		 * Return the path segment value to use for pattern matching purposes.
		 * By default this is the same as {@link #value()} but may also differ
		 * in sub-interfaces (e.g. decoded, sanitized, etc.).
		 */
		String valueToMatch();

		/**
		 * The same as {@link #valueToMatch()} but as a {@code char[]}.
		 */
		char[] valueToMatchAsChars();

		/**
		 * Path parameters parsed from the path segment.
		 */
		MultiValueMap<String, String> parameters();
	}

}
