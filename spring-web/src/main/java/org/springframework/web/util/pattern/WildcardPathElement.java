/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer.Element;
import org.springframework.http.server.PathContainer.PathSegment;
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
			if (!(element instanceof PathSegment pathSegment)) {
				// Should not match a separator
				return false;
			}
			segmentData = pathSegment.valueToMatch();
			pathIndex++;
		}

		if (isNoMorePattern()) {
			if (matchingContext.determineRemainingPath) {
				matchingContext.remainingPathIndex = pathIndex;
				return true;
			}
			else {
				// and the path data has run out too
				return (pathIndex == matchingContext.pathLength);
			}
		}
		else {
			// Within a path (e.g. /aa/*/bb) there must be at least one character to match the wildcard
			if (segmentData == null || segmentData.isEmpty()) {
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
	public char[] getChars() {
		return new char[] {'*'};
	}

	@Override
	public int getWildcardCount() {
		return 1;
	}

	@Override
	public int getScore() {
		return WILDCARD_WEIGHT;
	}


	@Override
	public String toString() {
		return "Wildcard(*)";
	}

}
