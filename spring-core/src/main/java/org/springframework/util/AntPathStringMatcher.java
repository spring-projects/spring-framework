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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Package-protected helper class for {@link AntPathMatcher}. Tests whether or not a string matches against a pattern
 * via a {@link Pattern}.
 *
 * <p>The pattern may contain special characters: '*' means zero or more characters; '?' means one and only one
 * character; '{' and '}' indicate a URI template pattern. For example <tt>/users/{user}</tt>.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
class AntPathStringMatcher {

	private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

	private static final String DEFAULT_VARIABLE_PATTERN = "(.*)";

	private final Pattern pattern;

	private String str;

	private final List<String> variableNames = new LinkedList<String>();

	private final Map<String, String> uriTemplateVariables;

	/** Construct a new instance of the <code>AntPatchStringMatcher</code>. */
	AntPathStringMatcher(String pattern, String str, Map<String, String> uriTemplateVariables) {
		this.str = str;
		this.uriTemplateVariables = uriTemplateVariables;
		this.pattern = createPattern(pattern);
	}

	private Pattern createPattern(String pattern) {
		StringBuilder patternBuilder = new StringBuilder();
		Matcher m = GLOB_PATTERN.matcher(pattern);
		int end = 0;
		while (m.find()) {
			patternBuilder.append(quote(pattern, end, m.start()));
			String match = m.group();
			if ("?".equals(match)) {
				patternBuilder.append('.');
			}
			else if ("*".equals(match)) {
				patternBuilder.append(".*");
			}
			else if (match.startsWith("{") && match.endsWith("}")) {
				int colonIdx = match.indexOf(':');
				if (colonIdx == -1) {
					patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
					variableNames.add(m.group(1));
				}
				else {
					String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
					patternBuilder.append('(');
					patternBuilder.append(variablePattern);
					patternBuilder.append(')');
					String variableName = match.substring(1, colonIdx);
					variableNames.add(variableName);
				}
			}
			end = m.end();
		}
		patternBuilder.append(quote(pattern, end, pattern.length()));
		return Pattern.compile(patternBuilder.toString());
	}

	private String quote(String s, int start, int end) {
		if (start == end) {
			return "";
		}
		return Pattern.quote(s.substring(start, end));
	}

	/**
	 * Main entry point.
	 *
	 * @return <code>true</code> if the string matches against the pattern, or <code>false</code> otherwise.
	 */
	public boolean matchStrings() {
		Matcher matcher = pattern.matcher(str);
		if (matcher.matches()) {
			if (uriTemplateVariables != null) {
				// SPR-8455
				Assert.isTrue(variableNames.size() == matcher.groupCount(), 
						"The number of capturing groups in the pattern segment " + pattern + 
						" does not match the number of URI template variables it defines, which can occur if " + 
						" capturing groups are used in a URI template regex. Use non-capturing groups instead.");
				for (int i = 1; i <= matcher.groupCount(); i++) {
					String name = this.variableNames.get(i - 1);
					String value = matcher.group(i);
					uriTemplateVariables.put(name, value);
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

}
