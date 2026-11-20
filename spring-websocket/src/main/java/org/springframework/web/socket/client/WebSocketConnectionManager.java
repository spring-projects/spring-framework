/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.client;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpHeaders;
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

	private @Nullable WebSocketSession webSocketSession;

	private final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();


	/**
	 * Constructor with the client to use and a handler to handle messages with.
	 */
	public WebSocketConnectionManager(WebSocketClient client,
			WebSocketHandler webSocketHandler, String uriTemplate, @Nullable Object... uriVariables) {

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
	public @Nullable String getOrigin() {
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
