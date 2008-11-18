package org.springframework.util;

import java.util.Map;

/**
 * Package-protected helper class for {@link AntPathMatcher}. Tests whether or not a string matches against a pattern.
 * The pattern may contain special characters:<br> '*' means zero or more characters<br> '?' means one and only one
 * character, '{' and '}' indicate a uri template pattern
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
	 * Constructs a new instance of the <code>AntPatchStringMatcher</code>.
	 * @param pattern
	 * @param str
	 * @param uriTemplateVariables
	 */
	AntPatchStringMatcher(String pattern, String str, Map<String, String> uriTemplateVariables) {
		patArr = pattern.toCharArray();
		strArr = str.toCharArray();
		this.uriTemplateVariables = uriTemplateVariables;
		patIdxEnd = patArr.length - 1;
		strIdxEnd = strArr.length - 1;
	}

	private void addTemplateVariable(String varName, String varValue) {
		if (uriTemplateVariables != null) {
			uriTemplateVariables.put(varName, varValue);
		}
	}

	private void addTemplateVariable(int curlyIdxStart, int curlyIdxEnd, int valIdxStart, int valIdxEnd) {
		if (uriTemplateVariables != null) {
			String varName = new String(patArr, curlyIdxStart + 1, curlyIdxEnd - curlyIdxStart - 1);
			String varValue = new String(strArr, valIdxStart, valIdxEnd - valIdxStart + 1);
			uriTemplateVariables.put(varName, varValue);
		}
	}

	boolean matchStrings() {
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
			int patIdxTmp = findNextStar();
			if (patIdxTmp == patIdxStart + 1 && patArr[patIdxTmp] == '*') {
				// Two stars next to each other, skip the first one.
				patIdxStart++;
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

	private int findNextStar() {
		for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
			if (patArr[i] == '*') {
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
