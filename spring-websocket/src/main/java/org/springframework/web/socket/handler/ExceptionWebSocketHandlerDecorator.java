/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * An exception handling {@link WebSocketHandlerDecorator}.
 * Traps all {@link Throwable} instances that escape from the decorated
 * handler and closes the session with {@link CloseStatus#SERVER_ERROR}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ExceptionWebSocketHandlerDecorator extends WebSocketHandlerDecorator {

	private static final Log logger = LogFactory.getLog(ExceptionWebSocketHandlerDecorator.class);


	public ExceptionWebSocketHandlerDecorator(WebSocketHandler delegate) {
		super(delegate);
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		try {
			getDelegate().afterConnectionEstablished(session);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex, logger);
		}
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
		try {
			getDelegate().handleMessage(session, message);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex, logger);
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		try {
			getDelegate().handleTransportError(session, exception);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex, logger);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
		try {
			getDelegate().afterConnectionClosed(session, closeStatus);
		}
		catch (Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Unhandled error for " + this, ex);
			}
		}
	}


	public static void tryCloseWithError(WebSocketSession session, Throwable exception, Log logger) {
		if (logger.isErrorEnabled()) {
			logger.error("Closing session due to exception for " + session, exception);
		}
		if (session.isOpen()) {
			try {
				session.close(CloseStatus.SERVER_ERROR);
			}
			catch (Throwable ex) {
				// ignore
			}
		}
	}

}
