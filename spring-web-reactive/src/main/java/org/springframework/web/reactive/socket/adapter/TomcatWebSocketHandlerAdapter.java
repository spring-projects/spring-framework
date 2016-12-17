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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
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
public class TomcatWebSocketHandlerAdapter extends WebSocketHandlerAdapterSupport {

	private TomcatWebSocketSession session;


	public TomcatWebSocketHandlerAdapter(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler delegate) {

		super(request, response, delegate);
	}


	public Endpoint getEndpoint() {
		return new StandardEndpoint();
	}

	private TomcatWebSocketSession getSession() {
		return this.session;
	}


	private class StandardEndpoint extends Endpoint {

		@Override
		public void onOpen(Session session, EndpointConfig config) {
			TomcatWebSocketHandlerAdapter.this.session =
					new TomcatWebSocketSession(session, getUri(), getBufferFactory());

			session.addMessageHandler(String.class, message -> {
				WebSocketMessage webSocketMessage = toMessage(message);
				getSession().handleMessage(webSocketMessage.getType(), webSocketMessage);
			});
			session.addMessageHandler(ByteBuffer.class, message -> {
				WebSocketMessage webSocketMessage = toMessage(message);
				getSession().handleMessage(webSocketMessage.getType(), webSocketMessage);
			});
			session.addMessageHandler(PongMessage.class, message -> {
				WebSocketMessage webSocketMessage = toMessage(message);
				getSession().handleMessage(webSocketMessage.getType(), webSocketMessage);
			});

			HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber();
			getDelegate().handle(TomcatWebSocketHandlerAdapter.this.session).subscribe(resultSubscriber);
		}

		private <T> WebSocketMessage toMessage(T message) {
			if (message instanceof String) {
				byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
				return new WebSocketMessage(Type.TEXT, getBufferFactory().wrap(bytes));
			}
			else if (message instanceof ByteBuffer) {
				DataBuffer buffer = getBufferFactory().wrap((ByteBuffer) message);
				return new WebSocketMessage(Type.BINARY, buffer);
			}
			else if (message instanceof PongMessage) {
				DataBuffer buffer = getBufferFactory().wrap(((PongMessage) message).getApplicationData());
				return new WebSocketMessage(Type.PONG, buffer);
			}
			else {
				throw new IllegalArgumentException("Unexpected message type: " + message);
			}
		}

		@Override
		public void onClose(Session session, CloseReason reason) {
			if (getSession() != null) {
				int code = reason.getCloseCode().getCode();
				getSession().handleClose(new CloseStatus(code, reason.getReasonPhrase()));
			}
		}

		@Override
		public void onError(Session session, Throwable exception) {
			if (getSession() != null) {
				getSession().handleError(exception);
			}
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
			if (getSession() != null) {
				getSession().close(new CloseStatus(CloseStatus.SERVER_ERROR.getCode(), ex.getMessage()));
			}
		}

		@Override
		public void onComplete() {
			if (getSession() != null) {
				getSession().close();
			}
		}
	}

}
