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
 * A literal path element. In the pattern '/foo/bar/goo' there are three
 * literal path elements 'foo', 'bar' and 'goo'.
 *
 * @author Andy Clement
 */
class LiteralPathElement extends PathElement {

	private char[] text;

	private int len;

	private boolean caseSensitive;

	public LiteralPathElement(int pos, char[] literalText, boolean caseSensitive, char separator) {
		super(pos, separator);
		this.len = literalText.length;
		this.caseSensitive = caseSensitive;
		if (caseSensitive) {
			this.text = literalText;
		}
		else {
			// Force all the text lower case to make matching faster
			this.text = new char[literalText.length];
			for (int i = 0; i < len; i++) {
				this.text[i] = Character.toLowerCase(literalText[i]);
			}
		}
	}

	@Override
	public boolean matches(int candidateIndex, MatchingContext matchingContext) {
		if ((candidateIndex + text.length) > matchingContext.candidateLength) {
			return false; // not enough data, cannot be a match
		}
		if (caseSensitive) {
			for (int i = 0; i < len; i++) {
				if (matchingContext.candidate[candidateIndex++] != text[i]) {
					return false;
				}
			}
		}
		else {
			for (int i = 0; i < len; i++) {
				// TODO revisit performance if doing a lot of case insensitive matching
				if (Character.toLowerCase(matchingContext.candidate[candidateIndex++]) != text[i]) {
					return false;
				}
			}
		}
		if (next == null) {
			if (matchingContext.determineRemainingPath && nextIfExistsIsSeparator(candidateIndex, matchingContext)) {
				matchingContext.remainingPathIndex = candidateIndex;
				return true;
			}
			else {
				if (candidateIndex == matchingContext.candidateLength) {
					return true;
				}
				else {
					return matchingContext.isAllowOptionalTrailingSlash() &&
						   (candidateIndex + 1) == matchingContext.candidateLength && 
						   matchingContext.candidate[candidateIndex] == separator;
				}
			}
		}
		else {
			if (matchingContext.isMatchStartMatching && candidateIndex == matchingContext.candidateLength) {
				return true; // no more data but everything matched so far
			}
			return next.matches(candidateIndex, matchingContext);
		}
	}

	@Override
	public int getNormalizedLength() {
		return len;
	}

	public String toString() {
		return "Literal(" + new String(text) + ")";
	}

}