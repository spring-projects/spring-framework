/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;

/**
 * WebSocket {@link ConnectionManagerSupport connection manager} that connects
 * to the server via {@link WebSocketClient} and handles the session with a
 * {@link WebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class WebSocketConnectionManager extends ConnectionManagerSupport {

	private final WebSocketClient client;

	private final WebSocketHandler webSocketHandler;

	@Nullable
	private WebSocketSession webSocketSession;

	private final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();


	/**
	 * Constructor with the client to use and a handler to handle messages with.
	 */
	public WebSocketConnectionManager(WebSocketClient client,
			WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVariables) {

		super(uriTemplate, uriVariables);
		this.client = client;
		this.webSocketHandler = decorateWebSocketHandler(webSocketHandler);
	}

	/**
	 * Variant of {@link #WebSocketConnectionManager(WebSocketClient, WebSocketHandler, String, Object...)}
	 * with a prepared {@link URI}.
	 * @since 6.0.5
	 */
	public WebSocketConnectionManager(WebSocketClient client, WebSocketHandler webSocketHandler, URI uri) {
		super(uri);
		this.client = client;
		this.webSocketHandler = decorateWebSocketHandler(webSocketHandler);
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
	public void setOrigin(@Nullable String origin) {
		this.headers.setOrigin(origin);
	}

	/**
	 * Return the configured origin.
	 */
	@Nullable
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
		if (this.client instanceof Lifecycle lifecycle && !lifecycle.isRunning()) {
			lifecycle.start();
		}
		super.startInternal();
	}

	@Override
	public void stopInternal() throws Exception {
		if (this.client instanceof Lifecycle lifecycle && lifecycle.isRunning()) {
			lifecycle.stop();
		}
		super.stopInternal();
	}

	@Override
	public boolean isConnected() {
		return (this.webSocketSession != null && this.webSocketSession.isOpen());
	}

	@Override
	protected void openConnection() {
		if (logger.isInfoEnabled()) {
			logger.info("Connecting to WebSocket at " + getUri());
		}

		CompletableFuture<WebSocketSession> future =
				this.client.execute(this.webSocketHandler, this.headers, getUri());

		future.whenComplete((result, ex) -> {
			if (result != null) {
				this.webSocketSession = result;
				logger.info("Successfully connected");
			}
			else if (ex != null) {
				logger.error("Failed to connect", ex);
			}
		});
	}

	@Override
	protected void closeConnection() throws Exception {
		if (this.webSocketSession != null) {
			this.webSocketSession.close();
		}
	}

	/**
	 * Decorate the WebSocketHandler provided to the class constructor.
	 * <p>By default {@link LoggingWebSocketHandlerDecorator} is added.
	 */
	protected WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
		return new LoggingWebSocketHandlerDecorator(handler);
	}

}
