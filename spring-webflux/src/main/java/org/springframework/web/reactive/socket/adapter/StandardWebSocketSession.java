/*
 * Copyright 2002-2018 the original author or authors.
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
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Spring {@link WebSocketSession} adapter for a standard Java (JSR 356)
 * {@link javax.websocket.Session}.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class StandardWebSocketSession extends AbstractListenerWebSocketSession<Session> {

	public StandardWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
		this(session, info, factory, null);
	}

	public StandardWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
			@Nullable MonoProcessor<Void> completionMono) {

		super(session, session.getId(), info, factory, completionMono);
	}


	@Override
	protected boolean canSuspendReceiving() {
		return false;
	}

	@Override
	protected void suspendReceiving() {
		// no-op
	}

	@Override
	protected void resumeReceiving() {
		// no-op
	}

	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		ByteBuffer buffer = message.getPayload().asByteBuffer();
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			String text = new String(buffer.array(), StandardCharsets.UTF_8);
			getDelegate().getAsyncRemote().sendText(text, new SendProcessorCallback());
		}
		else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			getDelegate().getAsyncRemote().sendBinary(buffer, new SendProcessorCallback());
		}
		else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			getDelegate().getAsyncRemote().sendPing(buffer);
		}
		else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			getDelegate().getAsyncRemote().sendPong(buffer);
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}
		return true;
	}

	@Override
	public Mono<Void> close(CloseStatus status) {
		try {
			CloseReason.CloseCode code = CloseCodes.getCloseCode(status.getCode());
			getDelegate().close(new CloseReason(code, status.getReason()));
		}
		catch (IOException ex) {
			return Mono.error(ex);
		}
		return Mono.empty();
	}


	private final class SendProcessorCallback implements SendHandler {

		@Override
		public void onResult(SendResult result) {
			if (result.isOK()) {
				getSendProcessor().setReadyToSend(true);
				getSendProcessor().onWritePossible();
			}
			else {
				getSendProcessor().cancel();
				getSendProcessor().onError(result.getException());
			}
		}

	}

}
