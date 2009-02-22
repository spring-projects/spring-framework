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

package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Represents a URI template. An URI template is a URI-like string that contained variables marked of in braces
 * (<code>{</code>, <code>}</code>), which can be expanded to produce a URI.
 * <p/>
 * See {@link #expand(Map)}, {@link #expand(String[])}, and {@link #match(String)} for example usages.
 *
 * @author Arjen Poutsma
 * @see <a href="http://bitworking.org/projects/URI-Templates/">URI Templates</a>
 */
public final class UriTemplate {

	/**
	 * Captures URI template variable names.
	 */
	private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

	/**
	 * Replaces template variables in the URI template.
	 */
	private static final String VALUE_REGEX = "(.*)";

	private final List<String> variableNames;

	private final Pattern matchPattern;

	private final String uriTemplate;

	/**
	 * Constructs a new {@link UriTemplate} with the given string.
	 *
	 * @param uriTemplate the uri template string
	 */
	public UriTemplate(String uriTemplate) {
		Parser parser = new Parser(uriTemplate);
		this.uriTemplate = uriTemplate;
		this.variableNames = parser.getVariableNames();
		this.matchPattern = parser.getMatchPattern();
	}

	/**
	 * Returns the names of the variables in the template, in order.
	 *
	 * @return the template variable names
	 */
	public List<String> getVariableNames() {
		return variableNames;
	}

	/**
	 * Given the map of variables, expands this template into a URI string. The map keys represent variable names, the
	 * map values variable values. The order of variables is not significant.
	 * <p/>
	 * Example:
	 * <pre>
	 * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
	 * Map&lt;String, String&gt; uriVariables = new HashMap&lt;String, String&gt;();
	 * uriVariables.put("booking", "42");
	 * uriVariables.put("hotel", "1");
	 * System.out.println(template.expand(uriVariables));
	 * </pre>
	 * will print: <blockquote><code>http://example.com/hotels/1/bookings/42</code></blockquote>
	 *
	 * @param uriVariables the map of uri variables
	 * @return the expanded uri
	 * @throws IllegalArgumentException if <code>uriVariables</code> is <code>null</code>; or if it does not contain
	 *                                  values for all the variable names
	 */
	public URI expand(Map<String, String> uriVariables) {
		Assert.notNull(uriVariables, "'uriVariables' must not be null");
		String[] values = new String[variableNames.size()];
		for (int i = 0; i < variableNames.size(); i++) {
			String name = variableNames.get(i);
			Assert.isTrue(uriVariables.containsKey(name), "'uriVariables' has no value for [" + name + "]");
			values[i] = uriVariables.get(name);
		}
		return expand(values);
	}

	/**
	 * Given an array of variables, expands this template into a URI string. The array represent variable values. The
	 * order of variables is significant.
	 * <p/>
	 * Example:
	 * <pre>
	 * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
	 * System.out.println(template.expand("1", "42));
	 * </pre>
	 * will print: <blockquote><code>http://example.com/hotels/1/bookings/42</code></blockquote>
	 *
	 * @param uriVariableValues the array of uri variables
	 * @return the expanded uri
	 * @throws IllegalArgumentException if <code>uriVariables</code> is <code>null</code>; or if it does not contain
	 *                                  sufficient variables
	 */
	public URI expand(String... uriVariableValues) {
		Assert.notNull(uriVariableValues, "'uriVariableValues' must not be null");
		if (uriVariableValues.length != variableNames.size()) {
			throw new IllegalArgumentException(
					"Invalid amount of variables values in [" + uriTemplate + "]: expected " + variableNames.size() +
							"; got " + uriVariableValues.length);
		}
		Matcher m = NAMES_PATTERN.matcher(uriTemplate);
		StringBuffer buffer = new StringBuffer();
		int i = 0;
		while (m.find()) {
			String uriVariable = uriVariableValues[i++];
			m.appendReplacement(buffer, uriVariable);
		}
		m.appendTail(buffer);
		return URI.create(buffer.toString());
	}

	/**
	 * Indicates whether the given URI matches this template.
	 *
	 * @param uri the URI to match to
	 * @return <code>true</code> if it matches; <code>false</code> otherwise
	 */
	public boolean matches(String uri) {
		if (uri == null) {
			return false;
		}
		Matcher m = matchPattern.matcher(uri);
		return m.matches();
	}

	/**
	 * Matches the given URI to a map of variable values. Keys in the returned map are variable names, values are
	 * variable values, as occurred in the given URI.
	 * <p/>
	 * Example:
	 * <pre>
	 * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
	 * System.out.println(template.match("http://example.com/hotels/1/bookings/42"));
	 * </pre>
	 * will print: <blockquote><code>{hotel=1, booking=42}</code></blockquote>
	 *
	 * @param uri the URI to match to
	 * @return a map of variable values
	 */
	public Map<String, String> match(String uri) {
		Assert.notNull(uri, "'uri' must not be null");
		Map<String, String> result = new LinkedHashMap<String, String>(variableNames.size());
		Matcher m = matchPattern.matcher(uri);
		if (m.find()) {
			for (int i = 1; i <= m.groupCount(); i++) {
				String name = variableNames.get(i - 1);
				String value = m.group(i);
				result.put(name, value);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return uriTemplate;
	}

	/**
	 * Static inner class to parse uri template strings into a matching regular expression.
	 */
	private static class Parser {

		private List<String> variableNames = new LinkedList<String>();

		private StringBuilder patternBuilder = new StringBuilder();

		private Parser(String uriTemplate) {
			Assert.hasText(uriTemplate, "'uriTemplate' must not be null");
			Matcher m = NAMES_PATTERN.matcher(uriTemplate);
			int end = 0;
			while (m.find()) {
				patternBuilder.append(encodeAndQuote(uriTemplate, end, m.start()));
				patternBuilder.append(VALUE_REGEX);
				variableNames.add(m.group(1));
				end = m.end();
			}
			patternBuilder.append(encodeAndQuote(uriTemplate, end, uriTemplate.length()));

			int lastIdx = patternBuilder.length() - 1;
			if (lastIdx >= 0 && patternBuilder.charAt(lastIdx) == '/') {
				patternBuilder.deleteCharAt(lastIdx);
			}
		}

		private String encodeAndQuote(String fullPath, int start, int end) {
			if (start == end) {
				return "";
			}
			String result = fullPath.substring(start, end);
			try {
				URI uri = new URI(null, null, result, null);
				result = uri.toASCIIString();
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("Could not create URI from [" + fullPath + "]");
			}
			return Pattern.quote(result);
		}

		private List<String> getVariableNames() {
			return Collections.unmodifiableList(variableNames);
		}

		private Pattern getMatchPattern() {
			return Pattern.compile(patternBuilder.toString());
		}

	}

}
