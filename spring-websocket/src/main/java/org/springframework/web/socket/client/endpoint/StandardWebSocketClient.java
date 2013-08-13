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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.HandshakeResponse;
import javax.websocket.WebSocketContainer;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.StandardWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.StandardWebSocketSession;
import org.springframework.web.socket.client.AbstractWebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectFailureException;

/**
 * Initiates WebSocket requests to a WebSocket server programatically through the standard
 * Java WebSocket API.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketClient extends AbstractWebSocketClient {

	private final WebSocketContainer webSocketContainer;


	public StandardWebSocketClient() {
		this.webSocketContainer = ContainerProvider.getWebSocketContainer();
	}

	public StandardWebSocketClient(WebSocketContainer webSocketContainer) {
		Assert.notNull(webSocketContainer, "webSocketContainer must not be null");
		this.webSocketContainer = webSocketContainer;
	}


	@Override
	protected WebSocketSession doHandshakeInternal(WebSocketHandler webSocketHandler, HttpHeaders headers,
			URI uri, List<String> protocols, Map<String, Object> handshakeAttributes)
					throws WebSocketConnectFailureException {

		int port = getPort(uri);
		InetSocketAddress localAddress = new InetSocketAddress(getLocalHost(), port);
		InetSocketAddress remoteAddress = new InetSocketAddress(uri.getHost(), port);

		StandardWebSocketSession session = new StandardWebSocketSession(headers,
				handshakeAttributes, localAddress, remoteAddress);

		ClientEndpointConfig.Builder configBuidler = ClientEndpointConfig.Builder.create();
		configBuidler.configurator(new StandardWebSocketClientConfigurator(headers));
		configBuidler.preferredSubprotocols(protocols);

		try {
			// TODO: do not block
			Endpoint endpoint = new StandardWebSocketHandlerAdapter(webSocketHandler, session);
			this.webSocketContainer.connectToServer(endpoint, configBuidler.build(), uri);

			return session;
		}
		catch (Exception e) {
			throw new WebSocketConnectFailureException("Failed to connect to " + uri, e);
		}
	}

	private InetAddress getLocalHost() {
		try {
			return InetAddress.getLocalHost();
		}
		catch (UnknownHostException e) {
			return InetAddress.getLoopbackAddress();
		}
	}

	private int getPort(URI uri) {
		if (uri.getPort() == -1) {
	        String scheme = uri.getScheme().toLowerCase(Locale.ENGLISH);
	        return "wss".equals(scheme) ? 443 : 80;
		}
		return uri.getPort();
	}


	private class StandardWebSocketClientConfigurator extends Configurator {

		private final HttpHeaders headers;


		public StandardWebSocketClientConfigurator(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> requestHeaders) {
			requestHeaders.putAll(this.headers);
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake request headers: " + requestHeaders);
			}
		}
		@Override
		public void afterResponse(HandshakeResponse response) {
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake response headers: " + response.getHeaders());
			}
		}
	}

}
