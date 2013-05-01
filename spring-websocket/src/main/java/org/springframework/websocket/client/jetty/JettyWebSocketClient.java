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

package org.springframework.websocket.client.jetty;

import java.net.URI;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;
import org.springframework.websocket.adapter.JettyWebSocketListenerAdapter;
import org.springframework.websocket.adapter.JettyWebSocketSessionAdapter;
import org.springframework.websocket.client.WebSocketClient;
import org.springframework.websocket.client.WebSocketConnectFailureException;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JettyWebSocketClient implements WebSocketClient, SmartLifecycle {

	private static final Log logger = LogFactory.getLog(JettyWebSocketClient.class);

	private final org.eclipse.jetty.websocket.client.WebSocketClient client;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	private final Object lifecycleMonitor = new Object();


	public JettyWebSocketClient() {
		this.client = new org.eclipse.jetty.websocket.client.WebSocketClient();
	}


	// TODO: configure Jetty WebSocketClient properties

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
	public WebSocketSession doHandshake(WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVariables)
			throws WebSocketConnectFailureException {

		URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVariables).encode().toUri();
		return doHandshake(webSocketHandler, null, uri);
	}

	@Override
	public WebSocketSession doHandshake(WebSocketHandler webSocketHandler, HttpHeaders headers, URI uri)
			throws WebSocketConnectFailureException {

		// TODO: populate headers

		JettyWebSocketListenerAdapter listener = new JettyWebSocketListenerAdapter(webSocketHandler);

		try {
			// block for now
			Future<org.eclipse.jetty.websocket.api.Session> future = this.client.connect(listener, uri);
			Session session = future.get();
			return new JettyWebSocketSessionAdapter(session);
		}
		catch (Exception e) {
			throw new WebSocketConnectFailureException("Failed to connect to " + uri, e);
		}
	}

}
