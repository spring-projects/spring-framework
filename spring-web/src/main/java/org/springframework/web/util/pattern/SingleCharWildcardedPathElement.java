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

import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * A literal path element that does includes the single character wildcard '?' one
 * or more times (to basically many any character at that position).
 *
 * @author Andy Clement
 * @since 5.0
 */
class SingleCharWildcardedPathElement extends PathElement {

	private final char[] text;

	private final int len;

	private final int questionMarkCount;

	private final boolean caseSensitive;


	public SingleCharWildcardedPathElement(
			int pos, char[] literalText, int questionMarkCount, boolean caseSensitive, char separator) {

		super(pos, separator);
		this.len = literalText.length;
		this.questionMarkCount = questionMarkCount;
		this.caseSensitive = caseSensitive;
		if (caseSensitive) {
			this.text = literalText;
		}
		else {
			this.text = new char[literalText.length];
			for (int i = 0; i < len; i++) {
				this.text[i] = Character.toLowerCase(literalText[i]);
			}
		}
	}


	@Override
	public boolean matches(int candidateIndex, MatchingContext matchingContext) {
		if (matchingContext.candidateLength < (candidateIndex + len)) {
			return false;  // there isn't enough data to match
		}

		char[] candidate = matchingContext.candidate;
		if (this.caseSensitive) {
			for (int i = 0; i <this.len; i++) {
				char t = this.text[i];
				if (t == '?') {
					if (candidate[candidateIndex] == '%') {
						// encoded value, skip next two as well!
						candidateIndex += 2;
					}
				}
				else if (candidate[candidateIndex] != t) {
					// TODO unfortunate performance hit here on comparison when encoded data is the less likely case
					if (i < 3 || matchingContext.candidate[candidateIndex-2] != '%' ||
							Character.toUpperCase(matchingContext.candidate[candidateIndex]) != this.text[i]) {
						return false;
					}
				}
				candidateIndex++;
			}
		}
		else {
			for (int i = 0; i < this.len; i++) {
				char t = this.text[i];
				if (t == '?') {
					if (candidate[candidateIndex] == '%') {
						// encoded value, skip next two as well!
						candidateIndex += 2;
					}
				}
				else if (Character.toLowerCase(candidate[candidateIndex]) != t) {
					return false;
				}
				candidateIndex++;
			}
		}

		if (this.next == null) {
			if (matchingContext.determineRemainingPath && nextIfExistsIsSeparator(candidateIndex, matchingContext)) {
				matchingContext.remainingPathIndex = candidateIndex;
				return true;
			}
			else {
				if (candidateIndex == matchingContext.candidateLength) {
					return true;
				}
				else {
					return (matchingContext.isAllowOptionalTrailingSlash() &&
							(candidateIndex + 1) == matchingContext.candidateLength &&
							matchingContext.candidate[candidateIndex] == separator);
				}
			}
		}
		else {
			if (matchingContext.isMatchStartMatching && candidateIndex == matchingContext.candidateLength) {
				return true;  // no more data but matches up to this point
			}
			return this.next.matches(candidateIndex, matchingContext);
		}
	}

	@Override
	public int getWildcardCount() {
		return this.questionMarkCount;
	}

	@Override
	public int getNormalizedLength() {
		return len;
	}


	public String toString() {
		return "SingleCharWildcarded(" + String.valueOf(this.text) + ")";
	}

}
