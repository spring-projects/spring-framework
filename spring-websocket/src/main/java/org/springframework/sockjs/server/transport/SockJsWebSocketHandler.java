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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.sockjs.AbstractSockJsSession;
import org.springframework.sockjs.server.AbstractServerSockJsSession;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.SockJsFrame;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.TextMessageHandler;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A wrapper around a {@link WebSocketHandler} instance that parses and adds SockJS
 * messages frames as well as sends SockJS heartbeat messages.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsWebSocketHandler implements TextMessageHandler {

	private static final Log logger = LogFactory.getLog(SockJsWebSocketHandler.class);

	private final SockJsConfiguration sockJsConfig;

	private final HandlerProvider<WebSocketHandler> handlerProvider;

	private AbstractSockJsSession session;

	// TODO: JSON library used must be configurable
	private final ObjectMapper objectMapper = new ObjectMapper();


	public SockJsWebSocketHandler(SockJsConfiguration config, HandlerProvider<WebSocketHandler> handlerProvider) {
		Assert.notNull(config, "sockJsConfig is required");
		Assert.notNull(handlerProvider, "handlerProvider is required");
		this.sockJsConfig = config;
		this.handlerProvider = handlerProvider;
	}

	protected SockJsConfiguration getSockJsConfig() {
		return this.sockJsConfig;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
		this.session = new WebSocketServerSockJsSession(wsSession, getSockJsConfig());
	}

	@Override
	public void handleTextMessage(TextMessage message, WebSocketSession wsSession) throws Exception {
		String payload = message.getPayload();
		if (StringUtils.isEmpty(payload)) {
			logger.trace("Ignoring empty message");
			return;
		}
		try {
			String[] messages = this.objectMapper.readValue(payload, String[].class);
			this.session.delegateMessages(messages);
		}
		catch (IOException e) {
			logger.error("Broken data received. Terminating WebSocket connection abruptly", e);
			wsSession.close();
		}
	}

	@Override
	public void afterConnectionClosed(CloseStatus status, WebSocketSession wsSession) throws Exception {
		this.session.delegateConnectionClosed(status);
	}

	@Override
	public void handleError(Throwable exception, WebSocketSession webSocketSession) {
		this.session.delegateError(exception);
	}

	private static String getSockJsSessionId(WebSocketSession wsSession) {
		Assert.notNull(wsSession, "wsSession is required");
		String path = wsSession.getURI().getPath();
		String[] segments = StringUtils.tokenizeToStringArray(path, "/");
		Assert.isTrue(segments.length > 3, "SockJS request should have at least 3 path segments: " + path);
		return segments[segments.length-2];
	}


	private class WebSocketServerSockJsSession extends AbstractServerSockJsSession {

		private final WebSocketSession wsSession;


		public WebSocketServerSockJsSession(WebSocketSession wsSession, SockJsConfiguration sockJsConfig)
				throws Exception {

			super(getSockJsSessionId(wsSession), sockJsConfig, SockJsWebSocketHandler.this.handlerProvider);
			this.wsSession = wsSession;
			TextMessage message = new TextMessage(SockJsFrame.openFrame().getContent());
			this.wsSession.sendMessage(message);
			scheduleHeartbeat();
			delegateConnectionEstablished();
		}

		@Override
		public boolean isActive() {
			return this.wsSession.isOpen();
		}

		@Override
		public void sendMessageInternal(String message) throws Exception {
			cancelHeartbeat();
			writeFrame(SockJsFrame.messageFrame(message));
			scheduleHeartbeat();
		}

		@Override
		protected void writeFrameInternal(SockJsFrame frame) throws Exception {
			if (logger.isTraceEnabled()) {
				logger.trace("Write " + frame);
			}
			TextMessage message = new TextMessage(frame.getContent());
			this.wsSession.sendMessage(message);
		}

		@Override
		protected void disconnect(CloseStatus status) throws Exception {
			this.wsSession.close(status);
		}
	}

}
