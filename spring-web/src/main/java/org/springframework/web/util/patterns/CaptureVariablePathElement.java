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

import java.util.regex.Matcher;

import org.springframework.web.util.patterns.PathPattern.MatchingContext;

/**
 * A path element representing capturing a piece of the path as a variable. In the pattern
 * '/foo/{bar}/goo' the {bar} is represented as a {@link CaptureVariablePathElement}.
 *
 * @author Andy Clement
 */
class CaptureVariablePathElement extends PathElement {

	private String variableName;

	private java.util.regex.Pattern constraintPattern;

	/**
	 * @param pos the position in the pattern of this capture element
	 * @param captureDescriptor is of the form {AAAAA[:pattern]}
	 */
	CaptureVariablePathElement(int pos, char[] captureDescriptor, boolean caseSensitive) {
		super(pos);
		int colon = -1;
		for (int i = 0; i < captureDescriptor.length; i++) {
			if (captureDescriptor[i] == ':') {
				colon = i;
				break;
			}
		}
		if (colon == -1) {
			// no constraint
			variableName = new String(captureDescriptor, 1, captureDescriptor.length - 2);
		}
		else {
			variableName = new String(captureDescriptor, 1, colon - 1);
			if (caseSensitive) {
				constraintPattern = java.util.regex.Pattern
						.compile(new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2));
			}
			else {
				constraintPattern = java.util.regex.Pattern.compile(
						new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2),
						java.util.regex.Pattern.CASE_INSENSITIVE);
			}
		}
	}

	@Override
	public boolean matches(int candidateIndex, MatchingContext matchingContext) {
		int nextPos = matchingContext.scanAhead(candidateIndex);
		CharSequence candidateCapture = null;
		if (constraintPattern != null) {
			// TODO possible optimization - only regex match if rest of pattern matches? Benefit likely to vary pattern to pattern
			candidateCapture = new SubSequence(matchingContext.candidate, candidateIndex, nextPos);
			Matcher m = constraintPattern.matcher(candidateCapture);
			if (m.groupCount() != 0) {
				throw new IllegalArgumentException("No capture groups allowed in the constraint regex: " + constraintPattern.pattern());
			}
			if (!m.matches()) {
				return false;
			}
		}
		boolean match = false;
		if (next == null) {
			// Needs to be at least one character #SPR15264
			match = (nextPos == matchingContext.candidateLength && nextPos > candidateIndex);
		}
		else {
			if (matchingContext.isMatchStartMatching && nextPos == matchingContext.candidateLength) {
				match = true; // no more data but matches up to this point
			}
			else {
				match = next.matches(nextPos, matchingContext);
			}
		}
		if (match && matchingContext.extractingVariables) {
			matchingContext.set(variableName, new String(matchingContext.candidate, candidateIndex, nextPos - candidateIndex));
		}
		return match;
	}

	public String getVariableName() {
		return this.variableName;
	}

	public String toString() {
		return "CaptureVariable({" + variableName + (constraintPattern == null ? "" : ":" + constraintPattern.pattern()) + "})";
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
}