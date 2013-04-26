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

package org.springframework.sockjs.server.transport;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.sockjs.server.AbstractServerSockJsSession;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.SockJsFrame;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;
import org.springframework.websocket.adapter.TextWebSocketHandlerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A wrapper around a {@link WebSocketHandler} instance that parses as well as adds SockJS
 * messages frames as well as sends SockJS heartbeat messages.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsWebSocketHandler extends TextWebSocketHandlerAdapter {

	private final SockJsConfiguration sockJsConfig;

	private final HandlerProvider<WebSocketHandler<?>> handlerProvider;

	private WebSocketServerSockJsSession sockJsSession;

	private final AtomicInteger sessionCount = new AtomicInteger(0);

	// TODO: JSON library used must be configurable
	private final ObjectMapper objectMapper = new ObjectMapper();


	public SockJsWebSocketHandler(SockJsConfiguration config, HandlerProvider<WebSocketHandler<?>> handlerProvider) {
		Assert.notNull(config, "sockJsConfig is required");
		Assert.notNull(handlerProvider, "handlerProvider is required");
		this.sockJsConfig = config;
		this.handlerProvider = handlerProvider;
	}

	protected SockJsConfiguration getSockJsConfig() {
		return this.sockJsConfig;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession wsSession) {
		Assert.isTrue(this.sessionCount.compareAndSet(0, 1), "Unexpected connection");
		this.sockJsSession = new WebSocketServerSockJsSession(getSockJsSessionId(wsSession), getSockJsConfig());
		this.sockJsSession.initWebSocketSession(wsSession);
	}

	@Override
	public void handleMessage(WebSocketSession wsSession, TextMessage message) {
		this.sockJsSession.handleMessage(message, wsSession);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) {
		this.sockJsSession.delegateConnectionClosed(status);
	}

	@Override
	public void handleTransportError(WebSocketSession webSocketSession, Throwable exception) {
		this.sockJsSession.delegateError(exception);
	}

	private static String getSockJsSessionId(WebSocketSession wsSession) {
		Assert.notNull(wsSession, "wsSession is required");
		String path = wsSession.getURI().getPath();
		String[] segments = StringUtils.tokenizeToStringArray(path, "/");
		Assert.isTrue(segments.length > 3, "SockJS request should have at least 3 path segments: " + path);
		return segments[segments.length-2];
	}


	private class WebSocketServerSockJsSession extends AbstractServerSockJsSession {

		private WebSocketSession wsSession;


		public WebSocketServerSockJsSession(String sessionId, SockJsConfiguration config) {
			super(sessionId, config, SockJsWebSocketHandler.this.handlerProvider);
		}

		public void initWebSocketSession(WebSocketSession wsSession) {
			this.wsSession = wsSession;
			try {
				TextMessage message = new TextMessage(SockJsFrame.openFrame().getContent());
				this.wsSession.sendMessage(message);
			}
			catch (IOException ex) {
				tryCloseWithSockJsTransportError(ex, null);
				return;
			}
			scheduleHeartbeat();
			delegateConnectionEstablished();
		}

		@Override
		public boolean isActive() {
			return this.wsSession.isOpen();
		}

		public void handleMessage(TextMessage message, WebSocketSession wsSession) {
			String payload = message.getPayload();
			if (StringUtils.isEmpty(payload)) {
				logger.trace("Ignoring empty message");
				return;
			}
			String[] messages;
			try {
				messages = objectMapper.readValue(payload, String[].class);
			}
			catch (IOException ex) {
				logger.error("Broken data received. Terminating WebSocket connection abruptly", ex);
				tryCloseWithSockJsTransportError(ex, CloseStatus.BAD_DATA);
				return;
			}
			delegateMessages(messages);
		}

		@Override
		public void sendMessageInternal(String message) throws IOException {
			cancelHeartbeat();
			writeFrame(SockJsFrame.messageFrame(message));
			scheduleHeartbeat();
		}

		@Override
		protected void writeFrameInternal(SockJsFrame frame) throws IOException {
			if (logger.isTraceEnabled()) {
				logger.trace("Write " + frame);
			}
			TextMessage message = new TextMessage(frame.getContent());
			this.wsSession.sendMessage(message);
		}

		@Override
		protected void disconnect(CloseStatus status) throws IOException {
			this.wsSession.close(status);
		}
	}

}
