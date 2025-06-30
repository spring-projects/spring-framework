/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.socket.adapter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import org.jspecify.annotations.Nullable;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Adapter for the Jakarta WebSocket API (JSR-356) that delegates events to a
 * reactive {@link WebSocketHandler} and its session.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class StandardWebSocketHandlerAdapter extends Endpoint {

	private final WebSocketHandler delegateHandler;

	private final Function<Session, StandardWebSocketSession> sessionFactory;

	private @Nullable StandardWebSocketSession delegateSession;


	public StandardWebSocketHandlerAdapter(WebSocketHandler handler,
			Function<Session, StandardWebSocketSession> sessionFactory) {

		Assert.notNull(handler, "WebSocketHandler is required");
		Assert.notNull(sessionFactory, "'sessionFactory' is required");
		this.delegateHandler = handler;
		this.sessionFactory = sessionFactory;
	}


	@Override
	@SuppressWarnings("NullAway") // Lambda
	public void onOpen(Session session, EndpointConfig config) {
		this.delegateSession = this.sessionFactory.apply(session);
		Assert.state(this.delegateSession != null, "No delegate session");

		session.addMessageHandler(String.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});
		session.addMessageHandler(ByteBuffer.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});
		session.addMessageHandler(PongMessage.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});

		this.delegateHandler.handle(this.delegateSession)
				.checkpoint(session.getRequestURI() + " [StandardWebSocketHandlerAdapter]")
				.subscribe(this.delegateSession);
	}

	private <T> WebSocketMessage toMessage(T message) {
		WebSocketSession session = this.delegateSession;
		Assert.state(session != null, "Cannot create message without a session");
		if (message instanceof String text) {
			byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
			return new WebSocketMessage(Type.TEXT, session.bufferFactory().wrap(bytes));
		}
		else if (message instanceof ByteBuffer byteBuffer) {
			DataBuffer buffer = session.bufferFactory().wrap(byteBuffer);
			return new WebSocketMessage(Type.BINARY, buffer);
		}
		else if (message instanceof PongMessage pongMessage) {
			DataBuffer buffer = session.bufferFactory().wrap(pongMessage.getApplicationData());
			return new WebSocketMessage(Type.PONG, buffer);
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message);
		}
	}

	@Override
	public void onClose(Session session, CloseReason reason) {
		if (this.delegateSession != null) {
			int code = reason.getCloseCode().getCode();
			this.delegateSession.handleClose(CloseStatus.create(code, reason.getReasonPhrase()));
		}
	}

	@Override
	public void onError(Session session, Throwable exception) {
		if (this.delegateSession != null) {
			this.delegateSession.handleError(exception);
		}
	}

}
