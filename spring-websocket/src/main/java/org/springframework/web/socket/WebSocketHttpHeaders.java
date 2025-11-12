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

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;

/**
 * An {@link HttpHeaders} variant that adds support for the HTTP headers defined
 * by the WebSocket specification RFC 6455.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class WebSocketHttpHeaders extends HttpHeaders {

	public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

	public static final String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

	public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

	public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

	public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";

	private static final long serialVersionUID = -6644521016187828916L;


	/**
	 * Construct a new, empty {@code WebSocketHttpHeaders} instance.
	 */
	public WebSocketHttpHeaders() {
		super();
	}

	/**
	 * Construct a new {@code WebSocketHttpHeaders} instance backed by the supplied
	 * {@code HttpHeaders}.
	 * <p>Changes to the {@code WebSocketHttpHeaders} created by this constructor
	 * will write through to the supplied {@code HttpHeaders}. If you wish to copy
	 * an existing {@code HttpHeaders} or {@code WebSocketHttpHeaders} instance,
	 * use {@link #copyOf(HttpHeaders)} instead. Note, however, that {@code copyOf()}
	 * does not create an instance of {@code WebSocketHttpHeaders}.
	 * <p>If the supplied {@code HttpHeaders} instance is a
	 * {@linkplain #readOnlyHttpHeaders(HttpHeaders) read-only}
	 * {@code HttpHeaders} wrapper, it will be unwrapped to ensure that the
	 * {@code WebSocketHttpHeaders} instance created by this constructor is mutable.
	 * Once the writable instance is mutated, the read-only instance is likely to
	 * be out of sync and should be discarded.
	 * @param httpHeaders the headers to expose
	 * @see #copyOf(HttpHeaders)
	 */
	public WebSocketHttpHeaders(HttpHeaders httpHeaders) {
		super(httpHeaders);
	}


	/**
	 * Sets the (new) value of the {@code Sec-WebSocket-Accept} header.
	 * @param secWebSocketAccept the value of the header
	 */
	public void setSecWebSocketAccept(@Nullable String secWebSocketAccept) {
		set(SEC_WEBSOCKET_ACCEPT, secWebSocketAccept);
	}

	/**
	 * Returns the value of the {@code Sec-WebSocket-Accept} header.
	 * @return the value of the header
	 */
	public @Nullable String getSecWebSocketAccept() {
		return getFirst(SEC_WEBSOCKET_ACCEPT);
	}

	/**
	 * Returns the value of the {@code Sec-WebSocket-Extensions} header.
	 * @return the value of the header
	 */
	public List<WebSocketExtension> getSecWebSocketExtensions() {
		List<String> values = get(SEC_WEBSOCKET_EXTENSIONS);
		if (CollectionUtils.isEmpty(values)) {
			return Collections.emptyList();
		}
		else {
			List<WebSocketExtension> result = new ArrayList<>(values.size());
			for (String value : values) {
				result.addAll(WebSocketExtension.parseExtensions(value));
			}
			return result;
		}
	}

	/**
	 * Sets the (new) value(s) of the {@code Sec-WebSocket-Extensions} header.
	 * @param extensions the values for the header
	 */
	public void setSecWebSocketExtensions(List<WebSocketExtension> extensions) {
		List<String> result = new ArrayList<>(extensions.size());
		for (WebSocketExtension extension : extensions) {
			result.add(extension.toString());
		}
		set(SEC_WEBSOCKET_EXTENSIONS, toCommaDelimitedString(result));
	}

	/**
	 * Sets the (new) value of the {@code Sec-WebSocket-Key} header.
	 * @param secWebSocketKey the value of the header
	 */
	public void setSecWebSocketKey(@Nullable String secWebSocketKey) {
		set(SEC_WEBSOCKET_KEY, secWebSocketKey);
	}

	/**
	 * Returns the value of the {@code Sec-WebSocket-Key} header.
	 * @return the value of the header
	 */
	public @Nullable String getSecWebSocketKey() {
		return getFirst(SEC_WEBSOCKET_KEY);
	}

	/**
	 * Sets the (new) value of the {@code Sec-WebSocket-Protocol} header.
	 * @param secWebSocketProtocol the value of the header
	 */
	public void setSecWebSocketProtocol(String secWebSocketProtocol) {
		set(SEC_WEBSOCKET_PROTOCOL, secWebSocketProtocol);
	}

	/**
	 * Sets the (new) value of the {@code Sec-WebSocket-Protocol} header.
	 * @param secWebSocketProtocols the value of the header
	 */
	public void setSecWebSocketProtocol(List<String> secWebSocketProtocols) {
		set(SEC_WEBSOCKET_PROTOCOL, toCommaDelimitedString(secWebSocketProtocols));
	}

	/**
	 * Returns the value of the {@code Sec-WebSocket-Protocol} header.
	 * @return the value of the header
	 */
	public List<String> getSecWebSocketProtocol() {
		List<String> values = get(SEC_WEBSOCKET_PROTOCOL);
		if (CollectionUtils.isEmpty(values)) {
			return Collections.emptyList();
		}
		else if (values.size() == 1) {
			return getValuesAsList(SEC_WEBSOCKET_PROTOCOL);
		}
		else {
			return values;
		}
	}

	/**
	 * Sets the (new) value of the {@code Sec-WebSocket-Version} header.
	 * @param secWebSocketVersion the value of the header
	 */
	public void setSecWebSocketVersion(@Nullable String secWebSocketVersion) {
		set(SEC_WEBSOCKET_VERSION, secWebSocketVersion);
	}

	/**
	 * Returns the value of the {@code Sec-WebSocket-Version} header.
	 * @return the value of the header
	 */
	public @Nullable String getSecWebSocketVersion() {
		return getFirst(SEC_WEBSOCKET_VERSION);
	}

}
