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

package org.springframework.sockjs.server;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * An implementation of {@link WebSocketHandler} supporting the SockJS protocol.
 * Methods merely delegate to a {@link StandardWebSocketServerSession}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsWebSocketHandler implements WebSocketHandler {

	private static final Log logger = LogFactory.getLog(SockJsWebSocketHandler.class);

	private final StandardWebSocketServerSession sockJsSession;

	// TODO: the JSON library used must be configurable
	private final ObjectMapper objectMapper = new ObjectMapper();


	public SockJsWebSocketHandler(StandardWebSocketServerSession sockJsSession) {
		this.sockJsSession = sockJsSession;
	}

	@Override
	public void newSession(WebSocketSession webSocketSession) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("New session: " + webSocketSession);
		}
		this.sockJsSession.setWebSocketSession(webSocketSession);
	}

	@Override
	public void handleTextMessage(WebSocketSession session, String message) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Received payload " + message + " for " + sockJsSession);
		}
		if (StringUtils.isEmpty(message)) {
			logger.trace("Ignoring empty payload");
			return;
		}
		try {
			String[] messages = this.objectMapper.readValue(message, String[].class);
			this.sockJsSession.delegateMessages(messages);
		}
		catch (IOException e) {
			logger.error("Broken data received. Terminating WebSocket connection abruptly", e);
			session.close();
		}
	}

	@Override
	public void handleBinaryMessage(WebSocketSession session, InputStream message) throws Exception {
		// should not happen
		throw new UnsupportedOperationException();
	}

	@Override
	public void handleException(WebSocketSession session, Throwable exception) {
		exception.printStackTrace();
	}

	@Override
	public void sessionClosed(WebSocketSession session, int statusCode, String reason) throws Exception {
		logger.debug("WebSocket connection closed for " + this.sockJsSession);
		this.sockJsSession.close();
	}

}
