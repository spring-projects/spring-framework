/*
 * Copyright 2002-present the original author or authors.
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import guru.mocker.annotation.mixin.Mixin;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.jspecify.annotations.Nullable;

import org.springframework.core.io.buffer.CloseableDataBuffer;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketMessage.Type;

/**
 * Jetty {@link org.eclipse.jetty.websocket.api.Session.Listener} handler that delegates events to a
 * reactive {@link WebSocketHandler} and its session.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class JettyWebSocketHandlerAdapter implements Session.Listener {

	private final WebSocketHandler delegateHandler;

	private final Function<Session, JettyWebSocketSession> sessionFactory;

	private @Nullable JettyWebSocketSession delegateSession;


	public JettyWebSocketHandlerAdapter(WebSocketHandler handler,
			Function<Session, JettyWebSocketSession> sessionFactory) {

		Assert.notNull(handler, "WebSocketHandler is required");
		Assert.notNull(sessionFactory, "'sessionFactory' is required");
		this.delegateHandler = handler;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void onWebSocketOpen(Session session) {
		JettyWebSocketSession delegateSession = this.sessionFactory.apply(session);
		this.delegateSession = delegateSession;
		this.delegateHandler.handle(delegateSession)
				.checkpoint(session.getUpgradeRequest().getRequestURI() + " [JettyWebSocketHandlerAdapter]")
				.subscribe(unused -> {}, delegateSession::onHandlerError, delegateSession::onHandleComplete);
	}

	@Override
	public void onWebSocketText(String message) {
		Assert.state(this.delegateSession != null, "No delegate session available");
		byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = this.delegateSession.bufferFactory().wrap(bytes);
		WebSocketMessage webSocketMessage = new WebSocketMessage(Type.TEXT, buffer);
		this.delegateSession.handleMessage(webSocketMessage);
	}

	@Override
	public void onWebSocketBinary(ByteBuffer byteBuffer, Callback callback) {
		Assert.state(this.delegateSession != null, "No delegate session available");
		DataBuffer buffer = this.delegateSession.bufferFactory().wrap(byteBuffer);
		buffer = new JettyCallbackDataBuffer(buffer, callback);
		WebSocketMessage webSocketMessage = new WebSocketMessage(Type.BINARY, buffer);
		this.delegateSession.handleMessage(webSocketMessage);
	}

	@Override
	public void onWebSocketPong(ByteBuffer payload) {
		Assert.state(this.delegateSession != null, "No delegate session available");
		DataBuffer buffer = this.delegateSession.bufferFactory().wrap(BufferUtil.copy(payload));
		WebSocketMessage webSocketMessage = new WebSocketMessage(Type.PONG, buffer);
		this.delegateSession.handleMessage(webSocketMessage);
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason, Callback callback) {
		Assert.state(this.delegateSession != null, "No delegate session available");
		this.delegateSession.handleClose(CloseStatus.create(statusCode, reason));
		callback.succeed();
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		Assert.state(this.delegateSession != null, "No delegate session available");
		this.delegateSession.handleError(cause);
	}

	@Mixin
	static final class JettyCallbackDataBuffer extends JettyCallbackDataBufferForwarder implements CloseableDataBuffer {

		public JettyCallbackDataBuffer(DataBuffer delegate, Callback callback) {
			super(delegate, callback);
		}

		@Override
		public void close() {
			this.callback.succeed();
		}

		@Override
		@Deprecated(since = "6.0")
		public DataBuffer capacity(int capacity) {
			this.delegate.capacity(capacity);
			return this;
		}

		@Override
		@Deprecated(since = "6.0")
		public DataBuffer slice(int index, int length) {
			DataBuffer delegateSlice = this.delegate.slice(index, length);
			return new JettyCallbackDataBuffer(delegateSlice, this.callback);
		}

		@Override
		public DataBuffer split(int index) {
			DataBuffer delegateSplit = this.delegate.split(index);
			return new JettyCallbackDataBuffer(delegateSplit, this.callback);
		}

		@Override
		@Deprecated(since = "6.0")
		public ByteBuffer asByteBuffer() {
			return this.delegate.asByteBuffer();
		}

		@Override
		@Deprecated(since = "6.0")
		public ByteBuffer asByteBuffer(int index, int length) {
			return this.delegate.asByteBuffer(index, length);
		}

		@Override
		@Deprecated(since = "6.0.5")
		public ByteBuffer toByteBuffer(int index, int length) {
			return this.delegate.toByteBuffer(index, length);
		}

		@Override
		public String toString(int index, int length, Charset charset) {
			return this.delegate.toString(index, length, charset);
		}
	}

}
