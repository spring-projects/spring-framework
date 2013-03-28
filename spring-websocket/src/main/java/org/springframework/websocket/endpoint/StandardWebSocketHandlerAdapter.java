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
import org.springframework.websocket.Session;
import org.springframework.websocket.WebSocketHandler;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketHandlerAdapter extends Endpoint {

	private static Log logger = LogFactory.getLog(StandardWebSocketHandlerAdapter.class);

	private final WebSocketHandler webSocketHandler;

	private final Map<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();


	public StandardWebSocketHandlerAdapter(WebSocketHandler webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
	}

	@Override
	public void onOpen(javax.websocket.Session sourceSession, EndpointConfig config) {
		logger.debug("New WebSocket session: " + sourceSession);
		try {
			Session session = new StandardSessionAdapter(sourceSession);
			this.sessionMap.put(sourceSession.getId(), session);
			sourceSession.addMessageHandler(new StandardMessageHandler(sourceSession.getId()));
			this.webSocketHandler.newSession(session);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Error while processing new session", ex);
		}
	}

	@Override
	public void onClose(javax.websocket.Session sourceSession, CloseReason closeReason) {
		String id = sourceSession.getId();
		if (logger.isDebugEnabled()) {
			logger.debug("Closing session: " + sourceSession + ", " + closeReason);
		}
		try {
			Session session = getSession(id);
			this.sessionMap.remove(id);
			int code = closeReason.getCloseCode().getCode();
			String reason = closeReason.getReasonPhrase();
			session.close(code, reason);
			this.webSocketHandler.sessionClosed(session, code, reason);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Error while processing session closing", ex);
		}
	}

	@Override
	public void onError(javax.websocket.Session sourceSession, Throwable exception) {
		logger.error("Error for WebSocket session: " + sourceSession.getId(), exception);
		try {
			Session session = getSession(sourceSession.getId());
			this.webSocketHandler.handleException(session, exception);
		}
		catch (Throwable ex) {
			// TODO
			logger.error("Failed to handle error", ex);
		}
	}

	private Session getSession(String sourceSessionId) {
		Session session = this.sessionMap.get(sourceSessionId);
		Assert.notNull(session, "No session");
		return session;
	}


	private class StandardMessageHandler implements MessageHandler.Whole<String> {

		private final String sourceSessionId;

		public StandardMessageHandler(String sourceSessionId) {
			this.sourceSessionId = sourceSessionId;
		}

		@Override
		public void onMessage(String message) {
			if (logger.isTraceEnabled()) {
				logger.trace("Message for session [" + this.sourceSessionId + "]: " + message);
			}
			try {
				Session session = getSession(this.sourceSessionId);
				StandardWebSocketHandlerAdapter.this.webSocketHandler.handleTextMessage(session, message);
			}
			catch (Throwable ex) {
				// TODO
				logger.error("Error while processing message", ex);
			}
		}

	}

}
