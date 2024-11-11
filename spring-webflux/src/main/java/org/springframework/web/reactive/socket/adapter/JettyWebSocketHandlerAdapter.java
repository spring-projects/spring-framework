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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.IntPredicate;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import org.springframework.core.io.buffer.CloseableDataBuffer;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
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

	@Nullable
	private JettyWebSocketSession delegateSession;


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
	public void onWebSocketClose(int statusCode, String reason) {
		Assert.state(this.delegateSession != null, "No delegate session available");
		this.delegateSession.handleClose(CloseStatus.create(statusCode, reason));
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		Assert.state(this.delegateSession != null, "No delegate session available");
		this.delegateSession.handleError(cause);
	}


	private static final class JettyCallbackDataBuffer implements CloseableDataBuffer {

		private final DataBuffer delegate;

		private final Callback callback;


		public JettyCallbackDataBuffer(DataBuffer delegate, Callback callback) {
			Assert.notNull(delegate, "'delegate` must not be null");
			Assert.notNull(callback, "Callback must not be null");
			this.delegate = delegate;
			this.callback = callback;
		}

		@Override
		public void close() {
			this.callback.succeed();
		}

		// delegation

		@Override
		public DataBufferFactory factory() {
			return this.delegate.factory();
		}

		@Override
		public int indexOf(IntPredicate predicate, int fromIndex) {
			return this.delegate.indexOf(predicate, fromIndex);
		}

		@Override
		public int lastIndexOf(IntPredicate predicate, int fromIndex) {
			return this.delegate.lastIndexOf(predicate, fromIndex);
		}

		@Override
		public int readableByteCount() {
			return this.delegate.readableByteCount();
		}

		@Override
		public int writableByteCount() {
			return this.delegate.writableByteCount();
		}

		@Override
		public int capacity() {
			return this.delegate.capacity();
		}

		@Override
		@Deprecated
		public DataBuffer capacity(int capacity) {
			this.delegate.capacity(capacity);
			return this;
		}

		@Override
		public DataBuffer ensureWritable(int capacity) {
			this.delegate.ensureWritable(capacity);
			return this;
		}

		@Override
		public int readPosition() {
			return this.delegate.readPosition();
		}

		@Override
		public DataBuffer readPosition(int readPosition) {
			this.delegate.readPosition(readPosition);
			return this;
		}

		@Override
		public int writePosition() {
			return this.delegate.writePosition();
		}

		@Override
		public DataBuffer writePosition(int writePosition) {
			this.delegate.writePosition(writePosition);
			return this;
		}

		@Override
		public byte getByte(int index) {
			return this.delegate.getByte(index);
		}

		@Override
		public byte read() {
			return this.delegate.read();
		}

		@Override
		public DataBuffer read(byte[] destination) {
			this.delegate.read(destination);
			return this;
		}

		@Override
		public DataBuffer read(byte[] destination, int offset, int length) {
			this.delegate.read(destination, offset, length);
			return this;
		}

		@Override
		public DataBuffer write(byte b) {
			this.delegate.write(b);
			return this;
		}

		@Override
		public DataBuffer write(byte[] source) {
			this.delegate.write(source);
			return this;
		}

		@Override
		public DataBuffer write(byte[] source, int offset, int length) {
			this.delegate.write(source, offset, length);
			return this;
		}

		@Override
		public DataBuffer write(DataBuffer... buffers) {
			this.delegate.write(buffers);
			return this;
		}

		@Override
		public DataBuffer write(ByteBuffer... buffers) {
			this.delegate.write(buffers);
			return this;
		}

		@Override
		@Deprecated
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
		@Deprecated
		public ByteBuffer asByteBuffer() {
			return this.delegate.asByteBuffer();
		}

		@Override
		@Deprecated
		public ByteBuffer asByteBuffer(int index, int length) {
			return this.delegate.asByteBuffer(index, length);
		}

		@Override
		@Deprecated
		public ByteBuffer toByteBuffer(int index, int length) {
			return this.delegate.toByteBuffer(index, length);
		}

		@Override
		public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
			this.delegate.toByteBuffer(srcPos, dest, destPos, length);
		}

		@Override
		public ByteBufferIterator readableByteBuffers() {
			ByteBufferIterator delegateIterator = this.delegate.readableByteBuffers();
			return new JettyByteBufferIterator(delegateIterator);
		}

		@Override
		public ByteBufferIterator writableByteBuffers() {
			ByteBufferIterator delegateIterator = this.delegate.writableByteBuffers();
			return new JettyByteBufferIterator(delegateIterator);
		}

		@Override
		public String toString(int index, int length, Charset charset) {
			return this.delegate.toString(index, length, charset);
		}


		private record JettyByteBufferIterator(ByteBufferIterator delegate) implements ByteBufferIterator {

			@Override
			public void close() {
				this.delegate.close();
			}

			@Override
			public boolean hasNext() {
				return this.delegate.hasNext();
			}

			@Override
			public ByteBuffer next() {
				return this.delegate.next();
			}
		}
	}

}
