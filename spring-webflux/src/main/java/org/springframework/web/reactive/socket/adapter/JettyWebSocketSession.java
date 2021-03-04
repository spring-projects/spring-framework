/*
 * Copyright 2002-2020 the original author or authors.
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

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.SuspendToken;
import org.eclipse.jetty.websocket.api.WriteCallback;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
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

	@Nullable
	private volatile SuspendToken suspendToken;


	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
		this(session, info, factory, (Sinks.Empty<Void>) null);
	}

	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
			@Nullable Sinks.Empty<Void> completionSink) {

		super(session, ObjectUtils.getIdentityHexString(session), info, factory, completionSink);
		// TODO: suspend causes failures if invoked at this stage
		// suspendReceiving();
	}

	@Deprecated
	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
			@Nullable reactor.core.publisher.MonoProcessor<Void> completionMono) {

		super(session, ObjectUtils.getIdentityHexString(session), info, factory, completionMono);
	}


	@Override
	protected boolean canSuspendReceiving() {
		return true;
	}

	@Override
	protected void suspendReceiving() {
		Assert.state(this.suspendToken == null, "Already suspended");
		this.suspendToken = getDelegate().suspend();
	}

	@Override
	protected void resumeReceiving() {
		SuspendToken tokenToUse = this.suspendToken;
		this.suspendToken = null;
		if (tokenToUse != null) {
			tokenToUse.resume();
		}
	}

	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		ByteBuffer buffer = message.getPayload().asByteBuffer();
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			String text = new String(buffer.array(), StandardCharsets.UTF_8);
			getDelegate().getRemote().sendString(text, new SendProcessorCallback());
		}
		else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			getDelegate().getRemote().sendBytes(buffer, new SendProcessorCallback());
		}
		else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			getDelegate().getRemote().sendPing(buffer);
		}
		else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			getDelegate().getRemote().sendPong(buffer);
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}
		return true;
	}

	@Override
	public boolean isOpen() {
		return getDelegate().isOpen();
	}

	@Override
	public Mono<Void> close(CloseStatus status) {
		getDelegate().close(status.getCode(), status.getReason());
		return Mono.empty();
	}


	private final class SendProcessorCallback implements WriteCallback {

		@Override
		public void writeFailed(Throwable x) {
			getSendProcessor().cancel();
			getSendProcessor().onError(x);
		}

		@Override
		public void writeSuccess() {
			getSendProcessor().setReadyToSend(true);
			getSendProcessor().onWritePossible();
		}

	}

}
