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
 * A wildcard path element. In the pattern '/foo/&ast;/goo' the * is
 * represented by a WildcardPathElement. Within a path it matches at least 
 * one character but at the end of a path it can match zero characters.
 *
 * @author Andy Clement
 * @since 5.0
 */
class WildcardPathElement extends PathElement {

	public WildcardPathElement(int pos, char separator) {
		super(pos, separator);
	}

	/**
	 * Matching on a WildcardPathElement is quite straight forward. Scan the 
	 * candidate from the candidateIndex onwards for the next separator or the end of the
	 * candidate.
	 */
	@Override
	public boolean matches(int candidateIndex, MatchingContext matchingContext) {
		int nextPos = matchingContext.scanAhead(candidateIndex);
		if (next == null) {
			if (matchingContext.determineRemainingPath) {
				matchingContext.remainingPathIndex = nextPos;
				return true;
			}
			else {
				if (nextPos == matchingContext.candidateLength) {
					return true;
				}
				else {
					return matchingContext.isAllowOptionalTrailingSlash() && // if optional slash is on...
						   nextPos > candidateIndex && // and there is at least one character to match the *...
						   (nextPos + 1) == matchingContext.candidateLength &&  // and the nextPos is the end of the candidate...
						   matchingContext.candidate[nextPos] == separator; // and the final character is a separator
				}
			}
		}
		else { 
			if (matchingContext.isMatchStartMatching && nextPos == matchingContext.candidateLength) {
				return true; // no more data but matches up to this point
			}
			// Within a path (e.g. /aa/*/bb) there must be at least one character to match the wildcard
			if (nextPos == candidateIndex) {
				return false;
			}
			return next.matches(nextPos, matchingContext);
		}
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	public String toString() {
		return "Wildcard(*)";
	}

	@Override
	public int getWildcardCount() {
		return 1;
	}

	@Override
	public int getScore() {
		return WILDCARD_WEIGHT;
	}

}