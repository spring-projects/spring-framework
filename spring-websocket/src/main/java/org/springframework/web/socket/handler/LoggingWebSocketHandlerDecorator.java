/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * A {@link WebSocketHandlerDecorator} that adds logging to WebSocket lifecycle events.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class LoggingWebSocketHandlerDecorator extends WebSocketHandlerDecorator {

	private static final Log logger = LogFactory.getLog(LoggingWebSocketHandlerDecorator.class);


	public LoggingWebSocketHandlerDecorator(WebSocketHandler delegate) {
		super(delegate);
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("New " + session);
		}
		super.afterConnectionEstablished(session);
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Handling " + message + " in " + session);
		}
		super.handleMessage(session, message);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Transport error in " + session, exception);
		}
		super.handleTransportError(session, exception);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(session + " closed with " + closeStatus);
		}
		super.afterConnectionClosed(session, closeStatus);
	}

}
