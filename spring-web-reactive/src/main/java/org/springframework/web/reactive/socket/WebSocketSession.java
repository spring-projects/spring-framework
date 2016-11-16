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

import java.net.URI;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;

/**
 * Representation for a WebSocket session.
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
	 * Return the WebSocket endpoint URI.
	 */
	URI getUri();

	/**
	 * Get the flux of incoming messages.
	 * <p><strong>Note:</strong> the caller of this method is responsible for
	 * releasing the DataBuffer payload of each message after consuming it
	 * on runtimes where a {@code PooledByteBuffer} is used such as Netty.
	 * @see org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer)
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

}
