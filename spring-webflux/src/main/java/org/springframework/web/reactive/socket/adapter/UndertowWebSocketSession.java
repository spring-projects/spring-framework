/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
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

	public UndertowWebSocketSession(WebSocketChannel channel, HandshakeInfo info, DataBufferFactory factory) {
		this(channel, info, factory, null);
	}

	public UndertowWebSocketSession(WebSocketChannel channel, HandshakeInfo info,
			DataBufferFactory factory, @Nullable Sinks.Empty<Void> completionSink) {

		super(channel, ObjectUtils.getIdentityHexString(channel), info, factory, completionSink);
		suspendReceiving();
	}


	@Override
	protected boolean canSuspendReceiving() {
		return true;
	}

	@Override
	protected void suspendReceiving() {
		getDelegate().suspendReceives();
	}

	@Override
	protected void resumeReceiving() {
		getDelegate().resumeReceives();
	}

	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		DataBuffer dataBuffer = message.getPayload();
		WebSocketChannel channel = getDelegate();
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			String text = dataBuffer.toString(StandardCharsets.UTF_8);
			WebSockets.sendText(text, channel, new SendProcessorCallback(message.getPayload()));
		}
		else {
			getSendProcessor().setReadyToSend(false);
			try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
				while (iterator.hasNext()) {
					ByteBuffer byteBuffer = iterator.next();
					switch (message.getType()) {
						case BINARY -> WebSockets.sendBinary(byteBuffer, channel, new SendProcessorCallback(dataBuffer));
						case PING -> WebSockets.sendPing(byteBuffer, channel, new SendProcessorCallback(dataBuffer));
						case PONG -> WebSockets.sendPong(byteBuffer, channel, new SendProcessorCallback(dataBuffer));
						default -> throw new IllegalArgumentException("Unexpected message type: " + message.getType());
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean isOpen() {
		return getDelegate().isOpen();
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

		private final DataBuffer payload;

		SendProcessorCallback(DataBuffer payload) {
			this.payload = payload;
		}

		@Override
		public void complete(WebSocketChannel channel, Void context) {
			DataBufferUtils.release(this.payload);
			getSendProcessor().setReadyToSend(true);
			getSendProcessor().onWritePossible();
		}

		@Override
		public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
			DataBufferUtils.release(this.payload);
			getSendProcessor().cancel();
			getSendProcessor().onError(throwable);
		}
	}

}
