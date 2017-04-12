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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.patterns.PathPattern.MatchingContext;

/**
 * A regex path element. Used to represent any complicated element of the path.
 * For example in '<tt>/foo/&ast;_&ast;/&ast;_{foobar}</tt>' both <tt>*_*</tt> and <tt>*_{foobar}</tt>
 * are {@link RegexPathElement} path elements. Derived from the general {@link AntPathMatcher} approach.
 *
 * @author Andy Clement
 * @since 5.0
 */
class RegexPathElement extends PathElement {

	private final java.util.regex.Pattern GLOB_PATTERN = java.util.regex.Pattern
			.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

	private final String DEFAULT_VARIABLE_PATTERN = "(.*)";

	private final List<String> variableNames = new LinkedList<>();

	private char[] regex;

	private java.util.regex.Pattern pattern;

	private boolean caseSensitive;

	private int wildcardCount;

	RegexPathElement(int pos, char[] regex, boolean caseSensitive, char[] completePattern, char separator) {
		super(pos, separator);
		this.regex = regex;
		this.caseSensitive = caseSensitive;
		buildPattern(regex, completePattern);
	}

	public void buildPattern(char[] regex, char[] completePattern) {
		StringBuilder patternBuilder = new StringBuilder();
		String text = new String(regex);
		Matcher matcher = GLOB_PATTERN.matcher(text);
		int end = 0;
		while (matcher.find()) {
			patternBuilder.append(quote(text, end, matcher.start()));
			String match = matcher.group();
			if ("?".equals(match)) {
				patternBuilder.append('.');
			}
			else if ("*".equals(match)) {
				patternBuilder.append(".*");
				wildcardCount++;
			}
			else if (match.startsWith("{") && match.endsWith("}")) {
				int colonIdx = match.indexOf(':');
				if (colonIdx == -1) {
					patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
					String variableName = matcher.group(1);
					if (variableNames.contains(variableName)) {
						throw new PatternParseException(pos, completePattern, PatternMessage.ILLEGAL_DOUBLE_CAPTURE,
								variableName);
					}
					this.variableNames.add(variableName);
				}
				else {
					String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
					patternBuilder.append('(');
					patternBuilder.append(variablePattern);
					patternBuilder.append(')');
					String variableName = match.substring(1, colonIdx);
					if (variableNames.contains(variableName)) {
						throw new PatternParseException(pos, completePattern, PatternMessage.ILLEGAL_DOUBLE_CAPTURE,
								variableName);
					}
					this.variableNames.add(variableName);
				}
			}
			end = matcher.end();
		}
		patternBuilder.append(quote(text, end, text.length()));
		if (caseSensitive) {
			pattern = java.util.regex.Pattern.compile(patternBuilder.toString());
		}
		else {
			pattern = java.util.regex.Pattern.compile(patternBuilder.toString(),
					java.util.regex.Pattern.CASE_INSENSITIVE);
		}
	}

	public List<String> getVariableNames() {
		return variableNames;
	}

	private String quote(String s, int start, int end) {
		if (start == end) {
			return "";
		}
		return java.util.regex.Pattern.quote(s.substring(start, end));
	}

	@Override
	public boolean matches(int candidateIndex, MatchingContext matchingContext) {
		int p = matchingContext.scanAhead(candidateIndex);
		Matcher m = pattern.matcher(new SubSequence(matchingContext.candidate, candidateIndex, p));
		boolean matches = m.matches();
		if (matches) {
			if (next == null) {
				if (matchingContext.determineRemaining && 
					((this.variableNames.size() == 0) ? true : p > candidateIndex)) {
					matchingContext.remainingPathIndex = p;
					matches = true;
				}
				else {
					// No more pattern, is there more data?
					// If pattern is capturing variables there must be some actual data to bind to them
					matches = (p == matchingContext.candidateLength && 
							   ((this.variableNames.size() == 0) ? true : p > candidateIndex));
				}
			}
			else {
				if (matchingContext.isMatchStartMatching && p == matchingContext.candidateLength) {
					return true; // no more data but matches up to this point
				}
				matches = next.matches(p, matchingContext);
			}
		}
		if (matches && matchingContext.extractingVariables) {
			// Process captures
			if (this.variableNames.size() != m.groupCount()) { // SPR-8455
				throw new IllegalArgumentException("The number of capturing groups in the pattern segment "
						+ this.pattern + " does not match the number of URI template variables it defines, "
						+ "which can occur if capturing groups are used in a URI template regex. "
						+ "Use non-capturing groups instead.");
			}
			for (int i = 1; i <= m.groupCount(); i++) {
				String name = this.variableNames.get(i - 1);
				String value = m.group(i);
				matchingContext.set(name, value);
			}
		}
		return matches;
	}

	public String toString() {
		return "Regex(" + new String(regex) + ")";
	}

	@Override
	public int getNormalizedLength() {
		int varsLength = 0;
		for (String variableName : variableNames) {
			varsLength += variableName.length();
		}
		return regex.length - varsLength - variableNames.size();
	}

	public int getCaptureCount() {
		return variableNames.size();
	}

	@Override
	public int getWildcardCount() {
		return wildcardCount;
	}

	@Override
	public int getScore() {
		return getCaptureCount() * CAPTURE_VARIABLE_WEIGHT + getWildcardCount() * WILDCARD_WEIGHT;
	}

}