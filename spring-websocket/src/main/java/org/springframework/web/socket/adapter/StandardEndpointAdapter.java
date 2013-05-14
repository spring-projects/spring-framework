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

package org.springframework.web.socket.adapter;

import java.nio.ByteBuffer;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.support.ExceptionWebSocketHandlerDecorator;


/**
 * A wrapper around a {@link WebSocketHandler} that adapts it to {@link Endpoint}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardEndpointAdapter extends Endpoint {

	private static final Log logger = LogFactory.getLog(StandardEndpointAdapter.class);

	private final WebSocketHandler handler;

	private final StandardWebSocketSessionAdapter wsSession;


	public StandardEndpointAdapter(WebSocketHandler handler, StandardWebSocketSessionAdapter wsSession) {
		Assert.notNull(handler, "handler is required");
		Assert.notNull(wsSession, "wsSession is required");
		this.handler = handler;
		this.wsSession = wsSession;
	}


	@Override
	public void onOpen(final javax.websocket.Session session, EndpointConfig config) {

		this.wsSession.initSession(session);

		try {
			this.handler.afterConnectionEstablished(this.wsSession);
		}
		catch (Throwable t) {
			ExceptionWebSocketHandlerDecorator.tryCloseWithError(this.wsSession, t, logger);
			return;
		}

		session.addMessageHandler(new MessageHandler.Whole<String>() {
			@Override
			public void onMessage(String message) {
				handleTextMessage(session, message);
			}
		});
		session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
			@Override
			public void onMessage(ByteBuffer message) {
				handleBinaryMessage(session, message);
			}
		});
	}

	private void handleTextMessage(javax.websocket.Session session, String payload) {
		TextMessage textMessage = new TextMessage(payload);
		try {
			this.handler.handleMessage(this.wsSession, textMessage);
		}
		catch (Throwable t) {
			ExceptionWebSocketHandlerDecorator.tryCloseWithError(this.wsSession, t, logger);
		}
	}

	private void handleBinaryMessage(javax.websocket.Session session, ByteBuffer payload) {
		BinaryMessage binaryMessage = new BinaryMessage(payload);
		try {
			this.handler.handleMessage(this.wsSession, binaryMessage);
		}
		catch (Throwable t) {
			ExceptionWebSocketHandlerDecorator.tryCloseWithError(this.wsSession, t, logger);
		}
	}

	@Override
	public void onClose(javax.websocket.Session session, CloseReason reason) {
		CloseStatus closeStatus = new CloseStatus(reason.getCloseCode().getCode(), reason.getReasonPhrase());
		try {
			this.handler.afterConnectionClosed(this.wsSession, closeStatus);
		}
		catch (Throwable t) {
			logger.error("Unhandled error for " + this.wsSession, t);
		}
	}

	@Override
	public void onError(javax.websocket.Session session, Throwable exception) {
		try {
			this.handler.handleTransportError(this.wsSession, exception);
		}
		catch (Throwable t) {
			ExceptionWebSocketHandlerDecorator.tryCloseWithError(this.wsSession, t, logger);
		}
	}

}
