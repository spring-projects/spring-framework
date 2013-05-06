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

package org.springframework.web.socket.sockjs.transport;

import java.io.IOException;

import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.AbstractServerSockJsSession;
import org.springframework.web.socket.sockjs.SockJsConfiguration;
import org.springframework.web.socket.sockjs.SockJsFrame;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketServerSockJsSession extends AbstractServerSockJsSession {

	private WebSocketSession webSocketSession;

	// TODO: JSON library used must be configurable
	private final ObjectMapper objectMapper = new ObjectMapper();


	public WebSocketServerSockJsSession(String sessionId, SockJsConfiguration config, WebSocketHandler handler) {
		super(sessionId, config, handler);
	}

	public void initWebSocketSession(WebSocketSession session) throws Exception {
		this.webSocketSession = session;
		try {
			TextMessage message = new TextMessage(SockJsFrame.openFrame().getContent());
			this.webSocketSession.sendMessage(message);
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
		return ((this.webSocketSession != null) && this.webSocketSession.isOpen());
	}

	public void handleMessage(TextMessage message, WebSocketSession wsSession) throws Exception {
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
		this.webSocketSession.sendMessage(message);
	}

	@Override
	protected void disconnect(CloseStatus status) throws IOException {
		this.webSocketSession.close(status);
	}
}

