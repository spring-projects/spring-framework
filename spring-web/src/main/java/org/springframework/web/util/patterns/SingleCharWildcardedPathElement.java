/*
 * Copyright 2016 the original author or authors.
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
 * A literal path element that does includes the single character wildcard '?' one
 * or more times (to basically many any character at that position).
 * 
 * @author Andy Clement
 */
class SingleCharWildcardedPathElement extends PathElement {

	private char[] text;
	
	private int len;
	
	private int questionMarkCount;
	
	private boolean caseSensitive;

	public SingleCharWildcardedPathElement(int pos, char[] literalText, int questionMarkCount, boolean caseSensitive) {
		super(pos);
		this.len = literalText.length;
		this.questionMarkCount = questionMarkCount;
		this.caseSensitive = caseSensitive;
		if (caseSensitive) {
			this.text = literalText;
		} else {
			this.text = new char[literalText.length];
			for (int i = 0; i < len; i++) {
				this.text[i] = Character.toLowerCase(literalText[i]);
			}
		}
	}
	
	@Override
	public boolean matches(int candidateIndex, MatchingContext matchingContext) {
		if (matchingContext.candidateLength < (candidateIndex + len)) {
			return false; // There isn't enough data to match
		}
		char[] candidate = matchingContext.candidate;
		if (caseSensitive) {
			for (int i = 0; i < len; i++) {
				char t = text[i];
				if (t != '?' && candidate[candidateIndex] != t) {
					return false;
				}
				candidateIndex++;
			}
		} else {
			for (int i = 0; i < len; i++) {
				char t = text[i];
				if (t != '?' && Character.toLowerCase(candidate[candidateIndex]) != t) {
					return false;
				}
				candidateIndex++;
			}
		}
		if (next == null) {
			return candidateIndex == matchingContext.candidateLength;
		} else {
			if (matchingContext.isMatchStartMatching && candidateIndex == matchingContext.candidateLength) {
				return true; // no more data but matches up to this point
			}
			return next.matches(candidateIndex, matchingContext);
		}
	}
	
	@Override
	public int getWildcardCount() {
		return questionMarkCount;
	}

	public String toString() {
		return "SingleCharWildcarding(" + new String(text) + ")";
	}

	@Override
	public int getNormalizedLength() {
		return len;
	}

}