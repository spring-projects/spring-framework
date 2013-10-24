/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * WebSocket Protocol extension.
 * Adds new protocol features to the WebSocket protocol; the extensions used
 * within a session are negotiated during the handshake phase:
 * <ul>
 * <li>the client may ask for specific extensions in the HTTP request</li>
 * <li>the server declares the final list of supported extensions for the current session in the HTTP response</li>
 * </ul>
 *
 * <p>WebSocket Extension HTTP headers may include parameters and follow
 * <a href="https://tools.ietf.org/html/rfc2616#section-4.2">RFC 2616 Section 4.2</a>
 * specifications.</p>
 *
 * <p>Note that the order of extensions in HTTP headers defines their order of execution,
 * e.g. extensions "foo, bar" will be executed as "bar(foo(message))".</p>
 *
 * @author Brian Clozel
 * @since 4.0
 * @see <a href="https://tools.ietf.org/html/rfc6455#section-9">
 *     WebSocket Protocol Extensions, RFC 6455 - Section 9</a>
 */
public class WebSocketExtension {

	private final String name;

	private final Map<String, String> parameters;

	public WebSocketExtension(String name) {
		this(name,null);
	}

	public WebSocketExtension(String name, Map<String, String> parameters) {
		Assert.hasLength(name, "extension name must not be empty");
		this.name = name;
		if (!CollectionUtils.isEmpty(parameters)) {
			Map<String, String> m = new LinkedCaseInsensitiveMap<String>(parameters.size(), Locale.ENGLISH);
			m.putAll(parameters);
			this.parameters = Collections.unmodifiableMap(m);
		}
		else {
			this.parameters = Collections.emptyMap();
		}
	}

	/**
	 * @return the name of the extension
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the parameters of the extension
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	/**
	 * Parse a list of raw WebSocket extension headers
	 */
	public static List<WebSocketExtension> parseHeaders(List<String> headers) {
		if (headers == null || headers.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			List<WebSocketExtension> result = new ArrayList<WebSocketExtension>(headers.size());
			for (String header : headers) {
				result.addAll(parseHeader(header));
			}
			return result;
		}
	}

	/**
	 * Parse a raw WebSocket extension header
	 */
	public static List<WebSocketExtension> parseHeader(String header) {
		if (header == null || !StringUtils.hasText(header)) {
			return Collections.emptyList();
		}
		else {
			List<WebSocketExtension> result = new ArrayList<WebSocketExtension>();
			for(String token : header.split(",")) {
				result.add(parse(token));
			}
			return result;
		}
	}

	private static WebSocketExtension parse(String extension) {
		Assert.doesNotContain(extension,",","this string contains multiple extension declarations");
		String[] parts = StringUtils.tokenizeToStringArray(extension, ";");
		String name = parts[0].trim();

		Map<String, String> parameters = null;
		if (parts.length > 1) {
			parameters = new LinkedHashMap<String, String>(parts.length - 1);
			for (int i = 1; i < parts.length; i++) {
				String parameter = parts[i];
				int eqIndex = parameter.indexOf('=');
				if (eqIndex != -1) {
					String attribute = parameter.substring(0, eqIndex);
					String value = parameter.substring(eqIndex + 1, parameter.length());
					parameters.put(attribute, value);
				}
			}
		}

		return new WebSocketExtension(name,parameters);
	}

	/**
	 * Convert a list of WebSocketExtensions to a list of String,
	 * which is convenient for native HTTP headers.
	 */
	public static List<String> toStringList(List<WebSocketExtension> extensions) {
		List<String> result = new ArrayList<String>(extensions.size());
		for(WebSocketExtension extension : extensions) {
			result.add(extension.toString());
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.name);
		for (String param : parameters.keySet()) {
			str.append(';');
			str.append(param);
			str.append('=');
			str.append(this.parameters.get(param));
		}
		return str.toString();
	}
}
