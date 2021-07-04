/*
 * Copyright 2002-2020 the original author or authors.
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
package org.springframework.web.reactive.socket;

import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

/**
 * Represents a WebSocket session.
 *
 * <p>Use {@link WebSocketSession#receive() session.receive()} to compose on
 * the inbound message stream, and {@link WebSocketSession#send(Publisher)
 * session.send(publisher)} to provide the outbound message stream.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketSession {

	/**
	 * Return the id for the session.
	 */
	String getId();

	/**
	 * Return information from the handshake request.
	 */
	HandshakeInfo getHandshakeInfo();

	/**
	 * Return a {@code DataBuffer} Factory to create message payloads.
	 * @return the buffer factory for the session
	 */
	DataBufferFactory bufferFactory();

	/**
	 * Return the map with attributes associated with the WebSocket session.
	 * @return a Map with the session attributes (never {@code null})
	 * @since 5.1
	 */
	Map<String, Object> getAttributes();

	/**
	 * Provides access to the stream of inbound messages.
	 * <p>This stream receives a completion or error signal when the connection
	 * is closed. In a typical {@link WebSocketHandler} implementation this
	 * stream is composed into the overall processing flow, so that when the
	 * connection is closed, handling will end.
	 * <p>See the class-level doc of {@link WebSocketHandler} and the reference
	 * for more details and examples of how to handle the session.
	 */
	Flux<WebSocketMessage> receive();

	/**
	 * Give a source of outgoing messages, write the messages and return a
	 * {@code Mono<Void>} that completes when the source completes and writing
	 * is done.
	 * <p>See the class-level doc of {@link WebSocketHandler} and the reference
	 * for more details and examples of how to handle the session.
	 */
	Mono<Void> send(Publisher<WebSocketMessage> messages);

	/**
	 * Close the WebSocket session with {@link CloseStatus#NORMAL}.
	 */
	default Mono<Void> close() {
		return close(CloseStatus.NORMAL);
	}

	/**
	 * Close the WebSocket session with the given status.
	 * @param status the close status
	 */
	Mono<Void> close(CloseStatus status);


	// WebSocketMessage factory methods

	/**
	 * Factory method to create a text {@link WebSocketMessage} using the
	 * {@link #bufferFactory()} for the session.
	 */
	WebSocketMessage textMessage(String payload);

	/**
	 * Factory method to create a binary WebSocketMessage using the
	 * {@link #bufferFactory()} for the session.
	 */
	WebSocketMessage binaryMessage(Function<DataBufferFactory, DataBuffer> payloadFactory);

	/**
	 * Factory method to create a ping WebSocketMessage using the
	 * {@link #bufferFactory()} for the session.
	 */
	WebSocketMessage pingMessage(Function<DataBufferFactory, DataBuffer> payloadFactory);

	/**
	 * Factory method to create a pong WebSocketMessage using the
	 * {@link #bufferFactory()} for the session.
	 */
	WebSocketMessage pongMessage(Function<DataBufferFactory, DataBuffer> payloadFactory);

}
