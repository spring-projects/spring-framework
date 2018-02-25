/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.reactive.socket;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

/**
 * Represents a WebSocket session with Reactive Streams input and output.
 *
 * <p>On the server side a WebSocket session can be handled by mapping
 * requests to a {@link WebSocketHandler} and ensuring there is a
 * {@link org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
 * WebSocketHandlerAdapter} strategy registered in Spring configuration.
 * On the client side a {@link WebSocketHandler} can be provided to a
 * {@link org.springframework.web.reactive.socket.client.WebSocketClient
 * WebSocketClient}.
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
	 * Get access to the stream of incoming messages.
	 */
	Flux<WebSocketMessage> receive();

	/**
	 * Write the given messages to the WebSocket connection.
	 * @param messages the messages to write
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
