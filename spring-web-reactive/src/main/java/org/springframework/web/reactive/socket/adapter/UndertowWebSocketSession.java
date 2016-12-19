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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Spring {@link WebSocketSession} implementation that adapts to an Undertow
 * {@link io.undertow.websockets.core.WebSocketChannel}.
 * 
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UndertowWebSocketSession extends AbstractListenerWebSocketSession<WebSocketChannel> {


	public UndertowWebSocketSession(WebSocketChannel channel, HandshakeInfo handshakeInfo,
			DataBufferFactory bufferFactory) {

		super(channel, ObjectUtils.getIdentityHexString(channel), handshakeInfo, bufferFactory);
	}


	@Override
	protected boolean canSuspendReceiving() {
		return true;
	}

	protected void suspendReceiving() {
		getDelegate().suspendReceives();
	}

	protected void resumeReceiving() {
		getDelegate().resumeReceives();
	}

	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		ByteBuffer buffer = message.getPayload().asByteBuffer();
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			String text = new String(buffer.array(), StandardCharsets.UTF_8);
			WebSockets.sendText(text, getDelegate(), new SendProcessorCallback());
		}
		else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			WebSockets.sendBinary(buffer, getDelegate(), new SendProcessorCallback());
		}
		else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			WebSockets.sendPing(buffer, getDelegate(), new SendProcessorCallback());
		}
		else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			WebSockets.sendPong(buffer, getDelegate(), new SendProcessorCallback());
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}
		return true;
	}

	@Override
	public Mono<Void> close(CloseStatus status) {
		CloseMessage cm = new CloseMessage(status.getCode(), status.getReason());
		if (!getDelegate().isCloseFrameSent()) {
			WebSockets.sendClose(cm, getDelegate(), null);
		}
		return Mono.empty();
	}


	private final class SendProcessorCallback implements WebSocketCallback<Void> {

		@Override
		public void complete(WebSocketChannel channel, Void context) {
			getSendProcessor().setReadyToSend(true);
			getSendProcessor().onWritePossible();
		}

		@Override
		public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
			getSendProcessor().cancel();
			getSendProcessor().onError(throwable);
		}
	}

}
