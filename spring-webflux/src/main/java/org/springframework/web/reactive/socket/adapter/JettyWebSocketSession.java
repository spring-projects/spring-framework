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
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
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
	private final AtomicLong requested = new AtomicLong(0);

	private final Sinks.One<CloseStatus> closeStatusSink = Sinks.one();
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
			emitter.onRequest(n ->
			{
				requested.addAndGet(n);
				tryDemand();
			});
		});
	}

	void handleMessage(WebSocketMessage.Type type, WebSocketMessage message) {
		this.sink.next(message);
		tryDemand();
	}

	void handleError(Throwable ex) {
	}

	void handleClose(CloseStatus closeStatus) {
		this.closeStatusSink.tryEmitValue(closeStatus);
		this.sink.complete();
	}

	void onHandlerError(Throwable ex) {
		if (this.handlerCompletionSink != null) {
			// Ignore result: can't overflow, ok if not first or no one listens
			this.handlerCompletionSink.tryEmitError(ex);
		}
		close(CloseStatus.SERVER_ERROR);
	}

	void onHandleComplete() {
		if (this.handlerCompletionSink != null) {
			// Ignore result: can't overflow, ok if not first or no one listens
			this.handlerCompletionSink.tryEmitEmpty();
		}
		close();
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
		return closeStatusSink.asMono();
	}

	@Override
	public Flux<WebSocketMessage> receive() {
		return flux;
	}

	private void tryDemand()
	{
		while (true)
		{
			long r = requested.get();
			if (r == 0)
				return;

			// TODO: protect against readpending from multiple demand.
			if (requested.compareAndSet(r, r - 1))
			{
				getDelegate().demand();
				return;
			}
		}
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
			// TODO: Ping and Pong message should combine payload into single buffer?
			try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
				while (iterator.hasNext()) {
					ByteBuffer byteBuffer = iterator.next();
					switch (message.getType()) {
						case BINARY -> session.sendBinary(byteBuffer, completable);
						case PING -> session.sendPing(byteBuffer, completable);
						case PONG -> session.sendPong(byteBuffer, completable);
						default -> throw new IllegalArgumentException("Unexpected message type: " + message.getType());
					}
				}
			}
		}
		return Mono.fromFuture(completable);
	}
}
