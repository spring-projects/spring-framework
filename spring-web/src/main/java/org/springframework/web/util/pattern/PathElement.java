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

package org.springframework.web.util.pattern;

import java.nio.charset.StandardCharsets;

import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * Common supertype for the Ast nodes created to represent a path pattern.
 *
 * @author Andy Clement
 * @since 5.0
 */
abstract class PathElement {

	// Score related
	protected static final int WILDCARD_WEIGHT = 100;

	protected static final int CAPTURE_VARIABLE_WEIGHT = 1;


	// Position in the pattern where this path element starts
	protected final int pos;

	// The separator used in this path pattern
	protected final char separator;

	// The next path element in the chain
	protected PathElement next;

	// The previous path element in the chain
	protected PathElement prev;


	/**
	 * Create a new path element.
	 * @param pos the position where this path element starts in the pattern data
	 * @param separator the separator in use in the path pattern
	 */
	PathElement(int pos, char separator) {
		this.pos = pos;
		this.separator = separator;
	}


	/**
	 * Attempt to match this path element.
	 * @param candidatePos the current position within the candidate path
	 * @param matchingContext encapsulates context for the match including the candidate
	 * @return {@code true} if it matches, otherwise {@code false}
	 */
	public abstract boolean matches(int candidatePos, MatchingContext matchingContext);

	/**
	 * @return the length of the path element where captures are considered to be one character long.
	 */
	public abstract int getNormalizedLength();

	/**
	 * Return the number of variables captured by the path element.
	 */
	public int getCaptureCount() {
		return 0;
	}

	/**
	 * Return the number of wildcard elements (*, ?) in the path element.
	 */
	public int getWildcardCount() {
		return 0;
	}

	/**
	 * Return the score for this PathElement, combined score is used to compare parsed patterns.
	 */
	public int getScore() {
		return 0;
	}

	/**
	 * Return {@code true} if there is no next character, or if there is then it is a separator.
	 */
	protected boolean nextIfExistsIsSeparator(int nextIndex, MatchingContext matchingContext) {
		return (nextIndex >= matchingContext.candidateLength ||
				matchingContext.candidate[nextIndex] == this.separator);
	}

	/**
	 * Decode an input CharSequence if necessary.
	 * @param toDecode the input char sequence that should be decoded if necessary
	 * @return the decoded result
	 */
	protected String decode(CharSequence toDecode) {
		CharSequence decoded = toDecode;
		if (includesPercent(toDecode)) {
			decoded = UriUtils.decode(toDecode.toString(), StandardCharsets.UTF_8);
		}
		return decoded.toString();
	}

	/**
	 * @param chars sequence of characters
	 * @param from start position (included in check)
	 * @param to end position (excluded from check)
	 * @return true if the chars array includes a '%' character between the specified positions
	 */
	protected boolean includesPercent(char[] chars, int from, int to) {
		for (int i = from; i < to; i++) {
			if (chars[i] == '%') {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param chars string that may include a '%' character indicating it is encoded
	 * @return true if the string contains a '%' character
	 */
	protected boolean includesPercent(CharSequence chars) {
		for (int i = 0, max = chars.length(); i < max; i++) {
			if (chars.charAt(i) == '%') {
				return true;
			}
		}
		return false;
	}

}
