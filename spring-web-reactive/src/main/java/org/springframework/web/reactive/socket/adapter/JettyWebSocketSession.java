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
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import reactor.core.publisher.Mono;

/**
 * Spring {@link WebSocketSession} adapter for Jetty's
 * {@link org.eclipse.jetty.websocket.api.Session}.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
public class JettyWebSocketSession extends AbstractListenerWebSocketSession<Session> {

	public JettyWebSocketSession(Session session) {
		super(session, ObjectUtils.getIdentityHexString(session),
				session.getUpgradeRequest().getRequestURI());
	}

	@Override
	protected Mono<Void> closeInternal(CloseStatus status) {
		getDelegate().close(status.getCode(), status.getReason());
		return Mono.empty();
	}

	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			getSendProcessor().setReady(false);
			getDelegate().getRemote().sendString(
					new String(message.getPayload().asByteBuffer().array(), StandardCharsets.UTF_8),
					new WebSocketMessageWriteCallback());
		}
		else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			getSendProcessor().setReady(false);
			getDelegate().getRemote().sendBytes(message.getPayload().asByteBuffer(),
					new WebSocketMessageWriteCallback());
		}
		else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			getDelegate().getRemote().sendPing(message.getPayload().asByteBuffer());
		}
		else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			getDelegate().getRemote().sendPong(message.getPayload().asByteBuffer());
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}
		return true;
	}

	private final class WebSocketMessageWriteCallback implements WriteCallback {

		@Override
		public void writeFailed(Throwable x) {
			getSendProcessor().cancel();
			getSendProcessor().onError(x);
		}

		@Override
		public void writeSuccess() {
			getSendProcessor().setReady(true);
			getSendProcessor().onWritePossible();
		}

	}

}
