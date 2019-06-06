/*
 * Copyright 2002-2019 the original author or authors.
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
 * Structured representation of a URI path parsed via {@link #parsePath(String)}
 * into a sequence of {@link Separator} and {@link PathSegment} elements.
 *
 * <p>Each {@link PathSegment} exposes its content in decoded form and with path
 * parameters removed. This makes it safe to match one path segment at a time
 * without the risk of decoded reserved characters altering the structure of
 * the path.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface PathContainer {

	/**
	 * The original path from which this instance was parsed.
	 */
	String value();

	/**
	 * The contained path elements, either {@link Separator} or {@link PathSegment}.
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
	 * Extract a sub-path from the given start offset into the element list
	 * (inclusive) and to the end offset (exclusive).
	 * @param startIndex the start element index (inclusive)
	 * @param endIndex the end element index (exclusive)
	 * @return the sub-path
	 */
	default PathContainer subPath(int startIndex, int endIndex) {
		return DefaultPathContainer.subPath(this, startIndex, endIndex);
	}


	/**
	 * Parse the path value into a sequence of {@code "/"} {@link Separator Separator}
	 * and {@link PathSegment PathSegment} elements.
	 * @param path the encoded, raw path value to parse
	 * @return the parsed path
	 */
	static PathContainer parsePath(String path) {
		return DefaultPathContainer.createFromUrlPath(path, "/");
	}

	/**
	 * Parse the path value into a sequence of {@link Separator Separator} and
	 * {@link PathSegment PathSegment} elements.
	 * @param path the encoded, raw path value to parse
	 * @param separator the decoded separator for parsing patterns
	 * @return the parsed path
	 * @since 5.2
	 */
	static PathContainer parsePath(String path, String separator) {
		return DefaultPathContainer.createFromUrlPath(path, separator);
	}


	/**
	 * A path element, either separator or path segment.
	 */
	interface Element {

		/**
		 * The unmodified, original value of this element.
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
		 * Return the path segment value, decoded and sanitized, for path matching.
		 */
		String valueToMatch();

		/**
		 * Expose {@link #valueToMatch()} as a character array.
		 */
		char[] valueToMatchAsChars();

		/**
		 * Path parameters associated with this path segment.
		 */
		MultiValueMap<String, String> parameters();
	}

}
