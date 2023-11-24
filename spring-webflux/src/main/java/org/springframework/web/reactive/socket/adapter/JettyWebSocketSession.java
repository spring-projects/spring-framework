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

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Spring {@link WebSocketSession} implementation that adapts to a Jetty
 * WebSocket {@link org.eclipse.jetty.websocket.api.Session}.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class JettyWebSocketSession extends AbstractListenerWebSocketSession<Session> {

	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
		this(session, info, factory, null);
	}

	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
			@Nullable Sinks.Empty<Void> completionSink) {

		super(session, ObjectUtils.getIdentityHexString(session), info, factory, completionSink);
		// TODO: suspend causes failures if invoked at this stage
		// suspendReceiving();
	}


	@Override
	protected boolean canSuspendReceiving() {
		// Jetty 12 TODO: research suspend functionality in Jetty 12
		return false;
	}

	@Override
	protected void suspendReceiving() {
	}

	@Override
	protected void resumeReceiving() {
	}

	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		DataBuffer dataBuffer = message.getPayload();
		Session session = getDelegate();
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			String text = dataBuffer.toString(StandardCharsets.UTF_8);
			session.sendText(text, new SendProcessorCallback());
		}
		else {
			if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
				getSendProcessor().setReadyToSend(false);
			}
			try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
				while (iterator.hasNext()) {
					ByteBuffer byteBuffer = iterator.next();
					switch (message.getType()) {
						case BINARY -> session.sendBinary(byteBuffer, new SendProcessorCallback());
						case PING -> session.sendPing(byteBuffer, new SendProcessorCallback());
						case PONG -> session.sendPong(byteBuffer, new SendProcessorCallback());
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
		Callback.Completable callback = new Callback.Completable();
		getDelegate().close(status.getCode(), status.getReason(), callback);

		return Mono.fromFuture(callback);
	}


	private final class SendProcessorCallback implements Callback {

		@Override
		public void fail(Throwable x) {
			getSendProcessor().cancel();
			getSendProcessor().onError(x);
		}

		@Override
		public void succeed() {
			getSendProcessor().setReadyToSend(true);
			getSendProcessor().onWritePossible();
		}

	}

}
