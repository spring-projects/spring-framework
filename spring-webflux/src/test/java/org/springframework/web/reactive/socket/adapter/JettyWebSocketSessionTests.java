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
 *
 * @author Max Guiking
 */
class JettyWebSocketSessionTests {

	private final DefaultDataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;

	private final Session jettySession = mock(Session.class);

	private final JettyWebSocketSession session = new JettyWebSocketSession(this.jettySession,
			new HandshakeInfo(URI.create("ws://example.org"), new HttpHeaders(), Mono.empty(), null),
			this.bufferFactory);


	@Test
	void sendBinaryMessageWithSingleFragmentMarksFragmentAsLast() {
		succeedOnSendPartialBinary();

		DataBuffer payload = this.bufferFactory.wrap("hello".getBytes(StandardCharsets.UTF_8));
		WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.BINARY, payload);

		this.session.sendMessage(message).block();

		ArgumentCaptor<Boolean> lastCaptor = ArgumentCaptor.forClass(Boolean.class);
		verify(this.jettySession).sendPartialBinary(any(ByteBuffer.class), lastCaptor.capture(), any(Callback.class));
		assertThat(lastCaptor.getValue()).as("FIN bit must be set for the final (and only) fragment").isTrue();
	}

	@Test
	void sendBinaryMessageWithMultipleFragmentsMarksOnlyFinalFragmentAsLast() {
		succeedOnSendPartialBinary();

		List<ByteBuffer> fragments = List.of(
				ByteBuffer.wrap("one".getBytes(StandardCharsets.UTF_8)),
				ByteBuffer.wrap("two".getBytes(StandardCharsets.UTF_8)),
				ByteBuffer.wrap("three".getBytes(StandardCharsets.UTF_8)));
		WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.BINARY,
				new MultiBufferDataBuffer(this.bufferFactory, fragments));

		this.session.sendMessage(message).block();

		ArgumentCaptor<Boolean> lastCaptor = ArgumentCaptor.forClass(Boolean.class);
		verify(this.jettySession, times(fragments.size()))
				.sendPartialBinary(any(ByteBuffer.class), lastCaptor.capture(), any(Callback.class));
		assertThat(lastCaptor.getAllValues()).containsExactly(false, false, true);
	}

	private void succeedOnSendPartialBinary() {
		doAnswer(invocation -> {
			Callback callback = invocation.getArgument(2);
			callback.succeed();
			return null;
		}).when(this.jettySession).sendPartialBinary(any(ByteBuffer.class), anyBoolean(), any(Callback.class));
	}


	/**
	 * Minimal {@link DataBuffer} whose {@link #readableByteBuffers()} yields a
	 * caller-supplied list of buffers, exercising the multi-fragment branch of
	 * {@link JettyWebSocketSession#sendMessage(WebSocketMessage)}.
	 */
	private static final class MultiBufferDataBuffer extends DataBufferWrapper {

		private final List<ByteBuffer> buffers;

		MultiBufferDataBuffer(DefaultDataBufferFactory factory, List<ByteBuffer> buffers) {
			super(factory.allocateBuffer(0));
			this.buffers = buffers;
		}

		@Override
		public DataBuffer.ByteBufferIterator readableByteBuffers() {
			return new DataBuffer.ByteBufferIterator() {

				private int index = 0;

				@Override
				public boolean hasNext() {
					return this.index < MultiBufferDataBuffer.this.buffers.size();
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
