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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.websocket.Extension;

import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * Represents a WebSocket extension as defined in the RFC 6455.
 * WebSocket extensions add protocol features to the WebSocket protocol. The extensions
 * used within a session are negotiated during the handshake phase as follows:
 * <ul>
 * <li>the client may ask for specific extensions in the HTTP handshake request</li>
 * <li>the server responds with the final list of extensions to use in the current session</li>
 * </ul>
 *
 * <p>WebSocket Extension HTTP headers may include parameters and follow
 * <a href="https://tools.ietf.org/html/rfc2616#section-4.2">RFC 2616 Section 4.2</a></p>
 *
 * <p>Note that the order of extensions in HTTP headers defines their order of execution,
 * e.g. extensions "foo, bar" will be executed as "bar(foo(message))".</p>
 *
 * @author Brian Clozel
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
	 * @return the parameters of the extension, never {@code null}
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	/**
	 * Parse the given, comma-separated string into a list of {@code WebSocketExtension} objects.
	 * <p>This method can be used to parse a "Sec-WebSocket-Extension" extensions.
	 * @param extensions the string to parse
	 * @return the list of extensions
	 * @throws IllegalArgumentException if the string cannot be parsed
	 */
	public static List<WebSocketExtension> parseExtensions(String extensions) {
		if (extensions == null || !StringUtils.hasText(extensions)) {
			return Collections.emptyList();
		}
		else {
			List<WebSocketExtension> result = new ArrayList<WebSocketExtension>();
			for(String token : extensions.split(",")) {
				result.add(parseExtension(token));
			}
			return result;
		}
	}

	private static WebSocketExtension parseExtension(String extension) {
		Assert.doesNotContain(extension, ",", "Expected a single extension value: " + extension);
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

		return new WebSocketExtension(name, parameters);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}
		WebSocketExtension that = (WebSocketExtension) o;
		if (!name.equals(that.name)) {
			return false;
		}
		if (!parameters.equals(that.parameters)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + parameters.hashCode();
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


	// Standard WebSocketExtension adapters

	public static class StandardToWebSocketExtensionAdapter extends WebSocketExtension {

		public StandardToWebSocketExtensionAdapter(Extension ext) {
			super(ext.getName());
			for (Extension.Parameter p : ext.getParameters()) {
				super.getParameters().put(p.getName(), p.getValue());
			}
		}
	}

	public static class WebSocketToStandardExtensionAdapter implements Extension {

		private final String name;

		private final List<Parameter> parameters = new ArrayList<Parameter>();

		public WebSocketToStandardExtensionAdapter(final WebSocketExtension ext) {
			this.name = ext.getName();
			for (final String paramName : ext.getParameters().keySet()) {
				this.parameters.add(new Parameter() {
					@Override
					public String getName() {
						return paramName;
					}
					@Override
					public String getValue() {
						return ext.getParameters().get(paramName);
					}
				});
			}
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public List<Parameter> getParameters() {
			return this.parameters;
		}
	}

	// Jetty WebSocketExtension adapters

	public static class WebSocketToJettyExtensionConfigAdapter extends ExtensionConfig {

		public WebSocketToJettyExtensionConfigAdapter(WebSocketExtension extension) {
			super(extension.getName());
			for (Map.Entry<String,String> p : extension.getParameters().entrySet()) {
				super.setParameter(p.getKey(), p.getValue());
			}
		}
	}



}
