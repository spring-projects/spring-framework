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
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.PongMessage;
import javax.websocket.Session;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;

/**
 *  {@link Endpoint} delegating events
 * to a reactive {@link WebSocketHandler} and its session.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
public class StandardEndpoint extends Endpoint {

	private final WebSocketHandler handler;

	private final HandshakeInfo info;

	private final DataBufferFactory bufferFactory;

	private StandardWebSocketSession delegateSession;

	private final MonoProcessor<Void> completionMono;


	public StandardEndpoint(WebSocketHandler handler, HandshakeInfo info,
			DataBufferFactory bufferFactory) {
		this(handler, info, bufferFactory, null);
	}

	public StandardEndpoint(WebSocketHandler handler, HandshakeInfo info,
			DataBufferFactory bufferFactory, MonoProcessor<Void> completionMono) {
		this.handler = handler;
		this.info = info;
		this.bufferFactory = bufferFactory;
		this.completionMono = completionMono;
	}


	@Override
	public void onOpen(Session nativeSession, EndpointConfig config) {

		this.delegateSession = new StandardWebSocketSession(nativeSession, this.info, this.bufferFactory);

		nativeSession.addMessageHandler(String.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});
		nativeSession.addMessageHandler(ByteBuffer.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});
		nativeSession.addMessageHandler(PongMessage.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.delegateSession.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});

		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber();
		this.handler.handle(this.delegateSession).subscribe(resultSubscriber);
	}

	private <T> WebSocketMessage toMessage(T message) {
		if (message instanceof String) {
			byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
			return new WebSocketMessage(Type.TEXT, this.bufferFactory.wrap(bytes));
		}
		else if (message instanceof ByteBuffer) {
			DataBuffer buffer = this.bufferFactory.wrap((ByteBuffer) message);
			return new WebSocketMessage(Type.BINARY, buffer);
		}
		else if (message instanceof PongMessage) {
			DataBuffer buffer = this.bufferFactory.wrap(((PongMessage) message).getApplicationData());
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
			this.delegateSession.handleClose(new CloseStatus(code, reason.getReasonPhrase()));
		}
	}

	@Override
	public void onError(Session session, Throwable exception) {
		if (this.delegateSession != null) {
			this.delegateSession.handleError(exception);
		}
	}

	protected HandshakeInfo getHandshakeInfo() {
		return this.info;
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