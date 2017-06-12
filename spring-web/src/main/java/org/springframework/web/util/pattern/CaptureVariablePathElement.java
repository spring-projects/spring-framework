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

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.util.UriUtils;

/**
 * A path element representing capturing a piece of the path as a variable. In the pattern
 * '/foo/{bar}/goo' the {bar} is represented as a {@link CaptureVariablePathElement}. There
 * must be at least one character to bind to the variable.
 *
 * @author Andy Clement
 * @since 5.0
 */
class CaptureVariablePathElement extends PathElement {

	private final String variableName;

	private Pattern constraintPattern;


	/**
	 * @param pos the position in the pattern of this capture element
	 * @param captureDescriptor is of the form {AAAAA[:pattern]}
	 */
	CaptureVariablePathElement(int pos, char[] captureDescriptor, boolean caseSensitive, char separator) {
		super(pos, separator);
		int colon = -1;
		for (int i = 0; i < captureDescriptor.length; i++) {
			if (captureDescriptor[i] == ':') {
				colon = i;
				break;
			}
		}
		if (colon == -1) {
			// no constraint
			this.variableName = new String(captureDescriptor, 1, captureDescriptor.length - 2);
		}
		else {
			this.variableName = new String(captureDescriptor, 1, colon - 1);
			if (caseSensitive) {
				this.constraintPattern = Pattern.compile(
						new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2));
			}
			else {
				this.constraintPattern = Pattern.compile(
						new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2),
						Pattern.CASE_INSENSITIVE);
			}
		}
	}


	@Override
	public boolean matches(int candidateIndex, PathPattern.MatchingContext matchingContext) {
		int nextPos = matchingContext.scanAhead(candidateIndex);
		// There must be at least one character to capture:
		if (nextPos == candidateIndex) {
			return false;
		}

		String substringForDecoding = null;
		CharSequence candidateCapture = null;
		if (this.constraintPattern != null) {
			// TODO possible optimization - only regex match if rest of pattern matches? Benefit likely to vary pattern to pattern
			if (includesPercent(matchingContext.candidate, candidateIndex, nextPos)) {
				substringForDecoding = new String(matchingContext.candidate, candidateIndex, nextPos);
				candidateCapture = UriUtils.decode(substringForDecoding, StandardCharsets.UTF_8);
			}
			else {
				candidateCapture = new SubSequence(matchingContext.candidate, candidateIndex, nextPos);
			}
			Matcher matcher = constraintPattern.matcher(candidateCapture);
			if (matcher.groupCount() != 0) {
				throw new IllegalArgumentException(
						"No capture groups allowed in the constraint regex: " + this.constraintPattern.pattern());
			}
			if (!matcher.matches()) {
				return false;
			}
		}

		boolean match = false;
		if (this.next == null) {
			if (matchingContext.determineRemainingPath && nextPos > candidateIndex) {
				matchingContext.remainingPathIndex = nextPos;
				match = true;
			}
			else {
				// Needs to be at least one character #SPR15264
				match = (nextPos == matchingContext.candidateLength && nextPos > candidateIndex);
				if (!match && matchingContext.isAllowOptionalTrailingSlash()) {
					match = (nextPos > candidateIndex) &&
						    (nextPos + 1) == matchingContext.candidateLength && 
						     matchingContext.candidate[nextPos] == separator;
				}
			}
		}
		else {
			if (matchingContext.isMatchStartMatching && nextPos == matchingContext.candidateLength) {
				match = true;  // no more data but matches up to this point
			}
			else {
				match = this.next.matches(nextPos, matchingContext);
			}
		}

		if (match && matchingContext.extractingVariables) {
			matchingContext.set(this.variableName,
					candidateCapture != null ? candidateCapture.toString():
					decode(new String(matchingContext.candidate, candidateIndex, nextPos - candidateIndex)));
		}
		return match;
	}

	public String getVariableName() {
		return this.variableName;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public int getWildcardCount() {
		return 0;
	}

	@Override
	public int getCaptureCount() {
		return 1;
	}

	@Override
	public int getScore() {
		return CAPTURE_VARIABLE_WEIGHT;
	}


	public String toString() {
		return "CaptureVariable({" + this.variableName +
				(this.constraintPattern != null ? ":" + this.constraintPattern.pattern() : "") + "})";
	}

}
