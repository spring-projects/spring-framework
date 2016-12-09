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

import javax.websocket.CloseReason;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;

import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import reactor.core.publisher.Mono;

/**
 * Spring {@link WebSocketSession} adapter for Tomcat's
 * {@link javax.websocket.Session}.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
public class TomcatWebSocketSession extends AbstractListenerWebSocketSession<Session> {

	public TomcatWebSocketSession(Session session) {
		super(session, session.getId(), session.getRequestURI());
	}

	@Override
	protected Mono<Void> closeInternal(CloseStatus status) {
		try {
			getDelegate().close(
					new CloseReason(CloseCodes.getCloseCode(status.getCode()), status.getReason()));
		}
		catch (IOException e) {
			return Mono.error(e);
		}
		return Mono.empty();
	}

	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			getSendProcessor().setReady(false);
			getDelegate().getAsyncRemote().sendText(
					new String(message.getPayload().asByteBuffer().array(), StandardCharsets.UTF_8),
					new WebSocketMessageSendHandler());
		}
		else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			getSendProcessor().setReady(false);
			getDelegate().getAsyncRemote().sendBinary(message.getPayload().asByteBuffer(),
					new WebSocketMessageSendHandler());
		}
		else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			getDelegate().getAsyncRemote().sendPing(message.getPayload().asByteBuffer());
		}
		else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			getDelegate().getAsyncRemote().sendPong(message.getPayload().asByteBuffer());
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}
		return true;
	}

	private final class WebSocketMessageSendHandler implements SendHandler {

		@Override
		public void onResult(SendResult result) {
			if (result.isOK()) {
				getSendProcessor().setReady(true);
				getSendProcessor().onWritePossible();
			}
			else {
				getSendProcessor().cancel();
				getSendProcessor().onError(result.getException());
			}
		}

	}

}
