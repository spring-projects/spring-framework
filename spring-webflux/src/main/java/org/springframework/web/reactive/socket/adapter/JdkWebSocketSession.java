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

import java.io.ByteArrayOutputStream;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;

/**
 * Adapt a JDK {@link WebSocket} to a reactive WebSocket session.
 *
 * <p>Incoming fragments are aggregated into complete messages. JDK receive
 * callbacks, including the close callback, are requested according to demand
 * for {@link #receive()}.
 *
 * @author Goutam Adwant
 * @since 7.1
 */
public class JdkWebSocketSession extends AbstractWebSocketSession<WebSocket> implements WebSocket.Listener {

	private final int maxMessageSize;

	private final Flux<WebSocketMessage> receiveFlux;

	private final AtomicBoolean receiveSubscribed = new AtomicBoolean();

	private final AtomicBoolean sendSubscribed = new AtomicBoolean();

	private final AtomicBoolean awaitingCallback = new AtomicBoolean();

	private final AtomicBoolean terminated = new AtomicBoolean();

	private final Sinks.Empty<Void> termination = Sinks.empty();

	private final Sinks.One<CloseStatus> closeStatus = Sinks.one();

	private volatile @Nullable FluxSink<WebSocketMessage> receiveSink;

	private @Nullable ByteArrayOutputStream fragments;


	/**
	 * Create a session for the given JDK WebSocket.
	 * @param webSocket the native WebSocket
	 * @param info handshake information
	 * @param bufferFactory the buffer factory to use
	 * @param maxMessageSize maximum incoming text or binary message size in bytes
	 */
	public JdkWebSocketSession(WebSocket webSocket, HandshakeInfo info,
			DataBufferFactory bufferFactory, int maxMessageSize) {

		super(webSocket, ObjectUtils.getIdentityHexString(webSocket), info, bufferFactory);
		Assert.isTrue(maxMessageSize > 0, "Max message size must be positive");
		this.maxMessageSize = maxMessageSize;
		this.receiveFlux = Flux.<WebSocketMessage>create(sink -> {
			if (!this.receiveSubscribed.compareAndSet(false, true)) {
				sink.error(new IllegalStateException("receive() supports only one subscriber"));
				return;
			}
			this.receiveSink = sink;
			sink.onRequest(n -> requestCallback());
			sink.onCancel(this::clearFragments);
		}, FluxSink.OverflowStrategy.ERROR).takeUntilOther(this.termination.asMono())
				.doOnDiscard(WebSocketMessage.class, WebSocketMessage::release);
	}


	@Override
	public Flux<WebSocketMessage> receive() {
		return this.receiveFlux;
	}

	private void requestCallback() {
		FluxSink<WebSocketMessage> sink = this.receiveSink;
		if (!this.terminated.get() && sink != null && !sink.isCancelled() && sink.requestedFromDownstream() > 0 &&
				!getDelegate().isInputClosed() && this.awaitingCallback.compareAndSet(false, true)) {
			getDelegate().request(1);
		}
	}

	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		return Mono.defer(() -> {
			if (!this.sendSubscribed.compareAndSet(false, true)) {
				return Mono.error(new IllegalStateException("send() has already been called"));
			}
			return Flux.from(messages).concatMap(this::sendMessage)
					.takeUntilOther(this.termination.asMono())
					.doOnDiscard(WebSocketMessage.class, WebSocketMessage::release).then();
		});
	}

	private Mono<Void> sendMessage(WebSocketMessage message) {
		try {
			DataBuffer payload = message.getPayload();
			if (message.getType() == WebSocketMessage.Type.TEXT) {
				String text = payload.toString(StandardCharsets.UTF_8);
				return Mono.fromFuture(getDelegate().sendText(text, true)).then();
			}
			// The JDK may access this copy until its send future completes,
			// independently of cancellation and pooled buffer ownership.
			ByteBuffer bytes = ByteBuffer.allocate(payload.readableByteCount());
			payload.toByteBuffer(bytes);
			return switch (message.getType()) {
				case BINARY -> Mono.fromFuture(getDelegate().sendBinary(bytes, true)).then();
				case PING -> Mono.fromFuture(getDelegate().sendPing(bytes)).then();
				case PONG -> Mono.fromFuture(getDelegate().sendPong(bytes)).then();
				default -> Mono.error(new IllegalArgumentException("Unexpected message type: " + message.getType()));
			};
		}
		finally {
			message.release();
		}
	}

	@Override
	public boolean isOpen() {
		return !getDelegate().isInputClosed() && !getDelegate().isOutputClosed();
	}

	/**
	 * Send a close frame. The status code and reason must satisfy the constraints
	 * of {@link WebSocket#sendClose(int, String)}.
	 */
	@Override
	public Mono<Void> close(CloseStatus status) {
		return Mono.defer(() -> {
			if (getDelegate().isOutputClosed()) {
				return Mono.empty();
			}
			String reason = status.getReason();
			return Mono.fromFuture(getDelegate().sendClose(status.getCode(), (reason != null ? reason : "")))
					.doOnSuccess(webSocket -> this.closeStatus.tryEmitValue(status)).then();
		});
	}

	@Override
	public Mono<CloseStatus> closeStatus() {
		return this.closeStatus.asMono();
	}

	/**
	 * Abort the native connection, for example on cancellation.
	 */
	public void abort() {
		this.terminated.set(true);
		clearFragments();
		getDelegate().abort();
		this.closeStatus.tryEmitEmpty();
		this.termination.tryEmitEmpty();
	}

	@Override
	public void onOpen(WebSocket webSocket) {
		// Demand is driven by the receive subscriber, not the default listener.
	}

	@Override
	public synchronized @Nullable CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		if (!isReceiving()) {
			return null;
		}
		if (data.length() > remainingCapacity()) {
			throw new DataBufferLimitException("Exceeded maximum WebSocket message size: " + this.maxMessageSize);
		}
		byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
		handleFragment(WebSocketMessage.Type.TEXT, ByteBuffer.wrap(bytes), last);
		return null;
	}

	@Override
	public @Nullable CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
		handleFragment(WebSocketMessage.Type.BINARY, data, last);
		return null;
	}

	private int remainingCapacity() {
		return this.maxMessageSize - (this.fragments != null ? this.fragments.size() : 0);
	}

	private synchronized void handleFragment(WebSocketMessage.Type type, ByteBuffer data, boolean last) {
		if (!isReceiving()) {
			return;
		}
		if (data.remaining() > remainingCapacity()) {
			throw new DataBufferLimitException("Exceeded maximum WebSocket message size: " + this.maxMessageSize);
		}
		byte[] bytes = new byte[data.remaining()];
		data.get(bytes);
		if (!last || this.fragments != null) {
			if (this.fragments == null) {
				this.fragments = new ByteArrayOutputStream();
			}
			this.fragments.writeBytes(bytes);
			if (!last) {
				this.awaitingCallback.set(false);
				requestCallback();
				return;
			}
			bytes = this.fragments.toByteArray();
			this.fragments = null;
		}
		handleMessage(new WebSocketMessage(type, bufferFactory().wrap(bytes)));
	}

	private boolean isReceiving() {
		FluxSink<WebSocketMessage> sink = this.receiveSink;
		return !this.terminated.get() && sink != null && !sink.isCancelled();
	}

	private void handleMessage(WebSocketMessage message) {
		FluxSink<WebSocketMessage> sink = this.receiveSink;
		if (sink != null && !sink.isCancelled()) {
			sink.next(message);
		}
		else {
			message.release();
		}
		this.awaitingCallback.set(false);
		requestCallback();
	}

	@Override
	public @Nullable CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
		handleControlMessage(WebSocketMessage.Type.PING, message);
		return null;
	}

	@Override
	public @Nullable CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
		handleControlMessage(WebSocketMessage.Type.PONG, message);
		return null;
	}

	private void handleControlMessage(WebSocketMessage.Type type, ByteBuffer data) {
		byte[] bytes = new byte[data.remaining()];
		data.get(bytes);
		handleMessage(new WebSocketMessage(type, bufferFactory().wrap(bytes)));
	}

	@Override
	public @Nullable CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		try {
			CloseStatus status = CloseStatus.create(statusCode, reason);
			this.terminated.set(true);
			clearFragments();
			this.closeStatus.tryEmitValue(status);
			this.termination.tryEmitEmpty();
		}
		catch (IllegalArgumentException ex) {
			onError(webSocket, ex);
		}
		return null;
	}

	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		this.terminated.set(true);
		clearFragments();
		this.closeStatus.tryEmitEmpty();
		this.termination.tryEmitError(error);
	}

	private synchronized void clearFragments() {
		this.fragments = null;
	}

}
