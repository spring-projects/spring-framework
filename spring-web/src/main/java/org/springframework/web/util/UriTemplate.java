/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Represents a URI template. A URI template is a URI-like String that contains variables
 * enclosed by braces ({@code {}}), which can be expanded to produce an actual URI.
 *
 * <p>See {@link #expand(Map)}, {@link #expand(Object[])}, and {@link #match(String)}
 * for example usages.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @see <a href="http://bitworking.org/projects/URI-Templates/">URI Templates</a>
 */
@SuppressWarnings("serial")
public class UriTemplate implements Serializable {

	private final UriComponents uriComponents;

	private final List<String> variableNames;

	private final Pattern matchPattern;

	private final String uriTemplate;


	/**
	 * Construct a new {@code UriTemplate} with the given URI String.
	 * @param uriTemplate the URI template string
	 */
	public UriTemplate(String uriTemplate) {
		Assert.hasText(uriTemplate, "'uriTemplate' must not be null");
		this.uriTemplate = uriTemplate;
		this.uriComponents = UriComponentsBuilder.fromUriString(uriTemplate).build();

		TemplateInfo info = TemplateInfo.parse(uriTemplate);
		this.variableNames = Collections.unmodifiableList(info.getVariableNames());
		this.matchPattern = info.getMatchPattern();
	}


	/**
	 * Return the names of the variables in the template, in order.
	 * @return the template variable names
	 */
	public List<String> getVariableNames() {
		return this.variableNames;
	}

	/**
	 * Given the Map of variables, expands this template into a URI. The Map keys represent variable names,
	 * the Map values variable values. The order of variables is not significant.
	 * <p>Example:
	 * <pre class="code">
	 * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
	 * Map&lt;String, String&gt; uriVariables = new HashMap&lt;String, String&gt;();
	 * uriVariables.put("booking", "42");
	 * uriVariables.put("hotel", "Rest & Relax");
	 * System.out.println(template.expand(uriVariables));
	 * </pre>
	 * will print: <blockquote>{@code http://example.com/hotels/Rest%20%26%20Relax/bookings/42}</blockquote>
	 * @param uriVariables the map of URI variables
	 * @return the expanded URI
	 * @throws IllegalArgumentException if {@code uriVariables} is {@code null};
	 * or if it does not contain values for all the variable names
	 */
	public URI expand(Map<String, ?> uriVariables) {
		UriComponents expandedComponents = this.uriComponents.expand(uriVariables);
		UriComponents encodedComponents = expandedComponents.encode();
		return encodedComponents.toUri();
	}

    /**
     * Given an array of variables, expand this template into a full URI. The array represent variable values.
     * The order of variables is significant.
     * <p>Example:
     * <pre class="code">
     * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
     * System.out.println(template.expand("Rest & Relax", "42));
     * </pre>
     * will print: <blockquote>{@code http://example.com/hotels/Rest%20%26%20Relax/bookings/42}</blockquote>
     * @param uriVariableValues the array of URI variables
     * @return the expanded URI
     * @throws IllegalArgumentException if {@code uriVariables} is {@code null}
     * or if it does not contain sufficient variables
     */
	public URI expand(Object... uriVariableValues) {
		UriComponents expandedComponents = this.uriComponents.expand(uriVariableValues);
		UriComponents encodedComponents = expandedComponents.encode();
		return encodedComponents.toUri();
	}

	/**
	 * Indicate whether the given URI matches this template.
	 * @param uri the URI to match to
	 * @return {@code true} if it matches; {@code false} otherwise
	 */
	public boolean matches(String uri) {
		if (uri == null) {
			return false;
		}
		Matcher matcher = this.matchPattern.matcher(uri);
		return matcher.matches();
	}

	/**
	 * Match the given URI to a map of variable values. Keys in the returned map are variable names,
	 * values are variable values, as occurred in the given URI.
	 * <p>Example:
	 * <pre class="code">
	 * UriTemplate template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
	 * System.out.println(template.match("http://example.com/hotels/1/bookings/42"));
	 * </pre>
	 * will print: <blockquote>{@code {hotel=1, booking=42}}</blockquote>
	 * @param uri the URI to match to
	 * @return a map of variable values
	 */
	public Map<String, String> match(String uri) {
		Assert.notNull(uri, "'uri' must not be null");
		Map<String, String> result = new LinkedHashMap<String, String>(this.variableNames.size());
		Matcher matcher = this.matchPattern.matcher(uri);
		if (matcher.find()) {
			for (int i = 1; i <= matcher.groupCount(); i++) {
				String name = this.variableNames.get(i - 1);
				String value = matcher.group(i);
				result.put(name, value);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return this.uriTemplate;
	}


	/**
	 * Helper to extract variable names and regex for matching to actual URLs.
	 */
	private static class TemplateInfo {

		private final List<String> variableNames;

		private final Pattern pattern;


		private TemplateInfo(List<String> vars, Pattern pattern) {
			this.variableNames = vars;
			this.pattern = pattern;
		}

		public List<String> getVariableNames() {
			return this.variableNames;
		}

		public Pattern getMatchPattern() {
			return this.pattern;
		}

		private static TemplateInfo parse(String uriTemplate) {
			int level = 0;
			List<String> variableNames = new ArrayList<String>();
			StringBuilder pattern = new StringBuilder();
			StringBuilder builder = new StringBuilder();
			for (int i = 0 ; i < uriTemplate.length(); i++) {
				char c = uriTemplate.charAt(i);
				if (c == '{') {
					level++;
					if (level == 1) {
						// start of URI variable
						pattern.append(quote(builder));
						builder = new StringBuilder();
						continue;
					}
				}
				else if (c == '}') {
					level--;
					if (level == 0) {
						// end of URI variable
						String variable = builder.toString();
						int idx = variable.indexOf(':');
						if (idx == -1) {
							pattern.append("(.*)");
							variableNames.add(variable);
						}
						else {
							if (idx + 1 == variable.length()) {
								throw new IllegalArgumentException(
										"No custom regular expression specified after ':' " +
												"in \"" + variable + "\"");
							}
							String regex = variable.substring(idx + 1, variable.length());
							pattern.append('(');
							pattern.append(regex);
							pattern.append(')');
							variableNames.add(variable.substring(0, idx));
						}
						builder = new StringBuilder();
						continue;
					}
				}
				builder.append(c);
			}
			if (builder.length() > 0) {
				pattern.append(quote(builder));
			}
			return new TemplateInfo(variableNames, Pattern.compile(pattern.toString()));
		}

		private static String quote(StringBuilder builder) {
			return builder.length() != 0 ? Pattern.quote(builder.toString()) : "";
		}
	}

}
