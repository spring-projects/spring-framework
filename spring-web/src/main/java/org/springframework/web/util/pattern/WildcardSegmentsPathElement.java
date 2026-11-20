/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.util.pattern;

/**
 * A path element representing wildcarding multiple segments in a path.
 * This element is only allowed in two situations:
 * <ol>
 * <li>At the start of a path, immediately followed by a {@link LiteralPathElement} like '&#47;**&#47;foo&#47;{bar}'
 * <li>At the end of a path, like '&#47;foo&#47;**'
 * </ol>
 * <p>Only a single {@link WildcardSegmentsPathElement} or {@link CaptureSegmentsPathElement} element is allowed
 * in a pattern. In the pattern '&#47;foo&#47;**' the '&#47;**' is represented as a {@link WildcardSegmentsPathElement}.
 *
 * @author Andy Clement
 * @author Brian Clozel
 * @since 5.0
 */
class WildcardSegmentsPathElement extends PathElement {

	WildcardSegmentsPathElement(int pos, char separator) {
		super(pos, separator);
	}


	@Override
	public boolean matches(int pathIndex, PathPattern.MatchingContext matchingContext) {
		// wildcard segments at the start of the pattern
		if (pathIndex == 0 && this.next != null) {
			int endPathIndex = pathIndex;
			while (endPathIndex < matchingContext.pathLength) {
				if (this.next.matches(endPathIndex, matchingContext)) {
					return true;
				}
				endPathIndex++;
			}
			return false;
		}
		// match until the end of the path
		else if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
			return false;
		}
		if (matchingContext.determineRemainingPath) {
			matchingContext.remainingPathIndex = matchingContext.pathLength;
		}
		return true;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return (this.separator + "**").toCharArray();
	}

	@Override
	public int getWildcardCount() {
		return 1;
	}


	@Override
	public String toString() {
		return "WildcardSegments(" + this.separator + "**)";
	}

}
