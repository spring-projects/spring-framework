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

package org.springframework.web.util.patterns;

import org.springframework.web.util.patterns.PathPattern.MatchingContext;

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

	/**
	 * Position in the pattern where this path element starts
	 */
	protected int pos;

	/**
	 * The next path element in the chain
	 */
	protected PathElement next;

	/**
	 * The previous path element in the chain
	 */
	protected PathElement prev;

	/**
	 * Create a new path element.
	 * @param pos the position where this path element starts in the pattern data
	 */
	PathElement(int pos) {
		this.pos = pos;
	}

	/**
	 * Attempt to match this path element.
	 *
	 * @param candidatePos the current position within the candidate path
	 * @param matchingContext encapsulates context for the match including the candidate
	 * @return true if matches, otherwise false
	 */
	public abstract boolean matches(int candidatePos, MatchingContext matchingContext);

	/**
	 * @return the length of the path element where captures are considered to be one character long
	 */
	public abstract int getNormalizedLength();

	/**
	 * @return the number of variables captured by the path element
	 */
	public int getCaptureCount() {
		return 0;
	}

	/**
	 * @return the number of wildcard elements (*, ?) in the path element
	 */
	public int getWildcardCount() {
		return 0;
	}

	/**
	 * @return the score for this PathElement, combined score is used to compare parsed patterns.
	 */
	public int getScore() {
		return 0;
	}
}