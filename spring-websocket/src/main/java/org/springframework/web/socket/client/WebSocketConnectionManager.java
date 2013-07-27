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

package org.springframework.web.socket.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.support.LoggingWebSocketHandlerDecorator;

/**
 * A WebSocket connection manager that is given a URI, a {@link WebSocketClient}, and a
 * {@link WebSocketHandler}, connects to a WebSocket server through {@link #start()} and
 * {@link #stop()} methods. If {@link #setAutoStartup(boolean)} is set to {@code true}
 * this will be done automatically when the Spring ApplicationContext is refreshed.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketConnectionManager extends ConnectionManagerSupport {

	private final WebSocketClient client;

	private final WebSocketHandler webSocketHandler;

	private WebSocketSession webSocketSession;

	private final List<String> protocols = new ArrayList<String>();

	private final boolean syncClientLifecycle;


	public WebSocketConnectionManager(WebSocketClient client,
			WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVariables) {

		super(uriTemplate, uriVariables);
		this.client = client;
		this.webSocketHandler = decorateWebSocketHandler(webSocketHandler);
		this.syncClientLifecycle = ((client instanceof SmartLifecycle) && !((SmartLifecycle) client).isRunning());
	}


	/**
	 * Decorate the WebSocketHandler provided to the class constructor.
	 *
	 * <p>By default {@link LoggingWebSocketHandlerDecorator} is added.
	 */
	protected WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
		return new LoggingWebSocketHandlerDecorator(handler);
	}

	public void setSupportedProtocols(List<String> protocols) {
		this.protocols.clear();
		if (!CollectionUtils.isEmpty(protocols)) {
			this.protocols.addAll(protocols);
		}
	}

	public List<String> getSupportedProtocols() {
		return this.protocols;
	}

	@Override
	public void startInternal() {
		if (this.syncClientLifecycle) {
			((SmartLifecycle) this.client).start();
		}
		super.startInternal();
	}

	@Override
	public void stopInternal() throws Exception {
		if (this.syncClientLifecycle) {
			((SmartLifecycle) this.client).stop();
		}
		super.stopInternal();
	}

	@Override
	protected void openConnection() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setSecWebSocketProtocol(this.protocols);
		this.webSocketSession = this.client.doHandshake(this.webSocketHandler, headers, getUri());
	}

	@Override
	protected void closeConnection() throws Exception {
		this.webSocketSession.close();
	}

	@Override
	protected boolean isConnected() {
		return ((this.webSocketSession != null) && (this.webSocketSession.isOpen()));
	}

}
