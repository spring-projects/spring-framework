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

import org.springframework.util.MultiValueMap;

/**
 * Represents the content of one path segment.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface PathSegment {

	/**
	 * Return the original, raw (encoded) path segment value not including
	 * path parameters.
	 */
	String value();

	/**
	 * Return the path {@link #value()} decoded.
	 */
	String valueDecoded();

	/**
	 * Return the same as {@link #valueDecoded()} but as a {@code char[]}.
	 */
	char[] valueCharsDecoded();

	/**
	 * Whether the path value (encoded or decoded) is empty meaning that it has
	 * {@link Character#isWhitespace whitespace} characters or none.
	 */
	boolean isEmpty();

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


	/**
	 * Parse the given path segment value.
	 * @param path the value to parse
	 * @param encoding the charset to use for the decoded value
	 * @return the parsed path segment
	 */
	static PathSegment parse(String path, Charset encoding) {
		return DefaultPathSegmentContainer.parsePathSegment(path, encoding);
	}

}
