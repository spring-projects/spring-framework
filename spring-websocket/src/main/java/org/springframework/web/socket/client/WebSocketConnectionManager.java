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

import java.util.List;

import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
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

	private HttpHeaders headers = new HttpHeaders();

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

	/**
	 * Set the sub-protocols to use. If configured, specified sub-protocols will be
	 * requested in the handshake through the {@code Sec-WebSocket-Protocol} header. The
	 * resulting WebSocket session will contain the protocol accepted by the server, if
	 * any.
	 */
	public void setSubProtocols(List<String> protocols) {
		this.headers.setSecWebSocketProtocol(protocols);
	}

	/**
	 * Return the configured sub-protocols to use.
	 */
	public List<String> getSubProtocols() {
		return this.headers.getSecWebSocketProtocol();
	}

	/**
	 * Set the origin to use.
	 */
	public void setOrigin(String origin) {
		this.headers.setOrigin(origin);
	}

	/**
	 * @return the configured origin.
	 */
	public String getOrigin() {
		return this.headers.getOrigin();
	}

	/**
	 * Provide default headers to add to the WebSocket handshake request.
	 */
	public void setHeaders(HttpHeaders headers) {
		this.headers.clear();
		this.headers.putAll(headers);
	}

	/**
	 * Return the default headers for the WebSocket handshake request.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
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
	protected void openConnection() {

		logger.info("Connecting to WebSocket at " + getUri());

		ListenableFuture<WebSocketSession> future =
				this.client.doHandshake(this.webSocketHandler, this.headers, getUri());

		future.addCallback(new ListenableFutureCallback<WebSocketSession>() {
			@Override
			public void onSuccess(WebSocketSession result) {
				webSocketSession = result;
				logger.info("Successfully connected");
			}
			@Override
			public void onFailure(Throwable t) {
				logger.error("Failed to connect", t);
			}
		});
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
