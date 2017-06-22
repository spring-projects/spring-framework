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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * A regex path element. Used to represent any complicated element of the path.
 * For example in '<tt>/foo/&ast;_&ast;/&ast;_{foobar}</tt>' both <tt>*_*</tt> and <tt>*_{foobar}</tt>
 * are {@link RegexPathElement} path elements. Derived from the general
 * {@link org.springframework.util.AntPathMatcher} approach.
 *
 * @author Andy Clement
 * @since 5.0
 */
class RegexPathElement extends PathElement {

	private final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

	private final String DEFAULT_VARIABLE_PATTERN = "(.*)";


	private char[] regex;

	private final boolean caseSensitive;

	private final Pattern pattern;

	private int wildcardCount;

	private final List<String> variableNames = new LinkedList<>();


	RegexPathElement(int pos, char[] regex, boolean caseSensitive, char[] completePattern, char separator) {
		super(pos, separator);
		this.regex = regex;
		this.caseSensitive = caseSensitive;
		this.pattern = buildPattern(regex, completePattern);
	}


	public Pattern buildPattern(char[] regex, char[] completePattern) {
		StringBuilder patternBuilder = new StringBuilder();
		String text = new String(regex);
		StringBuilder encodedRegexBuilder = new StringBuilder();
		Matcher matcher = GLOB_PATTERN.matcher(text);
		int end = 0;

		while (matcher.find()) {
			patternBuilder.append(quote(text, end, matcher.start(), encodedRegexBuilder));
			String match = matcher.group();
			if ("?".equals(match)) {
				patternBuilder.append('.');
				encodedRegexBuilder.append('?');
			}
			else if ("*".equals(match)) {
				patternBuilder.append(".*");
				encodedRegexBuilder.append('*');
				int pos = matcher.start();
				if (pos < 1 || text.charAt(pos-1) != '.') {
					// To be compatible with the AntPathMatcher comparator, 
					// '.*' is not considered a wildcard usage
					this.wildcardCount++;
				}
			}
			else if (match.startsWith("{") && match.endsWith("}")) {
				encodedRegexBuilder.append(match);
				int colonIdx = match.indexOf(':');
				if (colonIdx == -1) {
					patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
					String variableName = matcher.group(1);
					if (this.variableNames.contains(variableName)) {
						throw new PatternParseException(this.pos, completePattern,
								PatternParseException.PatternMessage.ILLEGAL_DOUBLE_CAPTURE, variableName);
					}
					this.variableNames.add(variableName);
				}
				else {
					String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
					patternBuilder.append('(');
					patternBuilder.append(variablePattern);
					patternBuilder.append(')');
					String variableName = match.substring(1, colonIdx);
					if (this.variableNames.contains(variableName)) {
						throw new PatternParseException(this.pos, completePattern,
								PatternParseException.PatternMessage.ILLEGAL_DOUBLE_CAPTURE, variableName);
					}
					this.variableNames.add(variableName);
				}
			}
			end = matcher.end();
		}

		patternBuilder.append(quote(text, end, text.length(), encodedRegexBuilder));
		this.regex = encodedRegexBuilder.toString().toCharArray();
		if (this.caseSensitive) {
			return Pattern.compile(patternBuilder.toString());
		}
		else {
			return Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
		}
	}

	public List<String> getVariableNames() {
		return this.variableNames;
	}

	private String quote(String s, int start, int end, StringBuilder encodedRegexBuilder) {
		if (start == end) {
			return "";
		}
		String substring = s.substring(start, end);
		String encodedSubString = UriUtils.encodePath(substring, StandardCharsets.UTF_8);
		encodedRegexBuilder.append(encodedSubString);
		return Pattern.quote(substring);
	}

	@Override
	public boolean matches(int candidateIndex, MatchingContext matchingContext) {
		int pos = matchingContext.scanAhead(candidateIndex);
		
		CharSequence textToMatch = null;
		if (includesPercent(matchingContext.candidate, candidateIndex, pos)) {
			textToMatch = decode(new SubSequence(matchingContext.candidate, candidateIndex, pos));
		}
		else {
			textToMatch = new SubSequence(matchingContext.candidate, candidateIndex, pos);
		}
		Matcher matcher = this.pattern.matcher(textToMatch);
		boolean matches = matcher.matches();

		if (matches) {
			if (this.next == null) {
				if (matchingContext.determineRemainingPath && 
					((this.variableNames.size() == 0) ? true : pos > candidateIndex)) {
					matchingContext.remainingPathIndex = pos;
					matches = true;
				}
				else {
					// No more pattern, is there more data?
					// If pattern is capturing variables there must be some actual data to bind to them
					matches = (pos == matchingContext.candidateLength &&
							   ((this.variableNames.size() == 0) ? true : pos > candidateIndex));
					if (!matches && matchingContext.isAllowOptionalTrailingSlash()) {
						matches = ((this.variableNames.size() == 0) ? true : pos > candidateIndex) &&
							      (pos + 1) == matchingContext.candidateLength &&
							      matchingContext.candidate[pos] == separator;
					}
				}
			}
			else {
				if (matchingContext.isMatchStartMatching && pos == matchingContext.candidateLength) {
					return true; // no more data but matches up to this point
				}
				matches = this.next.matches(pos, matchingContext);
			}
		}

		if (matches && matchingContext.extractingVariables) {
			// Process captures
			if (this.variableNames.size() != matcher.groupCount()) { // SPR-8455
				throw new IllegalArgumentException("The number of capturing groups in the pattern segment "
						+ this.pattern + " does not match the number of URI template variables it defines, "
						+ "which can occur if capturing groups are used in a URI template regex. "
						+ "Use non-capturing groups instead.");
			}
			for (int i = 1; i <= matcher.groupCount(); i++) {
				String name = this.variableNames.get(i - 1);
				String value = matcher.group(i);
				matchingContext.set(name, value);
			}
		}
		return matches;
	}

	@Override
	public int getNormalizedLength() {
		int varsLength = 0;
		for (String variableName : this.variableNames) {
			varsLength += variableName.length();
		}
		return (this.regex.length - varsLength - this.variableNames.size());
	}

	public int getCaptureCount() {
		return this.variableNames.size();
	}

	@Override
	public int getWildcardCount() {
		return this.wildcardCount;
	}

	@Override
	public int getScore() {
		return (getCaptureCount() * CAPTURE_VARIABLE_WEIGHT + getWildcardCount() * WILDCARD_WEIGHT);
	}


	public String toString() {
		return "Regex(" + String.valueOf(this.regex) + ")";
	}

}
