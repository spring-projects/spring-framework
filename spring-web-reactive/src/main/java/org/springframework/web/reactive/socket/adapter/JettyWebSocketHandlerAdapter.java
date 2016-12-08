/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.socket.adapter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.common.OpCode;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;

/**
 * Jetty {@code WebSocketHandler} implementation adapting and
 * delegating to a Spring {@link WebSocketHandler}.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
@WebSocket
public class JettyWebSocketHandlerAdapter {

	private static final ByteBuffer EMPTY_PAYLOAD = ByteBuffer.wrap(new byte[0]);

	private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory(false);

	private final WebSocketHandler handler;

	private JettyWebSocketSession wsSession;

	public JettyWebSocketHandlerAdapter(WebSocketHandler handler) {
		Assert.notNull("'handler' is required");
		this.handler = handler;
	}

	@OnWebSocketConnect
	public void onWebSocketConnect(Session session) {
		this.wsSession = new JettyWebSocketSession(session);

		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber();
		this.handler.handle(this.wsSession).subscribe(resultSubscriber);
	}

	@OnWebSocketMessage
	public void onWebSocketText(String message) {
		if (this.wsSession != null) {
			WebSocketMessage wsMessage = toMessage(Type.TEXT, message);
			this.wsSession.handleMessage(wsMessage.getType(), wsMessage);
		}
	}

	@OnWebSocketMessage
	public void onWebSocketBinary(byte[] message, int offset, int length) {
		if (this.wsSession != null) {
			WebSocketMessage wsMessage = toMessage(Type.BINARY, ByteBuffer.wrap(message, offset, length));
			wsSession.handleMessage(wsMessage.getType(), wsMessage);
		}
	}

	@OnWebSocketFrame
	public void onWebSocketFrame(Frame frame) {
		if (this.wsSession != null) {
			if (OpCode.PONG == frame.getOpCode()) {
				ByteBuffer message = frame.getPayload() != null ? frame.getPayload() : EMPTY_PAYLOAD;
				WebSocketMessage wsMessage = toMessage(Type.PONG, message);
				wsSession.handleMessage(wsMessage.getType(), wsMessage);
			}
		}
	}

	@OnWebSocketClose
	public void onWebSocketClose(int statusCode, String reason) {
		if (this.wsSession != null) {
			this.wsSession.handleClose(new CloseStatus(statusCode, reason));
		}
	}

	@OnWebSocketError
	public void onWebSocketError(Throwable cause) {
		if (this.wsSession != null) {
			this.wsSession.handleError(cause);
		}
	}

	private <T> WebSocketMessage toMessage(Type type, T message) {
		if (Type.TEXT.equals(type)) {
			return WebSocketMessage.create(Type.TEXT,
					bufferFactory.wrap(((String) message).getBytes(StandardCharsets.UTF_8)));
		}
		else if (Type.BINARY.equals(type)) {
			return WebSocketMessage.create(Type.BINARY,
					bufferFactory.wrap((ByteBuffer) message));
		}
		else if (Type.PONG.equals(type)) {
			return WebSocketMessage.create(Type.PONG,
					bufferFactory.wrap((ByteBuffer) message));
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message);
		}
	}

	private final class HandlerResultSubscriber implements Subscriber<Void> {

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Void aVoid) {
			// no op
		}

		@Override
		public void onError(Throwable ex) {
			if (wsSession != null) {
				wsSession.close(new CloseStatus(CloseStatus.SERVER_ERROR.getCode(), ex.getMessage()));
			}
		}

		@Override
		public void onComplete() {
			if (wsSession != null) {
				wsSession.close();
			}
		}
	}

}
