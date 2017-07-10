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

import org.springframework.util.MultiValueMap;

/**
 * Structured representation of a path whose {@link Element Elements} are
 * accessible as a sequence of either {@link Separator Separator} and/or
 * {@link Segment Segment} (element) types.
 *
 * <p>Each {@code Segment} exposes its own structure decoded safely without the
 * risk of encoded reserved characters altering the path or segment structure.
 *
 * <p>An instance of this class can also be created via
 * {@link #parse(String, Charset)}. The path for an HTTP request is parsed once
 * and subsequently accessible via {@link ServerHttpRequest#getPath()}.
 *
 * @author Rossen Stoyanchev
 */
public interface PathContainer {

	/**
	 * The original, raw (encoded) path value including path parameters.
	 */
	String value();

	/**
	 * The list of path elements, either {@link Separator} or {@link Segment}.
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
	 * Parse the given path value into a {@link PathContainer}.
	 * @param path the encoded, raw path value to parse
	 * @param encoding the charset to use for decoded path segment values
	 * @return the parsed path
	 */
	static PathContainer parse(String path, Charset encoding) {
		return DefaultPathContainer.parsePath(path, encoding);
	}


	/**
	 * Common representation of a path element, e.g. separator or segment.
	 */
	interface Element {

		/**
		 * Return the original, raw (encoded) value for the path component.
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
	interface Segment extends Element {

		/**
		 * Return the path segment {@link #value()} decoded.
		 */
		String valueDecoded();

		/**
		 * Variant of {@link #valueDecoded()} as a {@code char[]}.
		 */
		char[] valueDecodedChars();

		/**
		 * Return the portion of the path segment after and including the first
		 * ";" (semicolon) representing path parameters. The actual parsed
		 * parameters if any can be obtained via {@link #parameters()}.
		 */
		String semicolonContent();

		/**
		 * Path parameters parsed from the path segment.
		 */
		MultiValueMap<String, String> parameters();
	}

}
