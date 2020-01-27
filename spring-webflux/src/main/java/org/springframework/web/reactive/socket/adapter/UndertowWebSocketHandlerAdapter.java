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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
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
public class UndertowWebSocketHandlerAdapter extends AbstractReceiveListener {

	private final UndertowWebSocketSession session;


	public UndertowWebSocketHandlerAdapter(UndertowWebSocketSession session) {
		Assert.notNull(session, "UndertowWebSocketSession is required");
		this.session = session;
	}


	@Override
	protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
		this.session.handleMessage(Type.TEXT, toMessage(Type.TEXT, message.getData()));
	}

	@Override
	protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
		this.session.handleMessage(Type.BINARY, toMessage(Type.BINARY, message.getData().getResource()));
		message.getData().free();
	}

	@Override
	protected void onFullPongMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
		this.session.handleMessage(Type.PONG, toMessage(Type.PONG, message.getData().getResource()));
		message.getData().free();
	}

	@Override
	protected void onFullCloseMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
		CloseMessage closeMessage = new CloseMessage(message.getData().getResource());
		this.session.handleClose(new CloseStatus(closeMessage.getCode(), closeMessage.getReason()));
		message.getData().free();
	}

	@Override
	protected void onError(WebSocketChannel channel, Throwable error) {
		this.session.handleError(error);
	}

	private <T> WebSocketMessage toMessage(Type type, T message) {
		if (Type.TEXT.equals(type)) {
			byte[] bytes = ((String) message).getBytes(StandardCharsets.UTF_8);
			return new WebSocketMessage(Type.TEXT, this.session.bufferFactory().wrap(bytes));
		}
		else if (Type.BINARY.equals(type)) {
			DataBuffer buffer = this.session.bufferFactory().allocateBuffer().write((ByteBuffer[]) message);
			return new WebSocketMessage(Type.BINARY, buffer);
		}
		else if (Type.PONG.equals(type)) {
			DataBuffer buffer = this.session.bufferFactory().allocateBuffer().write((ByteBuffer[]) message);
			return new WebSocketMessage(Type.PONG, buffer);
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message);
		}
	}

}
