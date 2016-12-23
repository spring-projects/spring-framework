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

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
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
 * Undertow {@link WebSocketConnectionCallback} implementation that adapts and
 * delegates to a Spring {@link WebSocketHandler}.
 * 
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UndertowWebSocketHandlerAdapter {

	private final WebSocketHandler delegateHandler;

	private final MonoProcessor<Void> completionMono;

	private UndertowWebSocketSession delegateSession;


	public UndertowWebSocketHandlerAdapter(WebSocketHandler handler) {
		this(handler, null);
	}

	public UndertowWebSocketHandlerAdapter(WebSocketHandler handler, MonoProcessor<Void> completionMono) {

		Assert.notNull("WebSocketHandler is required");
		Assert.notNull("'sessionFactory' is required");
		this.delegateHandler = handler;
		this.completionMono = completionMono;
	}


	public void handle(UndertowWebSocketSession webSocketSession) {
		this.delegateSession = webSocketSession;
		webSocketSession.getDelegate().getReceiveSetter().set(new UndertowReceiveListener());
		webSocketSession.getDelegate().resumeReceives();

		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber();
		this.delegateHandler.handle(this.delegateSession).subscribe(resultSubscriber);
	}


	private final class UndertowReceiveListener extends AbstractReceiveListener {

		@Override
		protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
			delegateSession.handleMessage(Type.TEXT, toMessage(Type.TEXT, message.getData()));
		}

		@Override
		protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
			delegateSession.handleMessage(Type.BINARY, toMessage(Type.BINARY, message.getData().getResource()));
			message.getData().free();
		}

		@Override
		protected void onFullPongMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
			delegateSession.handleMessage(Type.PONG, toMessage(Type.PONG, message.getData().getResource()));
			message.getData().free();
		}

		@Override
		protected void onFullCloseMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
			CloseMessage closeMessage = new CloseMessage(message.getData().getResource());
			delegateSession.handleClose(new CloseStatus(closeMessage.getCode(), closeMessage.getReason()));
			message.getData().free();
		}

		@Override
		protected void onError(WebSocketChannel channel, Throwable error) {
			delegateSession.handleError(error);
		}

		private <T> WebSocketMessage toMessage(Type type, T message) {
			WebSocketSession session = delegateSession;
			Assert.state(session != null, "Cannot create message without a session");
			if (Type.TEXT.equals(type)) {
				byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
				return new WebSocketMessage(Type.TEXT, session.bufferFactory().wrap(bytes));
			}
			else if (Type.BINARY.equals(type)) {
				DataBuffer buffer = session.bufferFactory().allocateBuffer().write((ByteBuffer[]) message);
				return new WebSocketMessage(Type.BINARY, buffer);
			}
			else if (Type.PONG.equals(type)) {
				DataBuffer buffer = session.bufferFactory().allocateBuffer().write((ByteBuffer[]) message);
				return new WebSocketMessage(Type.PONG, buffer);
			}
			else {
				throw new IllegalArgumentException("Unexpected message type: " + message);
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
			if (completionMono != null) {
				completionMono.onError(ex);
			}
			int code = CloseStatus.SERVER_ERROR.getCode();
			delegateSession.close(new CloseStatus(code, ex.getMessage()));
		}

		@Override
		public void onComplete() {
			if (completionMono != null) {
				completionMono.onComplete();
			}
			delegateSession.close();
		}
	}

}
