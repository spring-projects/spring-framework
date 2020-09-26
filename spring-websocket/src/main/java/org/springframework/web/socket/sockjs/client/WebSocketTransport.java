/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * A SockJS {@link Transport} that uses a
 * {@link org.springframework.web.socket.client.WebSocketClient WebSocketClient}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class WebSocketTransport implements Transport, Lifecycle {

	private static final Log logger = LogFactory.getLog(WebSocketTransport.class);

	private final WebSocketClient webSocketClient;

	private volatile boolean running;


	public WebSocketTransport(WebSocketClient webSocketClient) {
		Assert.notNull(webSocketClient, "WebSocketClient is required");
		this.webSocketClient = webSocketClient;
	}


	/**
	 * Return the configured {@code WebSocketClient}.
	 */
	public WebSocketClient getWebSocketClient() {
		return this.webSocketClient;
	}

	@Override
	public List<TransportType> getTransportTypes() {
		return Collections.singletonList(TransportType.WEBSOCKET);
	}

	@Override
	public ListenableFuture<WebSocketSession> connect(TransportRequest request, WebSocketHandler handler) {
		final SettableListenableFuture<WebSocketSession> future = new SettableListenableFuture<>();
		WebSocketClientSockJsSession session = new WebSocketClientSockJsSession(request, handler, future);
		handler = new ClientSockJsWebSocketHandler(session);
		request.addTimeoutTask(session.getTimeoutTask());

		URI url = request.getTransportUrl();
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders(request.getHandshakeHeaders());
		if (logger.isDebugEnabled()) {
			logger.debug("Starting WebSocket session on " + url);
		}
		this.webSocketClient.doHandshake(handler, headers, url).addCallback(
				new ListenableFutureCallback<WebSocketSession>() {
					@Override
					public void onSuccess(@Nullable WebSocketSession webSocketSession) {
						// WebSocket session ready, SockJS Session not yet
					}
					@Override
					public void onFailure(Throwable ex) {
						future.setException(ex);
					}
				});
		return future;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			if (this.webSocketClient instanceof Lifecycle) {
				((Lifecycle) this.webSocketClient).start();
			}
			else {
				this.running = true;
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			if (this.webSocketClient instanceof Lifecycle) {
				((Lifecycle) this.webSocketClient).stop();
			}
			else {
				this.running = false;
			}
		}
	}

	@Override
	public boolean isRunning() {
		if (this.webSocketClient instanceof Lifecycle) {
			return ((Lifecycle) this.webSocketClient).isRunning();
		}
		else {
			return this.running;
		}
	}


	@Override
	public String toString() {
		return "WebSocketTransport[client=" + this.webSocketClient + "]";
	}


	private static class ClientSockJsWebSocketHandler extends TextWebSocketHandler {

		private final WebSocketClientSockJsSession sockJsSession;

		private final AtomicBoolean connected = new AtomicBoolean(false);

		public ClientSockJsWebSocketHandler(WebSocketClientSockJsSession session) {
			Assert.notNull(session, "Session must not be null");
			this.sockJsSession = session;
		}

		@Override
		public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
			Assert.state(this.connected.compareAndSet(false, true), "Already connected");
			this.sockJsSession.initializeDelegateSession(webSocketSession);
		}

		@Override
		public void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws Exception {
			this.sockJsSession.handleFrame(message.getPayload());
		}

		@Override
		public void handleTransportError(WebSocketSession webSocketSession, Throwable ex) throws Exception {
			this.sockJsSession.handleTransportError(ex);
		}

		@Override
		public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) throws Exception {
			this.sockJsSession.afterTransportClosed(status);
		}
	}

}
