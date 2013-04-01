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

package org.springframework.websocket.endpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.websocket.WebSocketSession;
import org.springframework.websocket.WebSocketHandler;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketHandlerAdapter extends Endpoint {

	private static Log logger = LogFactory.getLog(StandardWebSocketHandlerAdapter.class);

	private final WebSocketHandler webSocketHandler;

	private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<String, WebSocketSession>();


	public StandardWebSocketHandlerAdapter(WebSocketHandler webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
	}

	@Override
	public void onOpen(javax.websocket.Session session, EndpointConfig config) {
		logger.debug("New WebSocket session: " + session);
		try {
			WebSocketSession webSocketSession = new WebSocketStandardSessionAdapter(session);
			this.sessionMap.put(session.getId(), webSocketSession);
			session.addMessageHandler(new StandardMessageHandler(session.getId()));
			this.webSocketHandler.newSession(webSocketSession);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Error while processing new session", ex);
		}
	}

	@Override
	public void onClose(javax.websocket.Session session, CloseReason closeReason) {
		String id = session.getId();
		if (logger.isDebugEnabled()) {
			logger.debug("Closing session: " + session + ", " + closeReason);
		}
		try {
			WebSocketSession webSocketSession = getSession(id);
			this.sessionMap.remove(id);
			int code = closeReason.getCloseCode().getCode();
			String reason = closeReason.getReasonPhrase();
			webSocketSession.close(code, reason);
			this.webSocketHandler.sessionClosed(webSocketSession, code, reason);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Error while processing session closing", ex);
		}
	}

	@Override
	public void onError(javax.websocket.Session session, Throwable exception) {
		logger.error("Error for WebSocket session: " + session.getId(), exception);
		try {
			WebSocketSession webSocketSession = getSession(session.getId());
			this.webSocketHandler.handleException(webSocketSession, exception);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Failed to handle error", ex);
		}
	}

	private WebSocketSession getSession(String sourceSessionId) {
		WebSocketSession webSocketSession = this.sessionMap.get(sourceSessionId);
		Assert.notNull(webSocketSession, "No session");
		return webSocketSession;
	}


	private class StandardMessageHandler implements MessageHandler.Whole<String> {

		private final String sessionId;

		public StandardMessageHandler(String sessionId) {
			this.sessionId = sessionId;
		}

		@Override
		public void onMessage(String message) {
			if (logger.isTraceEnabled()) {
				logger.trace("Message for session [" + this.sessionId + "]: " + message);
			}
			try {
				WebSocketSession session = getSession(this.sessionId);
				StandardWebSocketHandlerAdapter.this.webSocketHandler.handleTextMessage(session, message);
			}
			catch (Throwable ex) {
				// TODO
				logger.error("Error while processing message", ex);
			}
		}

	}

}
