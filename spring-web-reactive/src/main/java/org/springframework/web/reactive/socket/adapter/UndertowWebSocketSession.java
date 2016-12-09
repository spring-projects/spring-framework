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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import reactor.core.publisher.Mono;

/**
 * Spring {@link WebSocketSession} adapter for Undertow's
 * {@link io.undertow.websockets.core.WebSocketChannel}.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
public class UndertowWebSocketSession extends AbstractListenerWebSocketSession<WebSocketChannel> {

	public UndertowWebSocketSession(WebSocketChannel channel) throws URISyntaxException {
		super(channel, ObjectUtils.getIdentityHexString(channel), new URI(channel.getUrl()));
	}

	@Override
	protected Mono<Void> closeInternal(CloseStatus status) {
		CloseMessage cm = new CloseMessage(status.getCode(), status.getReason());
		if (!getDelegate().isCloseFrameSent()) {
			WebSockets.sendClose(cm, getDelegate(), null);
		}
		return Mono.empty();
	}

	protected void resumeReceives() {
		super.resumeReceives();
		getDelegate().resumeReceives();
	}

	protected void suspendReceives() {
		super.suspendReceives();
		getDelegate().suspendReceives();
	}

	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			getSendProcessor().setReady(false);
			WebSockets.sendText(
					new String(message.getPayload().asByteBuffer().array(), StandardCharsets.UTF_8),
					getDelegate(), new WebSocketMessageSendHandler());
		}
		else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			getSendProcessor().setReady(false);
			WebSockets.sendBinary(message.getPayload().asByteBuffer(),
					getDelegate(), new WebSocketMessageSendHandler());
		}
		else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			getSendProcessor().setReady(false);
			WebSockets.sendPing(message.getPayload().asByteBuffer(),
					getDelegate(), new WebSocketMessageSendHandler());
		}
		else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			getSendProcessor().setReady(false);
			WebSockets.sendPong(message.getPayload().asByteBuffer(),
					getDelegate(), new WebSocketMessageSendHandler());
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}
		return true;
	}

	private final class WebSocketMessageSendHandler implements WebSocketCallback<Void> {

		@Override
		public void complete(WebSocketChannel channel, Void context) {
			getSendProcessor().setReady(true);
			getSendProcessor().onWritePossible();
		}

		@Override
		public void onError(WebSocketChannel channel, Void context,
				Throwable throwable) {
			getSendProcessor().cancel();
			getSendProcessor().onError(throwable);
		}

	}

}
