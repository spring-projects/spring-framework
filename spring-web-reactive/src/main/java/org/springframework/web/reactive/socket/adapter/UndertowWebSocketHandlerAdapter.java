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
import io.undertow.websockets.spi.WebSocketHttpExchange;

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
 * Undertow {@link WebSocketConnectionCallback} implementation that adapts and
 * delegates to a Spring {@link WebSocketHandler}.
 * 
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UndertowWebSocketHandlerAdapter extends WebSocketHandlerAdapterSupport
		implements WebSocketConnectionCallback {

	private UndertowWebSocketSession delegateSession;

	private final MonoProcessor<Void> completionMono;


	public UndertowWebSocketHandlerAdapter(WebSocketHandler delegate, HandshakeInfo info,
			DataBufferFactory bufferFactory) {

		this(delegate, info, bufferFactory, null);
	}

	public UndertowWebSocketHandlerAdapter(WebSocketHandler delegate, HandshakeInfo info,
			DataBufferFactory bufferFactory, MonoProcessor<Void> completionMono) {

		super(delegate, info, bufferFactory);
		this.completionMono = completionMono;
	}


	@Override
	public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
		this.delegateSession = new UndertowWebSocketSession(channel, getHandshakeInfo(), bufferFactory());
		channel.getReceiveSetter().set(new UndertowReceiveListener());
		channel.resumeReceives();

		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber();
		getDelegate().handle(this.delegateSession).subscribe(resultSubscriber);
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
			if (Type.TEXT.equals(type)) {
				byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
				return new WebSocketMessage(Type.TEXT, bufferFactory().wrap(bytes));
			}
			else if (Type.BINARY.equals(type)) {
				DataBuffer buffer = bufferFactory().allocateBuffer().write((ByteBuffer[]) message);
				return new WebSocketMessage(Type.BINARY, buffer);
			}
			else if (Type.PONG.equals(type)) {
				DataBuffer buffer = bufferFactory().allocateBuffer().write((ByteBuffer[]) message);
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
