/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * Represents a WebSocket extension as defined in RFC 6455.
 *
 * <p>WebSocket extensions add protocol features to the WebSocket protocol. The extensions
 * used within a session are negotiated during the handshake phase as follows:
 * <ul>
 * <li>the client may ask for specific extensions in the HTTP handshake request</li>
 * <li>the server responds with the final list of extensions to use in the current session</li>
 * </ul>
 *
 * <p>WebSocket Extension HTTP headers may include parameters and follow
 * <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 section 3.2</a>
 *
 * <p>Note that the order of extensions in HTTP headers defines their order of execution &mdash;
 * for example, extensions "foo, bar" will be executed as "bar(foo(message))".
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.0
 * @see <a href="https://tools.ietf.org/html/rfc6455#section-9">WebSocket Protocol Extensions, RFC 6455 - Section 9</a>
 */
public class WebSocketExtension {

	private final String name;

	private final Map<String, String> parameters;


	/**
	 * Create a WebSocketExtension with the given name.
	 * @param name the name of the extension
	 */
	public WebSocketExtension(String name) {
		this(name, null);
	}

	/**
	 * Create a WebSocketExtension with the given name and parameters.
	 * @param name the name of the extension
	 * @param parameters the parameters
	 */
	public WebSocketExtension(String name, @Nullable Map<String, String> parameters) {
		Assert.hasLength(name, "Extension name must not be empty");
		this.name = name;
		if (!CollectionUtils.isEmpty(parameters)) {
			Map<String, String> map = new LinkedCaseInsensitiveMap<>(parameters.size(), Locale.ROOT);
			map.putAll(parameters);
			this.parameters = Collections.unmodifiableMap(map);
		}
		else {
			this.parameters = Collections.emptyMap();
		}
	}


	/**
	 * Return the name of the extension (never {@code null} or empty).
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the parameters of the extension (never {@code null}).
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		return (other instanceof WebSocketExtension that &&
				this.name.equals(that.name) && this.parameters.equals(that.parameters));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 31 + this.parameters.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.name);
		this.parameters.forEach((key, value) -> str.append(';').append(key).append('=').append(value));
		return str.toString();
	}


	/**
	 * Parse the given, comma-separated string into a list of {@code WebSocketExtension} objects.
	 * <p>This method can be used to parse a "Sec-WebSocket-Extension" header.
	 * @param extensions the string to parse
	 * @return the list of extensions
	 * @throws IllegalArgumentException if the string cannot be parsed
	 */
	public static List<WebSocketExtension> parseExtensions(String extensions) {
		if (StringUtils.hasText(extensions)) {
			String[] tokens = tokenize(extensions, ',');
			List<WebSocketExtension> result = new ArrayList<>(tokens.length);
			for (String token : tokens) {
				result.add(parseExtension(token));
			}
			return result;
		}
		else {
			return Collections.emptyList();
		}
	}

	private static WebSocketExtension parseExtension(String extension) {
		if (extension.contains(",")) {
			throw new IllegalArgumentException("Expected single extension value: [" + extension + "]");
		}
		String[] parts = tokenize(extension, ';');
		String name = parts[0].trim();

		Map<String, String> parameters = null;
		if (parts.length > 1) {
			parameters = CollectionUtils.newLinkedHashMap(parts.length - 1);
			for (int i = 1; i < parts.length; i++) {
				String parameter = parts[i];
				int eqIndex = parameter.indexOf('=');
				if (eqIndex != -1) {
					String attribute = parameter.substring(0, eqIndex).trim();
					String value = parameter.substring(eqIndex + 1).trim();
					parameters.put(attribute, unquote(value));
				}
			}
		}

		return new WebSocketExtension(name, parameters);
	}

	/**
	 * Tokenize the given string using the supplied delimiter, respecting double-quoted
	 * segments (and backslash escapes within them) so that delimiters inside quotes are
	 * not treated as separators.
	 */
	private static String[] tokenize(String value, char delimiter) {
		List<String> tokens = new ArrayList<>();
		int start = 0;
		boolean quoted = false;
		boolean escaped = false;
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch == delimiter && !quoted) {
				addToken(tokens, value, start, i);
				start = i + 1;
			}
			else if (!escaped && ch == '"') {
				quoted = !quoted;
			}
			escaped = (!escaped && ch == '\\');
		}
		addToken(tokens, value, start, value.length());
		return tokens.toArray(new String[0]);
	}

	private static void addToken(List<String> tokens, String value, int start, int end) {
		String token = value.substring(start, end).trim();
		if (!token.isEmpty()) {
			tokens.add(token);
		}
	}

	/**
	 * Remove surrounding double quotes from the given value, if present.
	 */
	private static String unquote(String value) {
		if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
			value = value.substring(1, value.length() - 1);
		}
		return value;
	}

}
