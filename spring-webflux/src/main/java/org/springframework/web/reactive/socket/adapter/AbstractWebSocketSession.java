/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * @param <T> the native delegate type
 */
public abstract class AbstractWebSocketSession<T> implements WebSocketSession {

	protected final Log logger = LogFactory.getLog(getClass());

	private final T delegate;

	private final String id;

	private final HandshakeInfo handshakeInfo;

	private final DataBufferFactory bufferFactory;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	private final String logPrefix;


	/**
	 * Create a new WebSocket session.
	 */
	protected AbstractWebSocketSession(T delegate, String id, HandshakeInfo info, DataBufferFactory bufferFactory) {
		Assert.notNull(delegate, "Native session is required");
		Assert.notNull(id, "Session id is required");
		Assert.notNull(info, "HandshakeInfo is required");
		Assert.notNull(bufferFactory, "DataBuffer factory is required");

		this.delegate = delegate;
		this.id = id;
		this.handshakeInfo = info;
		this.bufferFactory = bufferFactory;
		this.logPrefix = initLogPrefix(info, id);

		info.getAttributes().entrySet().stream()
				.filter(entry -> (entry.getKey() != null && entry.getValue() != null))
				.forEach(entry -> this.attributes.put(entry.getKey(), entry.getValue()));

		if (logger.isDebugEnabled()) {
			logger.debug(getLogPrefix() + "Session id \"" + getId() + "\" for " + getHandshakeInfo().getUri());
		}
	}

	private static String initLogPrefix(HandshakeInfo info, String id) {
		return info.getLogPrefix() != null ? info.getLogPrefix() : "[" + id + "] ";
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
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	protected String getLogPrefix() {
		return this.logPrefix;
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
