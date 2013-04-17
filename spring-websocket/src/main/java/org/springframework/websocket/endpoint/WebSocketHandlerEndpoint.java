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
 * An {@link Endpoint} that delegates to a {@link WebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHandlerEndpoint extends Endpoint {

	private static Log logger = LogFactory.getLog(WebSocketHandlerEndpoint.class);

	private final WebSocketHandler webSocketHandler;

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<String, WebSocketSession>();


	public WebSocketHandlerEndpoint(WebSocketHandler webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
	}

	@Override
	public void onOpen(javax.websocket.Session session, EndpointConfig config) {
		if (logger.isDebugEnabled()) {
			logger.debug("New session: " + session);
		}
		try {
			WebSocketSession webSocketSession = new StandardWebSocketSession(session);
			this.sessions.put(session.getId(), webSocketSession);
			session.addMessageHandler(new StandardMessageHandler(session));
			this.webSocketHandler.newSession(webSocketSession);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Error while processing new session", ex);
		}
	}

	@Override
	public void onClose(javax.websocket.Session session, CloseReason closeReason) {
		if (logger.isDebugEnabled()) {
			logger.debug("Session closed: " + session + ", " + closeReason);
		}
		try {
			WebSocketSession wsSession = this.sessions.remove(session.getId());
			if (wsSession != null) {
				int code = closeReason.getCloseCode().getCode();
				String reason = closeReason.getReasonPhrase();
				this.webSocketHandler.sessionClosed(wsSession, code, reason);
			}
			else {
				Assert.notNull(wsSession, "No WebSocket session");
			}
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
			WebSocketSession wsSession = getWebSocketSession(session);
			if (wsSession != null) {
				this.webSocketHandler.handleException(wsSession, exception);
			}
			else {
				logger.warn("WebSocketSession not found. Perhaps onError was called after onClose?");
			}
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Failed to handle error", ex);
		}
	}

	private WebSocketSession getWebSocketSession(javax.websocket.Session session) {
		return this.sessions.get(session.getId());
	}


	private class StandardMessageHandler implements MessageHandler.Whole<String> {

		private final javax.websocket.Session session;

		public StandardMessageHandler(javax.websocket.Session session) {
			this.session = session;
		}

		@Override
		public void onMessage(String message) {
			if (logger.isTraceEnabled()) {
				logger.trace("Message for session [" + this.session + "]: " + message);
			}
			WebSocketSession wsSession = getWebSocketSession(this.session);
			Assert.notNull(wsSession, "WebSocketSession not found");
			try {
				WebSocketHandlerEndpoint.this.webSocketHandler.handleTextMessage(wsSession, message);
			}
			catch (Throwable ex) {
				// TODO
				logger.error("Error while processing message", ex);
			}
		}

	}

}
