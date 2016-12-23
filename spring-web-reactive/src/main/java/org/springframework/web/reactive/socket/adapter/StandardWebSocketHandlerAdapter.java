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
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.PongMessage;
import javax.websocket.Session;

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
 * Adapter for Java WebSocket API (JSR-356) that delegates events to a reactive
 * {@link WebSocketHandler} and its session.
 * 
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class StandardWebSocketHandlerAdapter extends Endpoint {

	private final WebSocketHandler delegateHandler;

	private final MonoProcessor<Void> completionMono;

	private Function<Session, StandardWebSocketSession> sessionFactory;

	private StandardWebSocketSession delegateSession;


	public StandardWebSocketHandlerAdapter(WebSocketHandler handler,
			Function<Session, StandardWebSocketSession> sessionFactory) {

		this(handler, null, sessionFactory);
	}

	public StandardWebSocketHandlerAdapter(WebSocketHandler handler, MonoProcessor<Void> completionMono,
			Function<Session, StandardWebSocketSession> sessionFactory) {

		Assert.notNull("WebSocketHandler is required");
		Assert.notNull("'sessionFactory' is required");
		this.delegateHandler = handler;
		this.completionMono = completionMono;
		this.sessionFactory = sessionFactory;
	}


	@Override
	public void onOpen(Session session, EndpointConfig config) {

		this.delegateSession = this.sessionFactory.apply(session);

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

		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber();
		this.delegateHandler.handle(this.delegateSession).subscribe(resultSubscriber);
	}

	private <T> WebSocketMessage toMessage(T message) {
		WebSocketSession session = this.delegateSession;
		Assert.state(session != null, "Cannot create message without a session");
		if (message instanceof String) {
			byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
			return new WebSocketMessage(Type.TEXT, session.bufferFactory().wrap(bytes));
		}
		else if (message instanceof ByteBuffer) {
			DataBuffer buffer = session.bufferFactory().wrap((ByteBuffer) message);
			return new WebSocketMessage(Type.BINARY, buffer);
		}
		else if (message instanceof PongMessage) {
			DataBuffer buffer = session.bufferFactory().wrap(((PongMessage) message).getApplicationData());
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