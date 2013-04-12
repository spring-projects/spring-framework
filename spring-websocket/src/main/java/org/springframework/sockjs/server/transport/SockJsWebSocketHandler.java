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

import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.server.AbstractServerSession;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.SockJsFrame;
import org.springframework.util.StringUtils;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A SockJS implementation of {@link WebSocketHandler}. Delegates messages to and from a
 * {@link SockJsHandler} and adds SockJS message framing.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsWebSocketHandler extends AbstractSockJsWebSocketHandler {

	// TODO: JSON library used must be configurable
	private final ObjectMapper objectMapper = new ObjectMapper();


	public SockJsWebSocketHandler(SockJsConfiguration sockJsConfig, SockJsHandler sockJsHandler) {
		super(sockJsConfig, sockJsHandler);
	}

	@Override
	protected SockJsSessionSupport createSockJsSession(WebSocketSession wsSession) throws Exception {
		return new WebSocketServerSession(wsSession, getSockJsConfig());
	}

	@Override
	public void handleTextMessage(WebSocketSession wsSession, String message) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Received payload " + message + " for " + wsSession);
		}
		if (StringUtils.isEmpty(message)) {
			logger.trace("Ignoring empty payload");
			return;
		}
		try {
			String[] messages = this.objectMapper.readValue(message, String[].class);
			SockJsSessionSupport session = getSockJsSession(wsSession);
			session.delegateMessages(messages);
		}
		catch (IOException e) {
			logger.error("Broken data received. Terminating WebSocket connection abruptly", e);
			wsSession.close();
		}
	}


	private class WebSocketServerSession extends AbstractServerSession {

		private WebSocketSession webSocketSession;


		public WebSocketServerSession(WebSocketSession wsSession, SockJsConfiguration sockJsConfig) throws Exception {
			super(String.valueOf(wsSession.hashCode()), sockJsConfig, getSockJsHandler());
			this.webSocketSession = wsSession;
			this.webSocketSession.sendText(SockJsFrame.openFrame().getContent());
			scheduleHeartbeat();
			connectionInitialized();
		}

		@Override
		public boolean isActive() {
			return ((this.webSocketSession != null) && this.webSocketSession.isOpen());
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
			this.webSocketSession.sendText(frame.getContent());
		}

		@Override
		public void closeInternal() {
			this.webSocketSession.close();
			this.webSocketSession = null;
			updateLastActiveTime();
		}

		@Override
		protected void deactivate() {
			this.webSocketSession.close();
		}
	}

}
