/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Abstract base class for {@link WebSocketClient} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractWebSocketClient implements WebSocketClient {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final Set<String> specialHeaders = new HashSet<String>();

	static {
		specialHeaders.add("cache-control");
		specialHeaders.add("cookie");
		specialHeaders.add("connection");
		specialHeaders.add("host");
		specialHeaders.add("sec-websocket-extensions");
		specialHeaders.add("sec-websocket-key");
		specialHeaders.add("sec-websocket-protocol");
		specialHeaders.add("sec-websocket-version");
		specialHeaders.add("pragma");
		specialHeaders.add("upgrade");
	}


	@Override
	public ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			String uriTemplate, Object... uriVars) {

		Assert.notNull(uriTemplate, "uriTemplate must not be null");
		URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
		return doHandshake(webSocketHandler, null, uri);
	}

	@Override
	public final ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			WebSocketHttpHeaders headers, URI uri) {

		Assert.notNull(webSocketHandler, "webSocketHandler must not be null");
		Assert.notNull(uri, "uri must not be null");

		String scheme = uri.getScheme();
		Assert.isTrue(((scheme != null) && ("ws".equals(scheme) || "wss".equals(scheme))), "Invalid scheme: " + scheme);

		if (logger.isDebugEnabled()) {
			logger.debug("Connecting to " + uri);
		}

		HttpHeaders headersToUse = new HttpHeaders();
		if (headers != null) {
			for (String header : headers.keySet()) {
				if (!specialHeaders.contains(header.toLowerCase())) {
					headersToUse.put(header, headers.get(header));
				}
			}
		}

		List<String> subProtocols = ((headers != null) && (headers.getSecWebSocketProtocol() != null)) ?
				headers.getSecWebSocketProtocol() : Collections.<String>emptyList();

		List<WebSocketExtension> extensions = ((headers != null) && (headers.getSecWebSocketExtensions() != null)) ?
				headers.getSecWebSocketExtensions() : Collections.<WebSocketExtension>emptyList();

		return doHandshakeInternal(webSocketHandler, headersToUse, uri, subProtocols, extensions,
				Collections.<String, Object>emptyMap());
	}

	/**
	 * Perform the actual handshake to establish a connection to the server.
	 * @param webSocketHandler the client-side handler for WebSocket messages
	 * @param headers HTTP headers to use for the handshake, with unwanted (forbidden)
	 * headers filtered out, never {@code null}
	 * @param uri the target URI for the handshake, never {@code null}
	 * @param subProtocols requested sub-protocols, or an empty list
	 * @param extensions requested WebSocket extensions, or an empty list
	 * @param handshakeAttributes attributes to make available via
	 * {@link WebSocketSession#getHandshakeAttributes()}; currently always an empty map.
	 * @return the established WebSocket session wrapped in a ListenableFuture.
	 */
	protected abstract ListenableFuture<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
			HttpHeaders headers, URI uri, List<String> subProtocols, List<WebSocketExtension> extensions,
			Map<String, Object> handshakeAttributes);

}
