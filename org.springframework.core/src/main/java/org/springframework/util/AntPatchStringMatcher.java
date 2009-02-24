/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.util;

import java.util.Map;

/**
 * Package-protected helper class for {@link AntPathMatcher}.
 * Tests whether or not a string matches against a pattern.
 *
 * <p>The pattern may contain special characters: '*' means zero or more characters;
 * '?' means one and only one character; '{' and '}' indicate a URI template pattern.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
class AntPatchStringMatcher {

	private final char[] patArr;

	private final char[] strArr;

	private int patIdxStart = 0;

	private int patIdxEnd;

	private int strIdxStart = 0;

	private int strIdxEnd;

	private char ch;

	private final Map<String, String> uriTemplateVariables;


	/**
	 * Construct a new instance of the <code>AntPatchStringMatcher</code>.
	 */
	public AntPatchStringMatcher(String pattern, String str, Map<String, String> uriTemplateVariables) {
		this.patArr = pattern.toCharArray();
		this.strArr = str.toCharArray();
		this.patIdxEnd = this.patArr.length - 1;
		this.strIdxEnd = this.strArr.length - 1;
		this.uriTemplateVariables = uriTemplateVariables;
	}


	/**
	 * Main entry point.
	 * @return <code>true</code> if the string matches against the pattern, or <code>false</code> otherwise.
	 */
	public boolean matchStrings() {
		if (shortcutPossible()) {
			return doShortcut();
		}
		if (patternContainsOnlyStar()) {
			return true;
		}
		if (patternContainsOneTemplateVariable()) {
			addTemplateVariable(0, patIdxEnd, 0, strIdxEnd);
			return true;
		}
		if (!matchBeforeFirstStarOrCurly()) {
			return false;
		}
		if (allCharsUsed()) {
			return onlyStarsLeft();
		}
		if (!matchAfterLastStarOrCurly()) {
			return false;
		}
		if (allCharsUsed()) {
			return onlyStarsLeft();
		}
		// process pattern between stars. padIdxStart and patIdxEnd point
		// always to a '*'.
		while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
			int patIdxTmp;
			if (patArr[patIdxStart] == '{') {
				patIdxTmp = findClosingCurly();
				addTemplateVariable(patIdxStart, patIdxTmp, strIdxStart, strIdxEnd);
				patIdxStart = patIdxTmp + 1;
				strIdxStart = strIdxEnd + 1;
				continue;
			}
			patIdxTmp = findNextStarOrCurly();
			if (consecutiveStars(patIdxTmp)) {
				continue;
			}
			// Find the pattern between padIdxStart & padIdxTmp in str between
			// strIdxStart & strIdxEnd
			int patLength = (patIdxTmp - patIdxStart - 1);
			int strLength = (strIdxEnd - strIdxStart + 1);
			int foundIdx = -1;
			strLoop:
			for (int i = 0; i <= strLength - patLength; i++) {
				for (int j = 0; j < patLength; j++) {
					ch = patArr[patIdxStart + j + 1];
					if (ch != '?') {
						if (ch != strArr[strIdxStart + i + j]) {
							continue strLoop;
						}
					}
				}

				foundIdx = strIdxStart + i;
				break;
			}

			if (foundIdx == -1) {
				return false;
			}

			patIdxStart = patIdxTmp;
			strIdxStart = foundIdx + patLength;
		}

		return onlyStarsLeft();
	}

	private void addTemplateVariable(int curlyIdxStart, int curlyIdxEnd, int valIdxStart, int valIdxEnd) {
		if (uriTemplateVariables != null) {
			String varName = new String(patArr, curlyIdxStart + 1, curlyIdxEnd - curlyIdxStart - 1);
			String varValue = new String(strArr, valIdxStart, valIdxEnd - valIdxStart + 1);
			uriTemplateVariables.put(varName, varValue);
		}
	}

	private boolean consecutiveStars(int patIdxTmp) {
		if (patIdxTmp == patIdxStart + 1 && patArr[patIdxStart] == '*' && patArr[patIdxTmp] == '*') {
			// Two stars next to each other, skip the first one.
			patIdxStart++;
			return true;
		}
		return false;
	}

	private int findNextStarOrCurly() {
		for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
			if (patArr[i] == '*' || patArr[i] == '{') {
				return i;
			}
		}
		return -1;
	}

	private int findClosingCurly() {
		for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
			if (patArr[i] == '}') {
				return i;
			}
		}
		return -1;
	}

	private boolean onlyStarsLeft() {
		for (int i = patIdxStart; i <= patIdxEnd; i++) {
			if (patArr[i] != '*') {
				return false;
			}
		}
		return true;
	}

	private boolean allCharsUsed() {
		return strIdxStart > strIdxEnd;
	}

	private boolean shortcutPossible() {
		for (char ch : patArr) {
			if (ch == '*' || ch == '{' || ch == '}') {
				return false;
			}
		}
		return true;
	}

	private boolean doShortcut() {
		if (patIdxEnd != strIdxEnd) {
			return false; // Pattern and string do not have the same size
		}
		for (int i = 0; i <= patIdxEnd; i++) {
			ch = patArr[i];
			if (ch != '?') {
				if (ch != strArr[i]) {
					return false;// Character mismatch
				}
			}
		}
		return true; // String matches against pattern
	}

	private boolean patternContainsOnlyStar() {
		return (patIdxEnd == 0 && patArr[0] == '*');
	}

	private boolean patternContainsOneTemplateVariable() {
		if ((patIdxEnd >= 2 && patArr[0] == '{' && patArr[patIdxEnd] == '}')) {
			for (int i = 1; i < patIdxEnd; i++) {
				if (patArr[i] == '}') {
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	private boolean matchBeforeFirstStarOrCurly() {
		while ((ch = patArr[patIdxStart]) != '*' && ch != '{' && strIdxStart <= strIdxEnd) {
			if (ch != '?') {
				if (ch != strArr[strIdxStart]) {
					return false;
				}
			}
			patIdxStart++;
			strIdxStart++;
		}
		return true;
	}

	private boolean matchAfterLastStarOrCurly() {
		while ((ch = patArr[patIdxEnd]) != '*' && ch != '}' && strIdxStart <= strIdxEnd) {
			if (ch != '?') {
				if (ch != strArr[strIdxEnd]) {
					return false;
				}
			}
			patIdxEnd--;
			strIdxEnd--;
		}
		return true;
	}

}
