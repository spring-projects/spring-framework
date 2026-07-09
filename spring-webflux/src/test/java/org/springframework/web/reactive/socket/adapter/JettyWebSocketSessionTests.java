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

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferWrapper;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JettyWebSocketSession}.
 * @author Max Guiking
 */
class JettyWebSocketSessionTests {

	private final Session nativeSession = mock(Session.class);

	private final JettyWebSocketSession webSocketSession = new JettyWebSocketSession(
			this.nativeSession, new HandshakeInfo(URI.create("ws://example.org"),
			new HttpHeaders(), Mono.empty(), null), DefaultDataBufferFactory.sharedInstance);


	@Test
	void sendBinaryMessageWithSingleBuffer() {
		succeedOnSendPartialBinary();

		DataBuffer payload = DefaultDataBufferFactory.sharedInstance.wrap("hello".getBytes(StandardCharsets.UTF_8));
		WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.BINARY, payload);

		this.webSocketSession.sendMessage(message).block();

		ArgumentCaptor<Boolean> last = ArgumentCaptor.forClass(Boolean.class);
		verify(this.nativeSession).sendPartialBinary(any(ByteBuffer.class), last.capture(), any(Callback.class));
		assertThat(last.getValue()).isTrue();
	}

	@Test
	void sendBinaryMessageWithMultipleBuffers() {
		succeedOnSendPartialBinary();

		WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.BINARY, new MultiBufferDataBuffer(
				ByteBuffer.wrap("one".getBytes(StandardCharsets.UTF_8)),
				ByteBuffer.wrap("two".getBytes(StandardCharsets.UTF_8)),
				ByteBuffer.wrap("three".getBytes(StandardCharsets.UTF_8))));

		this.webSocketSession.sendMessage(message).block();

		ArgumentCaptor<Boolean> last = ArgumentCaptor.forClass(Boolean.class);
		verify(this.nativeSession, times(3)).sendPartialBinary(any(ByteBuffer.class), last.capture(), any(Callback.class));
		assertThat(last.getAllValues()).containsExactly(false, false, true);
	}

	private void succeedOnSendPartialBinary() {
		doAnswer(invocation -> {
			Callback callback = invocation.getArgument(2);
			callback.succeed();
			return null;
		}).when(this.nativeSession).sendPartialBinary(any(ByteBuffer.class), anyBoolean(), any(Callback.class));
	}


	/**
	 * Minimal DataBuffer that returns a given list of buffers from {@link #readableByteBuffers()}.
	 */
	private static final class MultiBufferDataBuffer extends DataBufferWrapper {

		private final List<ByteBuffer> buffers;

		MultiBufferDataBuffer(ByteBuffer... buffers) {
			super(DefaultDataBufferFactory.sharedInstance.allocateBuffer(0));
			this.buffers = Arrays.asList(buffers);
		}

		@Override
		public DataBuffer.ByteBufferIterator readableByteBuffers() {
			return new DataBuffer.ByteBufferIterator() {

				private int index = 0;

				@Override
				public boolean hasNext() {
					return (this.index < MultiBufferDataBuffer.this.buffers.size());
				}

				@Override
				public ByteBuffer next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					return MultiBufferDataBuffer.this.buffers.get(this.index++).asReadOnlyBuffer();
				}

				@Override
				public void close() {
				}
			};
		}
	}

}
