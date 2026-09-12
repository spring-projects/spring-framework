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
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(10)
class JdkWebSocketSessionTests {

	private final WebSocket webSocket = mock();

	private final JdkWebSocketSession session = new JdkWebSocketSession(this.webSocket,
			new HandshakeInfo(URI.create("ws://example.org"), HttpHeaders.EMPTY, Mono.empty(), null),
			DefaultDataBufferFactory.sharedInstance, 16);


	@Test
	void fragmentsConsumeOneMessageOfDemand() {
		StepVerifier.create(this.session.receive(), 0)
				.then(() -> verify(this.webSocket, never()).request(1))
				.thenRequest(1)
				.then(() -> {
					verify(this.webSocket).request(1);
					this.session.onText(this.webSocket, "hel", false);
					verify(this.webSocket, times(2)).request(1);
					this.session.onText(this.webSocket, "lo", true);
				})
				.assertNext(message -> assertThat(message.getPayloadAsText()).isEqualTo("hello"))
				.then(() -> verify(this.webSocket, times(2)).request(1))
				.thenRequest(1)
				.then(() -> verify(this.webSocket, times(3)).request(1))
				.thenCancel()
				.verify();
	}

	@Test
	void pingInterleavedWithFragments() {
		StepVerifier.create(this.session.receive(), 1)
				.then(() -> {
					this.session.onText(this.webSocket, "hel", false);
					this.session.onPing(this.webSocket, ByteBuffer.wrap(new byte[] {1}));
				})
				.assertNext(message -> assertThat(message.getType()).isEqualTo(WebSocketMessage.Type.PING))
				.then(() -> verify(this.webSocket, times(2)).request(1))
				.thenRequest(1)
				.then(() -> this.session.onText(this.webSocket, "lo", true))
				.assertNext(message -> assertThat(message.getPayloadAsText()).isEqualTo("hello"))
				.thenCancel()
				.verify();
	}

	@Test
	void binaryInputIsCopiedBeforeCallbackReturns() {
		ByteBuffer buffer = ByteBuffer.wrap(new byte[] {1, 2});
		StepVerifier.create(this.session.receive(), 1)
				.then(() -> {
					this.session.onBinary(this.webSocket, buffer, false);
					buffer.put(0, (byte) 9);
					this.session.onBinary(this.webSocket, ByteBuffer.wrap(new byte[] {3}), true);
				})
				.assertNext(message -> {
					byte[] bytes = new byte[3];
					message.getPayload().read(bytes);
					assertThat(bytes).containsExactly(1, 2, 3);
				})
				.thenCancel()
				.verify();
	}

	@Test
	void cancellationStopsDemandBetweenFragments() {
		Disposable subscription = this.session.receive().subscribe();
		this.session.onText(this.webSocket, "first", false);
		subscription.dispose();
		this.session.onText(this.webSocket, "last", true);
		verify(this.webSocket, times(2)).request(1);
		verify(this.webSocket, never()).abort();
	}

	@Test
	void cancelledReceiveIgnoresOversizedOutstandingCallback() {
		Disposable subscription = this.session.receive().subscribe();
		subscription.dispose();
		assertThatCode(() -> this.session.onText(this.webSocket, "x".repeat(17), true))
				.doesNotThrowAnyException();
		verify(this.webSocket).request(1);
		verify(this.webSocket, never()).abort();
	}

	@Test
	void messageLimitIncludesFragments() {
		this.session.receive().subscribe();
		this.session.onText(this.webSocket, "1234567890", false);
		assertThatExceptionOfType(DataBufferLimitException.class)
				.isThrownBy(() -> this.session.onText(this.webSocket, "1234567", true));
	}

	@Test
	void messageLimitCountsUtf8Bytes() {
		this.session.receive().subscribe();
		assertThatExceptionOfType(DataBufferLimitException.class)
				.isThrownBy(() -> this.session.onText(this.webSocket, "€€€€€€", true));
	}

	@Test
	void closeCompletesReceiveAndPreservesStatus() {
		StepVerifier.create(this.session.receive())
				.then(() -> this.session.onClose(this.webSocket, 1001, "going away"))
				.verifyComplete();
		StepVerifier.create(this.session.closeStatus())
				.expectNext(CloseStatus.GOING_AWAY.withReason("going away"))
				.verifyComplete();
	}

	@Test
	void errorBeforeReceiveSubscriptionIsReplayed() {
		IllegalStateException error = new IllegalStateException("connection failed");
		this.session.onError(this.webSocket, error);
		StepVerifier.create(this.session.receive()).expectErrorSatisfies(actual -> assertThat(actual).isSameAs(error))
				.verify();
	}

	@Test
	void sendIsSerialAndIncludesEmptyMessages() {
		CompletableFuture<WebSocket> first = new CompletableFuture<>();
		when(this.webSocket.sendText("", true)).thenReturn(first);
		when(this.webSocket.sendText("next", true)).thenReturn(CompletableFuture.completedFuture(this.webSocket));
		StepVerifier.create(this.session.send(Flux.just(this.session.textMessage(""), this.session.textMessage("next"))))
				.then(() -> {
					verify(this.webSocket).sendText("", true);
					verify(this.webSocket, never()).sendText("next", true);
					first.complete(this.webSocket);
				})
				.verifyComplete();
		verify(this.webSocket).sendText("next", true);
	}

	@Test
	void pooledOutputIsCopiedAndReleasedEvenWhenSendIsCancelled() {
		NettyDataBuffer payload = new NettyDataBufferFactory(UnpooledByteBufAllocator.DEFAULT)
				.wrap(Unpooled.wrappedBuffer("hello".getBytes(StandardCharsets.UTF_8)));
		CompletableFuture<WebSocket> future = new CompletableFuture<>();
		when(this.webSocket.sendBinary(any(), eq(true))).thenReturn(future);
		Disposable subscription = this.session.send(Mono.just(new WebSocketMessage(WebSocketMessage.Type.BINARY, payload)))
				.subscribe();
		ArgumentCaptor<ByteBuffer> bytes = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(this.webSocket).sendBinary(bytes.capture(), eq(true));
		assertThat(payload.getNativeBuffer().refCnt()).isZero();
		subscription.dispose();
		assertThat(StandardCharsets.UTF_8.decode(bytes.getValue()).toString()).isEqualTo("hello");
	}

	@Test
	void sendCompositeBinaryPayload() {
		NettyDataBuffer payload = new NettyDataBufferFactory(UnpooledByteBufAllocator.DEFAULT).wrap(
				Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(new byte[] {1, 2}),
						Unpooled.wrappedBuffer(new byte[] {3, 4})));
		when(this.webSocket.sendBinary(any(), eq(true)))
				.thenReturn(CompletableFuture.completedFuture(this.webSocket));
		this.session.send(Mono.just(new WebSocketMessage(WebSocketMessage.Type.BINARY, payload))).block();
		ArgumentCaptor<ByteBuffer> bytes = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(this.webSocket).sendBinary(bytes.capture(), eq(true));
		assertThat(bytes.getValue().array()).containsExactly(1, 2, 3, 4);
		assertThat(payload.getNativeBuffer().refCnt()).isZero();
	}

	@Test
	void sendEmptyBinaryPayload() {
		when(this.webSocket.sendBinary(any(), eq(true)))
				.thenReturn(CompletableFuture.completedFuture(this.webSocket));
		this.session.send(Mono.just(this.session.binaryMessage(factory -> factory.wrap(new byte[0])))).block();
		ArgumentCaptor<ByteBuffer> bytes = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(this.webSocket).sendBinary(bytes.capture(), eq(true));
		assertThat(bytes.getValue().remaining()).isZero();
	}

	@Test
	void sendPingAndPongPayloads() {
		when(this.webSocket.sendPing(any())).thenReturn(CompletableFuture.completedFuture(this.webSocket));
		when(this.webSocket.sendPong(any())).thenReturn(CompletableFuture.completedFuture(this.webSocket));
		this.session.send(Flux.just(
				this.session.pingMessage(factory -> factory.wrap(new byte[] {1})),
				this.session.pongMessage(factory -> factory.wrap(new byte[] {2})))).block();
		ArgumentCaptor<ByteBuffer> ping = ArgumentCaptor.forClass(ByteBuffer.class);
		ArgumentCaptor<ByteBuffer> pong = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(this.webSocket).sendPing(ping.capture());
		verify(this.webSocket).sendPong(pong.capture());
		assertThat(ping.getValue().array()).containsExactly(1);
		assertThat(pong.getValue().array()).containsExactly(2);
	}

	@Test
	void failedSendIsPropagated() {
		IllegalStateException failure = new IllegalStateException("write failed");
		when(this.webSocket.sendText("hello", true)).thenReturn(CompletableFuture.failedFuture(failure));
		StepVerifier.create(this.session.send(Mono.just(this.session.textMessage("hello"))))
				.expectErrorSatisfies(actual -> assertThat(actual).isSameAs(failure)).verify();
	}

	@Test
	void sendDiscardsLateMessagesAfterCancellation() {
		AtomicReference<FluxSink<WebSocketMessage>> source = new AtomicReference<>();
		Disposable subscription = this.session.send(Flux.create(source::set)).subscribe();
		subscription.dispose();
		NettyDataBuffer payload = new NettyDataBufferFactory(UnpooledByteBufAllocator.DEFAULT)
				.wrap(Unpooled.wrappedBuffer(new byte[] {1}));
		source.get().next(new WebSocketMessage(WebSocketMessage.Type.BINARY, payload));
		assertThat(payload.getNativeBuffer().refCnt()).isZero();
	}

	@Test
	void sendCancellationReleasesQueuedPayload() {
		NettyDataBufferFactory factory = new NettyDataBufferFactory(UnpooledByteBufAllocator.DEFAULT);
		NettyDataBuffer first = factory.wrap(Unpooled.wrappedBuffer(new byte[] {1}));
		NettyDataBuffer second = factory.wrap(Unpooled.wrappedBuffer(new byte[] {2}));
		when(this.webSocket.sendBinary(any(), eq(true))).thenReturn(new CompletableFuture<>());
		Disposable subscription = this.session.send(Flux.create(sink -> {
			sink.next(new WebSocketMessage(WebSocketMessage.Type.BINARY, first));
			sink.next(new WebSocketMessage(WebSocketMessage.Type.BINARY, second));
		})).subscribe();
		subscription.dispose();
		assertThat(first.getNativeBuffer().refCnt()).isZero();
		assertThat(second.getNativeBuffer().refCnt()).isZero();
	}

	@Test
	@Timeout(30)
	void concurrentSendCancellationReleasesPayload() throws Exception {
		NettyDataBufferFactory factory = new NettyDataBufferFactory(UnpooledByteBufAllocator.DEFAULT);
		when(this.webSocket.sendBinary(any(), eq(true))).thenAnswer(invocation -> new CompletableFuture<>());
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			for (int i = 0; i < 1000; i++) {
				JdkWebSocketSession session = new JdkWebSocketSession(this.webSocket,
						this.session.getHandshakeInfo(), factory, 16);
				AtomicReference<FluxSink<WebSocketMessage>> source = new AtomicReference<>();
				Disposable subscription = session.send(Flux.create(source::set)).subscribe();
				NettyDataBuffer payload = factory.wrap(Unpooled.wrappedBuffer(new byte[] {1}));
				CyclicBarrier barrier = new CyclicBarrier(2);
				Future<?> sending = executor.submit(() -> {
					barrier.await();
					source.get().next(new WebSocketMessage(WebSocketMessage.Type.BINARY, payload));
					return null;
				});
				Future<?> cancelling = executor.submit(() -> {
					barrier.await();
					subscription.dispose();
					return null;
				});
				sending.get();
				cancelling.get();
				assertThat(payload.getNativeBuffer().refCnt()).as("iteration %s", i).isZero();
			}
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	void closeDoesNotRequireReceiveDemandAndNormalizesReason() {
		when(this.webSocket.sendClose(1000, "")).thenReturn(CompletableFuture.completedFuture(this.webSocket));
		this.session.close().block();
		verify(this.webSocket).sendClose(1000, "");
		verify(this.webSocket, never()).request(1);
		StepVerifier.create(this.session.closeStatus()).expectNext(CloseStatus.NORMAL).verifyComplete();
	}

	@Test
	void remoteCloseCancelsPendingSendSource() {
		AtomicBoolean cancelled = new AtomicBoolean();
		StepVerifier.create(this.session.send(Flux.<WebSocketMessage>never().doOnCancel(() -> cancelled.set(true))))
				.then(() -> this.session.onClose(this.webSocket, 1000, ""))
				.verifyComplete();
		assertThat(cancelled).isTrue();
	}

	@Test
	void nativeErrorTerminatesPendingSend() {
		IllegalStateException error = new IllegalStateException("connection failed");
		StepVerifier.create(this.session.send(Flux.never()))
				.then(() -> this.session.onError(this.webSocket, error))
				.expectErrorSatisfies(actual -> assertThat(actual).isSameAs(error)).verify();
	}

	@Test
	void unsupportedIncomingCloseStatusTerminatesReceive() {
		StepVerifier.create(this.session.receive())
				.then(() -> this.session.onClose(this.webSocket, 65535, ""))
				.verifyError(IllegalArgumentException.class);
	}

}
