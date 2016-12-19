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

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
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
public class JettyWebSocketHandlerAdapter extends WebSocketHandlerAdapterSupport {

	private static final ByteBuffer EMPTY_PAYLOAD = ByteBuffer.wrap(new byte[0]);

	private JettyWebSocketSession session;


	public JettyWebSocketHandlerAdapter(HandshakeInfo handshakeInfo, DataBufferFactory bufferFactory,
			WebSocketHandler delegate) {

		super(handshakeInfo, bufferFactory, delegate);
	}


	@OnWebSocketConnect
	public void onWebSocketConnect(Session session) {
		this.session = new JettyWebSocketSession(session, getHandshakeInfo(), getBufferFactory());
		HandlerResultSubscriber subscriber = new HandlerResultSubscriber();
		getDelegate().handle(this.session).subscribe(subscriber);
	}

	@OnWebSocketMessage
	public void onWebSocketText(String message) {
		if (this.session != null) {
			WebSocketMessage webSocketMessage = toMessage(Type.TEXT, message);
			this.session.handleMessage(webSocketMessage.getType(), webSocketMessage);
		}
	}

	@OnWebSocketMessage
	public void onWebSocketBinary(byte[] message, int offset, int length) {
		if (this.session != null) {
			ByteBuffer buffer = ByteBuffer.wrap(message, offset, length);
			WebSocketMessage webSocketMessage = toMessage(Type.BINARY, buffer);
			session.handleMessage(webSocketMessage.getType(), webSocketMessage);
		}
	}

	@OnWebSocketFrame
	public void onWebSocketFrame(Frame frame) {
		if (this.session != null) {
			if (OpCode.PONG == frame.getOpCode()) {
				ByteBuffer buffer = (frame.getPayload() != null ? frame.getPayload() : EMPTY_PAYLOAD);
				WebSocketMessage webSocketMessage = toMessage(Type.PONG, buffer);
				session.handleMessage(webSocketMessage.getType(), webSocketMessage);
			}
		}
	}

	private <T> WebSocketMessage toMessage(Type type, T message) {
		if (Type.TEXT.equals(type)) {
			byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
			DataBuffer buffer = getBufferFactory().wrap(bytes);
			return new WebSocketMessage(Type.TEXT, buffer);
		}
		else if (Type.BINARY.equals(type)) {
			DataBuffer buffer = getBufferFactory().wrap((ByteBuffer) message);
			return new WebSocketMessage(Type.BINARY, buffer);
		}
		else if (Type.PONG.equals(type)) {
			DataBuffer buffer = getBufferFactory().wrap((ByteBuffer) message);
			return new WebSocketMessage(Type.PONG, buffer);
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message);
		}
	}

	@OnWebSocketClose
	public void onWebSocketClose(int statusCode, String reason) {
		if (this.session != null) {
			this.session.handleClose(new CloseStatus(statusCode, reason));
		}
	}

	@OnWebSocketError
	public void onWebSocketError(Throwable cause) {
		if (this.session != null) {
			this.session.handleError(cause);
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
			if (session != null) {
				session.close(new CloseStatus(CloseStatus.SERVER_ERROR.getCode(), ex.getMessage()));
			}
		}

		@Override
		public void onComplete() {
			if (session != null) {
				session.close();
			}
		}
	}

}
