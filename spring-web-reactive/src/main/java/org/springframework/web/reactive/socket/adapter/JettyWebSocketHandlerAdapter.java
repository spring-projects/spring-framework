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
import java.util.function.Function;

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
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Jetty {@link WebSocket @WebSocket} handler that delegates events to a
 * reactive {@link WebSocketHandler} and its session.
 * 
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@WebSocket
public class JettyWebSocketHandlerAdapter {

	private static final ByteBuffer EMPTY_PAYLOAD = ByteBuffer.wrap(new byte[0]);


	private final WebSocketHandler delegateHandler;

	private final MonoProcessor<Void> completionMono;

	private final Function<Session, JettyWebSocketSession> sessionFactory;

	private JettyWebSocketSession delegateSession;


	public JettyWebSocketHandlerAdapter(WebSocketHandler handler,
			Function<Session, JettyWebSocketSession> sessionFactory) {

		this(handler, null, sessionFactory);
	}

	public JettyWebSocketHandlerAdapter(WebSocketHandler handler, MonoProcessor<Void> completionMono,
			Function<Session, JettyWebSocketSession> sessionFactory) {

		Assert.notNull("WebSocketHandler is required");
		Assert.notNull("'sessionFactory' is required");
		this.delegateHandler = handler;
		this.completionMono = completionMono;
		this.sessionFactory = sessionFactory;
	}

	@OnWebSocketConnect
	public void onWebSocketConnect(Session session) {
		this.delegateSession = sessionFactory.apply(session);
		HandlerResultSubscriber subscriber = new HandlerResultSubscriber();
		this.delegateHandler.handle(this.delegateSession).subscribe(subscriber);
	}

	@OnWebSocketMessage
	public void onWebSocketText(String message) {
		if (this.delegateSession != null) {
			WebSocketMessage webSocketMessage = toMessage(Type.TEXT, message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		}
	}

	@OnWebSocketMessage
	public void onWebSocketBinary(byte[] message, int offset, int length) {
		if (this.delegateSession != null) {
			ByteBuffer buffer = ByteBuffer.wrap(message, offset, length);
			WebSocketMessage webSocketMessage = toMessage(Type.BINARY, buffer);
			delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		}
	}

	@OnWebSocketFrame
	public void onWebSocketFrame(Frame frame) {
		if (this.delegateSession != null) {
			if (OpCode.PONG == frame.getOpCode()) {
				ByteBuffer buffer = (frame.getPayload() != null ? frame.getPayload() : EMPTY_PAYLOAD);
				WebSocketMessage webSocketMessage = toMessage(Type.PONG, buffer);
				delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
			}
		}
	}

	private <T> WebSocketMessage toMessage(Type type, T message) {
		WebSocketSession session = this.delegateSession;
		Assert.state(session != null, "Cannot create message without a session");
		if (Type.TEXT.equals(type)) {
			byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
			DataBuffer buffer = session.bufferFactory().wrap(bytes);
			return new WebSocketMessage(Type.TEXT, buffer);
		}
		else if (Type.BINARY.equals(type)) {
			DataBuffer buffer = session.bufferFactory().wrap((ByteBuffer) message);
			return new WebSocketMessage(Type.BINARY, buffer);
		}
		else if (Type.PONG.equals(type)) {
			DataBuffer buffer = session.bufferFactory().wrap((ByteBuffer) message);
			return new WebSocketMessage(Type.PONG, buffer);
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message);
		}
	}

	@OnWebSocketClose
	public void onWebSocketClose(int statusCode, String reason) {
		if (this.delegateSession != null) {
			this.delegateSession.handleClose(new CloseStatus(statusCode, reason));
		}
	}

	@OnWebSocketError
	public void onWebSocketError(Throwable cause) {
		if (this.delegateSession != null) {
			this.delegateSession.handleError(cause);
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
			if (completionMono != null) {
				completionMono.onError(ex);
			}
			if (delegateSession != null) {
				int code = CloseStatus.SERVER_ERROR.getCode();
				delegateSession.close(new CloseStatus(code, ex.getMessage()));
			}
		}

		@Override
		public void onComplete() {
			if (completionMono != null) {
				completionMono.onComplete();
			}
			if (delegateSession != null) {
				delegateSession.close();
			}
		}
	}

}
