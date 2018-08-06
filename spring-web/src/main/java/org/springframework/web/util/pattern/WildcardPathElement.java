/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.PathContainer.Element;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

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
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		String segmentData = null;
		// Assert if it exists it is a segment
		if (pathIndex < matchingContext.pathLength) {
			Element element = matchingContext.pathElements.get(pathIndex);
			if (!(element instanceof PathContainer.PathSegment)) {
				// Should not match a separator
				return false;
			}
			segmentData = ((PathContainer.PathSegment)element).valueToMatch();
			pathIndex++;
		}

		if (isNoMorePattern()) {
			if (matchingContext.determineRemainingPath) {
				matchingContext.remainingPathIndex = pathIndex;
				return true;
			}
			else {
				if (pathIndex == matchingContext.pathLength) {
					// and the path data has run out too
					return true;
				}
				else {
					return (matchingContext.isMatchOptionalTrailingSeparator() &&  // if optional slash is on...
							segmentData != null && segmentData.length() > 0 &&  // and there is at least one character to match the *...
							(pathIndex + 1) == matchingContext.pathLength &&   // and the next path element is the end of the candidate...
							matchingContext.isSeparator(pathIndex));  // and the final element is a separator
				}
			}
		}
		else {
			// Within a path (e.g. /aa/*/bb) there must be at least one character to match the wildcard
			if (segmentData == null || segmentData.length() == 0) {
				return false;
			}
			return (this.next != null && this.next.matches(pathIndex, matchingContext));
		}
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public int getWildcardCount() {
		return 1;
	}

	@Override
	public int getScore() {
		return WILDCARD_WEIGHT;
	}


	public String toString() {
		return "Wildcard(*)";
	}

	@Override
	public char[] getChars() {
		return new char[] {'*'};
	}
}
