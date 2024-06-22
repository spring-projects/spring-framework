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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.IteratingCallback;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Spring {@link WebSocketSession} implementation that adapts to a Jetty
 * WebSocket {@link Session}.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class JettyWebSocketSession extends AbstractWebSocketSession<Session> {

	private final Flux<WebSocketMessage> flux;

	private final Sinks.One<CloseStatus> closeStatusSink = Sinks.one();

	private final Lock lock = new ReentrantLock();

	private long requested = 0;

	private boolean awaitingMessage = false;

	@Nullable
	private FluxSink<WebSocketMessage> sink;

	@Nullable
	private final Sinks.Empty<Void> handlerCompletionSink;

	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
		this(session, info, factory, null);
	}

	public JettyWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
			@Nullable Sinks.Empty<Void> completionSink) {

		super(session, ObjectUtils.getIdentityHexString(session), info, factory);
		this.handlerCompletionSink = completionSink;
		this.flux = Flux.create(emitter -> {
			this.sink = emitter;
			emitter.onRequest(n -> {
				boolean demand = false;
				this.lock.lock();
				try {
					this.requested = Math.addExact(this.requested, n);
					if (this.requested < 0L) {
						this.requested = Long.MAX_VALUE;
					}

					if (!this.awaitingMessage && this.requested > 0) {
						if (this.requested != Long.MAX_VALUE) {
							this.requested--;
						}
						this.awaitingMessage = true;
						demand = true;
					}
				}
				finally {
					this.lock.unlock();
				}

				if (demand) {
					getDelegate().demand();
				}
			});
		});
	}

	void handleMessage(WebSocketMessage message) {
		Assert.state(this.sink != null, "No sink available");
		this.sink.next(message);

		boolean demand = false;
		this.lock.lock();
		try {
			if (!this.awaitingMessage) {
				throw new IllegalStateException();
			}
			this.awaitingMessage = false;
			if (this.requested > 0) {
				if (this.requested != Long.MAX_VALUE) {
					this.requested--;
				}
				this.awaitingMessage = true;
				demand = true;
			}
		}
		finally {
			this.lock.unlock();
		}

		if (demand) {
			getDelegate().demand();
		}
	}

	void handleError(Throwable ex) {
	}

	void handleClose(CloseStatus closeStatus) {
		this.closeStatusSink.tryEmitValue(closeStatus);
		if (this.sink != null) {
			this.sink.complete();
		}
	}

	void onHandlerError(Throwable error) {
		if (JettyWebSocketSession.this.handlerCompletionSink != null) {
			JettyWebSocketSession.this.handlerCompletionSink.tryEmitError(error);
		}
		getDelegate().close(StatusCode.SERVER_ERROR, error.getMessage(), Callback.NOOP);
	}

	void onHandleComplete() {
		if (JettyWebSocketSession.this.handlerCompletionSink != null) {
			JettyWebSocketSession.this.handlerCompletionSink.tryEmitEmpty();
		}
		getDelegate().close(StatusCode.NORMAL, null, Callback.NOOP);
	}

	@Override
	public boolean isOpen() {
		return getDelegate().isOpen();
	}

	@Override
	public Mono<Void> close(CloseStatus status) {
		Callback.Completable callback = new Callback.Completable();
		getDelegate().close(status.getCode(), status.getReason(), callback);
		return Mono.fromFuture(callback);
	}

	@Override
	public Mono<CloseStatus> closeStatus() {
		return this.closeStatusSink.asMono();
	}

	@Override
	public Flux<WebSocketMessage> receive() {
		return this.flux;
	}

	@Override
	public Mono<Void> send(Publisher<WebSocketMessage> messages) {
		return Flux.from(messages)
				.flatMap(this::sendMessage, 1)
				.then();
	}

	protected Mono<Void> sendMessage(WebSocketMessage message) {

		Callback.Completable completable = new Callback.Completable();
		DataBuffer dataBuffer = message.getPayload();
		Session session = getDelegate();
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			String text = dataBuffer.toString(StandardCharsets.UTF_8);
			session.sendText(text, completable);
		}
		else {
			switch (message.getType()) {
				case BINARY -> {
					@SuppressWarnings("resource")
					DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers();
					new IteratingCallback() {
						@Override
						protected Action process() {
							if (!iterator.hasNext()) {
								return Action.SUCCEEDED;
							}

							ByteBuffer buffer = iterator.next();
							boolean last = iterator.hasNext();
							session.sendPartialBinary(buffer, last, Callback.from(this::succeeded, this::failed));
							return Action.SCHEDULED;
						}

						@Override
						protected void onCompleteSuccess() {
							iterator.close();
							completable.succeed();
						}

						@Override
						protected void onCompleteFailure(Throwable cause) {
							iterator.close();
							completable.fail(cause);
						}
					}.iterate();
				}
				case PING -> {
					// Maximum size of Control frame payload is 125, per RFC 6455.
					ByteBuffer buffer = BufferUtil.allocate(125);
					dataBuffer.toByteBuffer(buffer);
					session.sendPing(buffer, completable);
				}
				case PONG -> {
					// Maximum size of Control frame payload is 125, per RFC 6455.
					ByteBuffer buffer = BufferUtil.allocate(125);
					dataBuffer.toByteBuffer(buffer);
					session.sendPong(buffer, completable);
				}
				default -> throw new IllegalArgumentException("Unexpected message type: " + message.getType());
			}
		}
		return Mono.fromFuture(completable);
	}
}
