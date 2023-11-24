/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.socket.client;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Abstract base class for {@link WebSocketClient} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractWebSocketClient implements WebSocketClient {

	private static final Set<String> specialHeaders = Set.of(
			"cache-control",
			"connection",
			"host",
			"sec-websocket-extensions",
			"sec-websocket-key",
			"sec-websocket-protocol",
			"sec-websocket-version",
			"pragma",
			"upgrade");


	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public CompletableFuture<WebSocketSession> execute(WebSocketHandler webSocketHandler,
			String uriTemplate, Object... uriVars) {

		Assert.notNull(uriTemplate, "'uriTemplate' must not be null");
		URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
		return execute(webSocketHandler, null, uri);
	}

	@Override
	public final CompletableFuture<WebSocketSession> execute(WebSocketHandler webSocketHandler,
			@Nullable WebSocketHttpHeaders headers, URI uri) {

		Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");
		assertUri(uri);

		if (logger.isDebugEnabled()) {
			logger.debug("Connecting to " + uri);
		}

		HttpHeaders headersToUse = new HttpHeaders();
		if (headers != null) {
			headers.forEach((header, values) -> {
				if (values != null && !specialHeaders.contains(header.toLowerCase())) {
					headersToUse.put(header, values);
				}
			});
		}

		List<String> subProtocols =
				(headers != null ? headers.getSecWebSocketProtocol() : Collections.emptyList());
		List<WebSocketExtension> extensions =
				(headers != null ? headers.getSecWebSocketExtensions() : Collections.emptyList());

		return executeInternal(webSocketHandler, headersToUse, uri, subProtocols, extensions,
				Collections.emptyMap());
	}

	protected void assertUri(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		String scheme = uri.getScheme();
		if (!"ws".equals(scheme) && !"wss".equals(scheme)) {
			throw new IllegalArgumentException("Invalid scheme: " + scheme);
		}
	}

	/**
	 * Perform the actual handshake to establish a connection to the server.
	 * @param webSocketHandler the client-side handler for WebSocket messages
	 * @param headers the HTTP headers to use for the handshake, with unwanted (forbidden)
	 * headers filtered out (never {@code null})
	 * @param uri the target URI for the handshake (never {@code null})
	 * @param subProtocols requested sub-protocols, or an empty list
	 * @param extensions requested WebSocket extensions, or an empty list
	 * @param attributes the attributes to associate with the WebSocketSession, i.e. via
	 * {@link WebSocketSession#getAttributes()}; currently always an empty map.
	 * @return the established WebSocket session wrapped in a {@code ListenableFuture}.
	 * @deprecated as of 6.0, in favor of {@link #executeInternal(WebSocketHandler, HttpHeaders, URI, List, List, Map)}
	 */
	@Deprecated(since = "6.0")
	protected org.springframework.util.concurrent.ListenableFuture<WebSocketSession> doHandshakeInternal(
			WebSocketHandler webSocketHandler, HttpHeaders headers, URI uri, List<String> subProtocols,
			List<WebSocketExtension> extensions, Map<String, Object> attributes) {

		throw new UnsupportedOperationException("doHandshakeInternal is deprecated in favor of executeInternal");
	}

	/**
	 * Perform the actual handshake to establish a connection to the server.
	 * @param webSocketHandler the client-side handler for WebSocket messages
	 * @param headers the HTTP headers to use for the handshake, with unwanted (forbidden)
	 * headers filtered out (never {@code null})
	 * @param uri the target URI for the handshake (never {@code null})
	 * @param subProtocols requested sub-protocols, or an empty list
	 * @param extensions requested WebSocket extensions, or an empty list
	 * @param attributes the attributes to associate with the WebSocketSession, i.e. via
	 * {@link WebSocketSession#getAttributes()}; currently always an empty map
	 * @return the established WebSocket session wrapped in a {@code CompletableFuture}.
	 */
	protected abstract CompletableFuture<WebSocketSession> executeInternal(WebSocketHandler webSocketHandler,
			HttpHeaders headers, URI uri, List<String> subProtocols, List<WebSocketExtension> extensions,
			Map<String, Object> attributes);

}
