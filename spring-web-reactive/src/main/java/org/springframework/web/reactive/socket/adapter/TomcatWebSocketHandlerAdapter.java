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

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;

/**
 * Tomcat {@code WebSocketHandler} implementation adapting and
 * delegating to a Spring {@link WebSocketHandler}.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
public class TomcatWebSocketHandlerAdapter extends Endpoint {

	private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory(false);

	private final WebSocketHandler delegate;

	private TomcatWebSocketSession session;


	public TomcatWebSocketHandlerAdapter(WebSocketHandler delegate) {
		Assert.notNull("WebSocketHandler is required");
		this.delegate = delegate;
	}


	@Override
	public void onOpen(Session session, EndpointConfig config) {
		this.session = new TomcatWebSocketSession(session);

		session.addMessageHandler(String.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.session.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});
		session.addMessageHandler(ByteBuffer.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.session.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});
		session.addMessageHandler(PongMessage.class, message -> {
			WebSocketMessage webSocketMessage = toMessage(message);
			this.session.handleMessage(webSocketMessage.getType(), webSocketMessage);
		});

		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber();
		this.delegate.handle(this.session).subscribe(resultSubscriber);
	}

	@Override
	public void onClose(Session session, CloseReason reason) {
		if (this.session != null) {
			int code = reason.getCloseCode().getCode();
			this.session.handleClose(new CloseStatus(code, reason.getReasonPhrase()));
		}
	}

	@Override
	public void onError(Session session, Throwable exception) {
		if (this.session != null) {
			this.session.handleError(exception);
		}
	}

	private <T> WebSocketMessage toMessage(T message) {
		if (message instanceof String) {
			byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
			return WebSocketMessage.create(Type.TEXT, this.bufferFactory.wrap(bytes));
		}
		else if (message instanceof ByteBuffer) {
			DataBuffer buffer = this.bufferFactory.wrap((ByteBuffer) message);
			return WebSocketMessage.create(Type.BINARY, buffer);
		}
		else if (message instanceof PongMessage) {
			DataBuffer buffer = this.bufferFactory.wrap(((PongMessage) message).getApplicationData());
			return WebSocketMessage.create(Type.PONG, buffer);
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
