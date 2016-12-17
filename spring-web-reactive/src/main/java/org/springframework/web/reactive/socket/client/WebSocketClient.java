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
package org.springframework.web.reactive.socket.client;

import java.net.URI;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Contract for starting a WebSocket interaction.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketClient {

	/**
	 * Start a WebSocket interaction to the given url.
	 * @param url the handshake url
	 * @return the session for the WebSocket interaction
	 */
	Mono<WebSocketSession> connect(URI url);

	/**
	 * Start a WebSocket interaction to the given url.
	 * @param url the handshake url
	 * @param headers headers for the handshake request
	 * @return the session for the WebSocket interaction
	 */
	Mono<WebSocketSession> connect(URI url, HttpHeaders headers);

}
