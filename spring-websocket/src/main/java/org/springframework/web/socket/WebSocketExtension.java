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

	private static final String TOKEN_SEPARATORS = "()<>@,;:\\\"/[]?={} \t";

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
			List<String> tokens = tokenize(extensions, ',');
			List<WebSocketExtension> result = new ArrayList<>(tokens.size());
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
		List<String> parts = tokenize(extension, ';');
		if (parts.isEmpty()) {
			throw new IllegalArgumentException("Expected an extension value: [" + extension + "]");
		}
		String name = parts.get(0);

		Map<String, String> parameters = null;
		if (parts.size() > 1) {
			parameters = CollectionUtils.newLinkedHashMap(parts.size() - 1);
			for (int i = 1; i < parts.size(); i++) {
				String parameter = parts.get(i);
				int eqIndex = parameter.indexOf('=');
				if (eqIndex != -1) {
					String attribute = parameter.substring(0, eqIndex);
					String value = parseParameterValue(parameter.substring(eqIndex + 1));
					parameters.put(attribute, value);
				}
			}
		}

		return new WebSocketExtension(name, parameters);
	}

	private static List<String> tokenize(String value, char delimiter) {
		List<String> tokens = new ArrayList<>();
		boolean inQuotes = false;
		int startIndex = 0;
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch == '\\' && inQuotes) {
				i++;
			}
			else if (ch == '"') {
				inQuotes = !inQuotes;
			}
			else if (ch == delimiter && !inQuotes) {
				addToken(value, startIndex, i, tokens);
				startIndex = i + 1;
			}
		}
		if (inQuotes) {
			throw new IllegalArgumentException("Unterminated quoted string in extension value: [" + value + "]");
		}
		addToken(value, startIndex, value.length(), tokens);
		return tokens;
	}

	private static void addToken(String value, int startIndex, int endIndex, List<String> tokens) {
		String token = value.substring(startIndex, endIndex).trim();
		if (!token.isEmpty()) {
			tokens.add(token);
		}
	}

	private static String parseParameterValue(String value) {
		if (!value.startsWith("\"")) {
			return value;
		}
		StringBuilder unquoted = new StringBuilder(value.length() - 2);
		for (int i = 1; i < value.length() - 1; i++) {
			char ch = value.charAt(i);
			if (ch == '\\') {
				ch = value.charAt(++i);
			}
			unquoted.append(ch);
		}
		String result = unquoted.toString();
		if (!isToken(result)) {
			throw new IllegalArgumentException(
					"Quoted extension parameter value must conform to the 'token' ABNF: [" + value + "]");
		}
		return result;
	}

	private static boolean isToken(String value) {
		if (value.isEmpty()) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch <= 31 || ch >= 127 || TOKEN_SEPARATORS.indexOf(ch) != -1) {
				return false;
			}
		}
		return true;
	}

}
