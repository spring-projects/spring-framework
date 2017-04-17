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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.PathMatcher;
import static org.springframework.util.StringUtils.hasLength;

/**
 * Represents a parsed path pattern. Includes a chain of path elements
 * for fast matching and accumulates computed state for quick comparison of
 * patterns.
 *
 * <p>PathPatterns match URL paths using the following rules:<br>
 * <ul>
 * <li>{@code ?} matches one character</li>
 * <li>{@code *} matches zero or more characters within a path segment</li>
 * <li>{@code **} matches zero or more <em>path segments</em> until the end of the path</li>
 * <li>{@code {spring}} matches a <em>path segment</em> and captures it as a variable named "spring"</li>
 * <li>{@code {spring:[a-z]+}} matches the regexp {@code [a-z]+} as a path variable named "spring"</li>
 * <li>{@code {*spring}} matches zero or more <em>path segments</em> until the end of the path
 * and captures it as a variable named "spring"</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <ul>
 * <li>{@code /pages/t?st.html} &mdash; matches {@code /pages/test.html} but also
 * {@code /pages/tast.html} but not {@code /pages/toast.html}</li>
 * <li>{@code /resources/*.png} &mdash; matches all {@code .png} files in the
 * {@code resources} directory</li>
 * <li><code>/resources/&#42;&#42;</code> &mdash; matches all files
 * underneath the {@code /resources/} path, including {@code /resources/image.png}
 * and {@code /resources/css/spring.css}</li>
 * <li><code>/resources/{&#42;path}</code> &mdash; matches all files
 * underneath the {@code /resources/} path and captures their relative path in
 * a variable named "path"; {@code /resources/image.png} will match with
 * "spring" -> "/image.png", and {@code /resources/css/spring.css} will match
 * with "spring" -> "/css/spring.css"</li>
 * <li>{@code /resources/{filename:\\w+}.dat} will match {@code /resources/spring.dat}
 * and assign the value {@code "spring"} to the {@code filename} variable</li>
 * </ul>
 *
 * @author Andy Clement
 * @since 5.0
 */
public class PathPattern implements Comparable<PathPattern> {

	private final static Map<String, String> NO_VARIABLES_MAP = Collections.emptyMap();

	/** First path element in the parsed chain of path elements for this pattern */
	private PathElement head;

	/** The text of the parsed pattern */
	private String patternString;

	/** The separator used when parsing the pattern */
	private char separator;

	/** Will this match candidates in a case sensitive way? (case sensitivity  at parse time) */
	private boolean caseSensitive;

	/** How many variables are captured in this pattern */
	private int capturedVariableCount;

	/**
	 * The normalized length is trying to measure the 'active' part of the pattern. It is computed
	 * by assuming all captured variables have a normalized length of 1. Effectively this means changing
	 * your variable name lengths isn't going to change the length of the active part of the pattern.
	 * Useful when comparing two patterns.
	 */
	int normalizedLength;

	/**
	 * Does the pattern end with '&lt;separator&gt;*' 
	 */
	boolean endsWithSeparatorWildcard = false;

	/**
	 * Score is used to quickly compare patterns. Different pattern components are given different
	 * weights. A 'lower score' is more specific. Current weights:
	 * <ul>
	 * <li>Captured variables are worth 1
	 * <li>Wildcard is worth 100
	 * </ul>
	 */
	private int score;

	/** Does the pattern end with {*...} */
	private boolean isCatchAll = false;

	public PathPattern(String patternText, PathElement head, char separator, boolean caseSensitive) {
		this.head = head;
		this.patternString = patternText;
		this.separator = separator;
		this.caseSensitive = caseSensitive;
		// Compute fields for fast comparison
		PathElement s = head;
		while (s != null) {
			this.capturedVariableCount += s.getCaptureCount();
			this.normalizedLength += s.getNormalizedLength();
			this.score += s.getScore();
			if (s instanceof CaptureTheRestPathElement || s instanceof WildcardTheRestPathElement) {
				this.isCatchAll = true;
			}
			if (s instanceof SeparatorPathElement && s.next != null
					&& s.next instanceof WildcardPathElement && s.next.next == null) {
				this.endsWithSeparatorWildcard = true;
			}
			s = s.next;
		}
	}

	/**
	 * @param path the candidate path to attempt to match against this pattern
	 * @return true if the path matches this pattern
	 */
	public boolean matches(String path) {
		if (head == null) {
			return !hasLength(path);
		}
		else if (!hasLength(path)) {
			if (head instanceof WildcardTheRestPathElement || head instanceof CaptureTheRestPathElement) {
				path = ""; // Will allow CaptureTheRest to bind the variable to empty
			}
			else {
				return false;
			}
		}
		MatchingContext matchingContext = new MatchingContext(path, false);
		return head.matches(0, matchingContext);
	}

	/**
	 * For a given path return the remaining piece that is not covered by this PathPattern.
	 * 
	 * @param path a path that may or may not match this path pattern
	 * @return the remaining path after as much has been consumed as possible by this pattern, 
	 * result can be the empty string if the path is entirely consumed or it will be null
	 * if the path does not match
	 */
	public String getPathRemaining(String path) {
		if (head == null) {
			if (path == null) {
				return path;
			}
			else {
				return hasLength(path)?path:"";				
			}
		}
		else if (!hasLength(path)) {
			return null;
		}
		MatchingContext matchingContext = new MatchingContext(path, false);
		matchingContext.setMatchAllowExtraPath();
		boolean matches = head.matches(0, matchingContext);
		if (!matches) {
			return null;
		}
		else {
			if (matchingContext.remainingPathIndex == path.length()) {
				return "";
			}
			else {
				return path.substring(matchingContext.remainingPathIndex);
			}
		}
	}

	/**
	 * @param path the path to check against the pattern
	 * @return true if the pattern matches as much of the path as is supplied
	 */
	public boolean matchStart(String path) {
		if (head == null) {
			return !hasLength(path);
		}
		else if (!hasLength(path)) {
			return true;
		}
		MatchingContext matchingContext = new MatchingContext(path, false);
		matchingContext.setMatchStartMatching(true);
		return head.matches(0, matchingContext);
	}

	/**
	 * @param path a path to match against this pattern
	 * @return a map of extracted variables - an empty map if no variables extracted. 
	 */
	public Map<String, String> matchAndExtract(String path) {
		MatchingContext matchingContext = new MatchingContext(path, true);
		if (head != null && head.matches(0, matchingContext)) {
			return matchingContext.getExtractedVariables();
		}
		else {
			if (!hasLength(path)) {
				return NO_VARIABLES_MAP;
			}
			else {
				throw new IllegalStateException("Pattern \"" + this.toString()
						+ "\" is not a match for \"" + path + "\"");
			}
		}
	}

	/**
	 * @return the original pattern string that was parsed to create this PathPattern
	 */
	public String getPatternString() {
		return patternString;
	}

	public PathElement getHeadSection() {
		return head;
	}

	/**
	 * Given a full path, determine the pattern-mapped part. <p>For example: <ul>
	 * <li>'{@code /docs/cvs/commit.html}' and '{@code /docs/cvs/commit.html} -> ''</li>
	 * <li>'{@code /docs/*}' and '{@code /docs/cvs/commit} -> '{@code cvs/commit}'</li>
	 * <li>'{@code /docs/cvs/*.html}' and '{@code /docs/cvs/commit.html} -> '{@code commit.html}'</li>
	 * <li>'{@code /docs/**}' and '{@code /docs/cvs/commit} -> '{@code cvs/commit}'</li>
	 * </ul>
	 * <p><b>Note:</b> Assumes that {@link #matches} returns {@code true} for '{@code pattern}' and '{@code path}', but
	 * does <strong>not</strong> enforce this. As per the contract on {@link PathMatcher}, this
	 * method will trim leading/trailing separators. It will also remove duplicate separators in
	 * the returned path.
	 * @param path a path that matches this pattern
	 * @return the subset of the path that is matched by pattern or "" if none of it is matched by pattern elements
	 */
	public String extractPathWithinPattern(String path) {
		// assert this.matches(path)
		PathElement s = head;
		int separatorCount = 0;
		// Find first path element that is pattern based
		while (s != null) {
			if (s instanceof SeparatorPathElement || s instanceof CaptureTheRestPathElement
					|| s instanceof WildcardTheRestPathElement) {
				separatorCount++;
			}
			if (s.getWildcardCount() != 0 || s.getCaptureCount() != 0) {
				break;
			}
			s = s.next;
		}
		if (s == null) {
			return ""; // There is no pattern mapped section
		}
		// Now separatorCount indicates how many sections of the path to skip
		char[] pathChars = path.toCharArray();
		int len = pathChars.length;
		int pos = 0;
		while (separatorCount > 0 && pos < len) {
			if (path.charAt(pos++) == separator) {
				// Skip any adjacent separators
				while (pos < len && path.charAt(pos) == separator) {
					pos++;
				}
				separatorCount--;
			}
		}
		int end = len;
		// Trim trailing separators
		while (end > 0 && path.charAt(end - 1) == separator) {
			end--;
		}
		// Check if multiple separators embedded in the resulting path, if so trim them out.
		// Example: aaa////bbb//ccc/d -> aaa/bbb/ccc/d
		// The stringWithDuplicateSeparatorsRemoved is only computed if necessary
		int c = pos;
		StringBuilder stringWithDuplicateSeparatorsRemoved = null;
		while (c < end) {
			char ch = path.charAt(c);
			if (ch == separator) {
				if ((c + 1) < end && path.charAt(c + 1) == separator) {
					// multiple separators
					if (stringWithDuplicateSeparatorsRemoved == null) {
						// first time seen, need to capture all data up to this point
						stringWithDuplicateSeparatorsRemoved = new StringBuilder();
						stringWithDuplicateSeparatorsRemoved.append(path.substring(pos, c));
					}
					do {
						c++;
					} while ((c + 1) < end && path.charAt(c + 1) == separator);
				}
			}
			if (stringWithDuplicateSeparatorsRemoved != null) {
				stringWithDuplicateSeparatorsRemoved.append(ch);
			}
			c++;
		}
		if (stringWithDuplicateSeparatorsRemoved != null) {
			return stringWithDuplicateSeparatorsRemoved.toString();
		}
		return pos == len ? "" : path.substring(pos, end);
	}

	/**
	 * Compare this pattern with a supplied pattern. Return -1,0,+1 if this pattern
	 * is more specific, the same or less specific than the supplied pattern.
	 * The aim is to sort more specific patterns first.
	 */
	@Override
	public int compareTo(PathPattern p) {
		// 1) null is sorted last
		if (p == null) {
			return -1;
		}
		// 2) catchall patterns are sorted last. If both catchall then the
		// length is considered
		if (isCatchAll()) {
			if (p.isCatchAll()) {
				int lenDifference = this.getNormalizedLength() - p.getNormalizedLength();
				if (lenDifference != 0) {
					return (lenDifference < 0) ? +1 : -1;
				}
			}
			else {
				return +1;
			}
		}
		else if (p.isCatchAll()) {
			return -1;
		}
		// 3) This will sort such that if they differ in terms of wildcards or
		// captured variable counts, the one with the most will be sorted last
		int score = this.getScore() - p.getScore();
		if (score != 0) {
			return (score < 0) ? -1 : +1;
		}
		// 4) longer is better
		int lenDifference = this.getNormalizedLength() - p.getNormalizedLength();
		return (lenDifference < 0) ? +1 : (lenDifference == 0 ? 0 : -1);
	}

	public int getScore() {
		return score;
	}

	public boolean isCatchAll() {
		return isCatchAll;
	}

	/**
	 * The normalized length is trying to measure the 'active' part of the pattern. It is computed
	 * by assuming all capture variables have a normalized length of 1. Effectively this means changing
	 * your variable name lengths isn't going to change the length of the active part of the pattern.
	 * Useful when comparing two patterns.
	 * @return the normalized length of the pattern
	 */
	public int getNormalizedLength() {
		return normalizedLength;
	}

	public boolean equals(Object o) {
		if (!(o instanceof PathPattern)) {
			return false;
		}
		PathPattern p = (PathPattern) o;
		return patternString.equals(p.getPatternString()) && separator == p.getSeparator()
				&& caseSensitive == p.caseSensitive;
	}

	public int hashCode() {
		return (patternString.hashCode() * 17 + separator) * 17 + (caseSensitive ? 1 : 0);
	}

	public String toChainString() {
		StringBuilder buf = new StringBuilder();
		PathElement pe = head;
		while (pe != null) {
			buf.append(pe.toString()).append(" ");
			pe = pe.next;
		}
		return buf.toString().trim();
	}

	public char getSeparator() {
		return separator;
	}

	public int getCapturedVariableCount() {
		return capturedVariableCount;
	}

	public String toString() {
		return patternString;
	}

	/**
	 * Encapsulates context when attempting a match. Includes some fixed state like the
	 * candidate currently being considered for a match but also some accumulators for
	 * extracted variables.
	 */
	class MatchingContext {

		// The candidate path to attempt a match against
		char[] candidate;

		// The length of the candidate path
		int candidateLength;

		boolean isMatchStartMatching = false;

		private Map<String, String> extractedVariables;

		boolean extractingVariables;
		
		boolean determineRemaining = false;
		
		// if determineRemaining is true, this is set to the position in
		// the candidate where the pattern finished matching - i.e. it
		// points to the remaining path that wasn't consumed
		int remainingPathIndex;

		public MatchingContext(String path, boolean extractVariables) {
			candidate = path.toCharArray();
			candidateLength = candidate.length;
			this.extractingVariables = extractVariables;
		}

		/**
		 * 
		 */
		public void setMatchAllowExtraPath() {
			determineRemaining = true;
		}

		public void setMatchStartMatching(boolean b) {
			isMatchStartMatching = b;
		}

		public void set(String key, String value) {
			if (this.extractedVariables == null) {
				extractedVariables = new HashMap<>();
			}
			extractedVariables.put(key, value);
		}

		public Map<String, String> getExtractedVariables() {
			if (this.extractedVariables == null) {
				return NO_VARIABLES_MAP;
			}
			else {
				return this.extractedVariables;
			}
		}

		/**
		 * Scan ahead from the specified position for either the next separator
		 * character or the end of the candidate.
		 *
		 * @param pos the starting position for the scan
		 * @return the position of the next separator or the end of the candidate
		 */
		public int scanAhead(int pos) {
			while (pos < candidateLength) {
				if (candidate[pos] == separator) {
					return pos;
				}
				pos++;
			}
			return candidateLength;
		}
	}

	/**
	 * Combine this pattern with another. Currently does not produce a new PathPattern, just produces a new string.
	 */
	public String combine(String pattern2string) {
		// If one of them is empty the result is the other. If both empty the result is ""
		if (!hasLength(patternString)) {
			if (!hasLength(pattern2string)) {
				return "";
			}
			else {
				return pattern2string;
			}
		}
		else if (!hasLength(pattern2string)) {
			return patternString;
		}

		// /* + /hotel => /hotel
		// /*.* + /*.html => /*.html
		// However:
		// /usr + /user => /usr/user 
		// /{foo} + /bar => /{foo}/bar
		if (!patternString.equals(pattern2string) && capturedVariableCount == 0 && matches(pattern2string)) {
			return pattern2string;
		}

		// /hotels/* + /booking => /hotels/booking
		// /hotels/* + booking => /hotels/booking
		if (endsWithSeparatorWildcard) {
			return concat(patternString.substring(0, patternString.length() - 2), pattern2string);
		}

		// /hotels + /booking => /hotels/booking
		// /hotels + booking => /hotels/booking
		int starDotPos1 = patternString.indexOf("*."); // Are there any file prefix/suffix things to consider?
		if (capturedVariableCount != 0 || starDotPos1 == -1 || separator == '.') {
			return concat(patternString, pattern2string);
		}

		// /*.html + /hotel => /hotel.html
		// /*.html + /hotel.* => /hotel.html
		String firstExtension = patternString.substring(starDotPos1 + 1); // looking for the first extension
		int dotPos2 = pattern2string.indexOf('.');
		String file2 = (dotPos2 == -1 ? pattern2string : pattern2string.substring(0, dotPos2));
		String secondExtension = (dotPos2 == -1 ? "" : pattern2string.substring(dotPos2));
		boolean firstExtensionWild = (firstExtension.equals(".*") || firstExtension.equals(""));
		boolean secondExtensionWild = (secondExtension.equals(".*") || secondExtension.equals(""));
		if (!firstExtensionWild && !secondExtensionWild) {
			throw new IllegalArgumentException("Cannot combine patterns: " + patternString + " and " + pattern2string);
		}
		return file2 + (firstExtensionWild ? secondExtension : firstExtension);
	}

	/**
	 * Join two paths together including a separator if necessary.
	 * Extraneous separators are removed (if the first path
	 * ends with one and the second path starts with one).
	 * @param path1 First path
	 * @param path2 Second path
	 * @return joined path that may include separator if necessary
	 */
	private String concat(String path1, String path2) {
		boolean path1EndsWithSeparator = path1.charAt(path1.length() - 1) == separator;
		boolean path2StartsWithSeparator = path2.charAt(0) == separator;
		if (path1EndsWithSeparator && path2StartsWithSeparator) {
			return path1 + path2.substring(1);
		}
		else if (path1EndsWithSeparator || path2StartsWithSeparator) {
			return path1 + path2;
		}
		else {
			return path1 + separator + path2;
		}
	}

}
