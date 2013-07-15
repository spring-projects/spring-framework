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

package org.springframework.web.socket.client.endpoint;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.HandshakeResponse;
import javax.websocket.WebSocketContainer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.StandardEndpointAdapter;
import org.springframework.web.socket.adapter.StandardWebSocketSessionAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectFailureException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Initiates WebSocket requests to a WebSocket server programatically through the standard
 * Java WebSocket API.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketClient implements WebSocketClient {

	private static final Log logger = LogFactory.getLog(StandardWebSocketClient.class);

	private final WebSocketContainer webSocketContainer;


	public StandardWebSocketClient() {
		this.webSocketContainer = ContainerProvider.getWebSocketContainer();
	}

	public StandardWebSocketClient(WebSocketContainer webSocketContainer) {
		Assert.notNull(webSocketContainer, "webSocketContainer must not be null");
		this.webSocketContainer = webSocketContainer;
	}


	@Override
	public WebSocketSession doHandshake(WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVariables)
			throws WebSocketConnectFailureException {

		Assert.notNull(uriTemplate, "uriTemplate must not be null");
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVariables).encode();
		return doHandshake(webSocketHandler, null, uriComponents.toUri());
	}

	@Override
	public WebSocketSession doHandshake(WebSocketHandler webSocketHandler, HttpHeaders httpHeaders, URI uri)
			throws WebSocketConnectFailureException {

		Assert.notNull(webSocketHandler, "webSocketHandler must not be null");
		Assert.notNull(uri, "uri must not be null");

		httpHeaders = (httpHeaders != null) ? httpHeaders : new HttpHeaders();

		if (logger.isDebugEnabled()) {
			logger.debug("Connecting to " + uri);
		}

		StandardWebSocketSessionAdapter session = new StandardWebSocketSessionAdapter();
		session.setUri(uri);
		session.setRemoteHostName(uri.getHost());

		ClientEndpointConfig.Builder configBuidler = ClientEndpointConfig.Builder.create();
		configBuidler.configurator(new StandardWebSocketClientConfigurator(httpHeaders));

		List<String> protocols = httpHeaders.getSecWebSocketProtocol();
		if (!protocols.isEmpty()) {
			configBuidler.preferredSubprotocols(protocols);
		}

		try {
			// TODO: do not block
			Endpoint endpoint = new StandardEndpointAdapter(webSocketHandler, session);
			this.webSocketContainer.connectToServer(endpoint, configBuidler.build(), uri);

			return session;
		}
		catch (Exception e) {
			throw new WebSocketConnectFailureException("Failed to connect to " + uri, e);
		}
	}


	private static class StandardWebSocketClientConfigurator extends Configurator {

		private static final Set<String> EXCLUDED_HEADERS = new HashSet<String>(
				Arrays.asList("Sec-WebSocket-Accept", "Sec-WebSocket-Extensions", "Sec-WebSocket-Key",
						"Sec-WebSocket-Protocol", "Sec-WebSocket-Version"));

		private final HttpHeaders httpHeaders;


		public StandardWebSocketClientConfigurator(HttpHeaders httpHeaders) {
			this.httpHeaders = httpHeaders;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> headers) {
			for (String headerName : this.httpHeaders.keySet()) {
				if (!EXCLUDED_HEADERS.contains(headerName)) {
					List<String> value = this.httpHeaders.get(headerName);
					if (logger.isTraceEnabled()) {
						logger.trace("Adding header [" + headerName + "=" + value + "]");
					}
					headers.put(headerName, value);
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake request headers: " + headers);
			}
		}
		@Override
		public void afterResponse(HandshakeResponse handshakeResponse) {
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake response headers: " + handshakeResponse.getHeaders());
			}
		}
	}

}
