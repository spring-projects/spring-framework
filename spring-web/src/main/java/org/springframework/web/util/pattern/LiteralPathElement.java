/*
 * Copyright 2002-2023 the original author or authors.
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
				if (pathIndex == matchingContext.pathLength) {
					return true;
				}
				else {
					return (matchingContext.isMatchOptionalTrailingSeparator() &&
							(pathIndex + 1) == matchingContext.pathLength &&
							matchingContext.isSeparator(pathIndex));
				}
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
