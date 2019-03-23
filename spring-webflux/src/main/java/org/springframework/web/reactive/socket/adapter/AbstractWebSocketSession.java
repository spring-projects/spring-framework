/*
 * Copyright 2002-2017 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Convenient base class for {@link WebSocketSession} implementations that
 * holds common fields and exposes accessors. Also implements the
 * {@code WebSocketMessage} factory methods.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractWebSocketSession<T> implements WebSocketSession {

	private final T delegate;

	private final String id;

	private final HandshakeInfo handshakeInfo;

	private final DataBufferFactory bufferFactory;


	/**
	 * Create a new instance and associate the given attributes with it.
	 */
	protected AbstractWebSocketSession(T delegate, String id, HandshakeInfo handshakeInfo,
			DataBufferFactory bufferFactory) {

		Assert.notNull(delegate, "Native session is required.");
		Assert.notNull(id, "Session id is required.");
		Assert.notNull(handshakeInfo, "HandshakeInfo is required.");
		Assert.notNull(bufferFactory, "DataBuffer factory is required.");

		this.delegate = delegate;
		this.id = id;
		this.handshakeInfo = handshakeInfo;
		this.bufferFactory = bufferFactory;
	}


	protected T getDelegate() {
		return this.delegate;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public HandshakeInfo getHandshakeInfo() {
		return this.handshakeInfo;
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	@Override
	public abstract Flux<WebSocketMessage> receive();

	@Override
	public abstract Mono<Void> send(Publisher<WebSocketMessage> messages);


	// WebSocketMessage factory methods

	@Override
	public WebSocketMessage textMessage(String payload) {
		byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = bufferFactory().wrap(bytes);
		return new WebSocketMessage(WebSocketMessage.Type.TEXT, buffer);
	}

	@Override
	public WebSocketMessage binaryMessage(Function<DataBufferFactory, DataBuffer> payloadFactory) {
		DataBuffer payload = payloadFactory.apply(bufferFactory());
		return new WebSocketMessage(WebSocketMessage.Type.BINARY, payload);
	}

	@Override
	public WebSocketMessage pingMessage(Function<DataBufferFactory, DataBuffer> payloadFactory) {
		DataBuffer payload = payloadFactory.apply(bufferFactory());
		return new WebSocketMessage(WebSocketMessage.Type.PING, payload);
	}

	@Override
	public WebSocketMessage pongMessage(Function<DataBufferFactory, DataBuffer> payloadFactory) {
		DataBuffer payload = payloadFactory.apply(bufferFactory());
		return new WebSocketMessage(WebSocketMessage.Type.PONG, payload);
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + getId() + ", uri=" + getHandshakeInfo().getUri() + "]";
	}

}
