/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer.Element;
import org.springframework.http.server.PathContainer.PathSegment;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * A literal path element. In the pattern '/foo/bar/goo' there are three
 * literal path elements 'foo', 'bar' and 'goo'.
 *
 * @author Andy Clement
 * @since 5.0
 */
class LiteralPathElement extends PathElement {

	private final String text;

	private final int len;

	private final boolean caseSensitive;


	public LiteralPathElement(int pos, char[] literalText, boolean caseSensitive, char separator) {
		super(pos, separator);
		this.len = literalText.length;
		this.caseSensitive = caseSensitive;
		this.text = new String(literalText);
	}


	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		if (pathIndex >= matchingContext.pathLength) {
			// no more path left to match this element
			return false;
		}
		Element element = matchingContext.pathElements.get(pathIndex);
		if (!(element instanceof PathSegment pathSegment)) {
			return false;
		}
		String value = pathSegment.valueToMatch();
		if (value.length() != this.len) {
			// Not enough data to match this path element
			return false;
		}

		if (this.caseSensitive) {
			if (!this.text.equals(value)) {
				return false;
			}
		}
		else {
			if (!this.text.equalsIgnoreCase(value)) {
				return false;
			}
		}

		pathIndex++;
		if (isNoMorePattern()) {
			if (matchingContext.determineRemainingPath) {
				matchingContext.remainingPathIndex = pathIndex;
				return true;
			}
			else {
				return (pathIndex == matchingContext.pathLength);
			}
		}
		else {
			return (this.next != null && this.next.matches(pathIndex, matchingContext));
		}
	}

	@Override
	public int getNormalizedLength() {
		return this.len;
	}

	@Override
	public char[] getChars() {
		return this.text.toCharArray();
	}

	@Override
	public boolean isLiteral() {
		return true;
	}

	@Override
	public String toString() {
		return "Literal(" + this.text + ")";
	}

}
