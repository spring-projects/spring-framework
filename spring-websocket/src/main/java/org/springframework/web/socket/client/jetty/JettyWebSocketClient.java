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

package org.springframework.web.socket.client.jetty;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.JettyWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.JettyWebSocketSession;
import org.springframework.web.socket.client.AbstractWebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectFailureException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Initiates WebSocket requests to a WebSocket server programatically through the Jetty
 * WebSocket API.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JettyWebSocketClient extends AbstractWebSocketClient implements SmartLifecycle {

	private final org.eclipse.jetty.websocket.client.WebSocketClient client;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Default constructor that creates an instance of
	 * {@link org.eclipse.jetty.websocket.client.WebSocketClient} with default settings.
	 */
	public JettyWebSocketClient() {
		this.client = new org.eclipse.jetty.websocket.client.WebSocketClient();
	}

	/**
	 * Constructor that accepts a pre-configured {@link WebSocketClient}.
	 */
	public JettyWebSocketClient(WebSocketClient client) {
		super();
		this.client = client;
	}


	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.client.isStarted();
		}
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Starting Jetty WebSocketClient");
					}
					this.client.start();
				}
				catch (Exception e) {
					throw new IllegalStateException("Failed to start Jetty client", e);
				}
			}
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Stopping Jetty WebSocketClient");
					}
					this.client.stop();
				}
				catch (Exception e) {
					logger.error("Error stopping Jetty WebSocketClient", e);
				}
			}
		}
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	@Override
	public WebSocketSession doHandshake(WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVars)
			throws WebSocketConnectFailureException {

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode();
		return doHandshake(webSocketHandler, null, uriComponents.toUri());
	}

	@Override
	public WebSocketSession doHandshakeInternal(WebSocketHandler wsHandler, HttpHeaders headers,
			URI uri, List<String> protocols, Map<String, Object> handshakeAttributes)
					throws WebSocketConnectFailureException {

		ClientUpgradeRequest request = new ClientUpgradeRequest();
		request.setSubProtocols(protocols);
		for (String header : headers.keySet()) {
			request.setHeader(header, headers.get(header));
		}

		Principal user = getUser();
		JettyWebSocketSession wsSession = new JettyWebSocketSession(user, handshakeAttributes);
		JettyWebSocketHandlerAdapter listener = new JettyWebSocketHandlerAdapter(wsHandler, wsSession);

		try {
			// TODO: do not block
			this.client.connect(listener, uri, request).get();
			return wsSession;
		}
		catch (Exception e) {
			throw new WebSocketConnectFailureException("Failed to connect to " + uri, e);
		}
	}


	/**
	 * @return the user to make available through {@link WebSocketSession#getPrincipal()};
	 *         by default this method returns {@code null}
	 */
	protected Principal getUser() {
		return null;
	}

}
