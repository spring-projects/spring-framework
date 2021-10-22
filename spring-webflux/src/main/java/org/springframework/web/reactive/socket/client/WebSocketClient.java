/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.socket.client;

import java.net.URI;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * Contract for reactive-style handling of a WebSocket session.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketClient {

	/**
	 * Execute a handshake request to the given url and handle the resulting
	 * WebSocket session with the given handler.
	 * @param url the handshake url
	 * @param handler the handler of the WebSocket session
	 * @return completion {@code Mono<Void>} to indicate the outcome of the
	 * WebSocket session handling.
	 */
	Mono<Void> execute(URI url, WebSocketHandler handler);

	/**
	 * A variant of {@link #execute(URI, WebSocketHandler)} with custom headers.
	 * @param url the handshake url
	 * @param headers custom headers for the handshake request
	 * @param handler the handler of the WebSocket session
	 * @return completion {@code Mono<Void>} to indicate the outcome of the
	 * WebSocket session handling.
	 */
	Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler);

}
