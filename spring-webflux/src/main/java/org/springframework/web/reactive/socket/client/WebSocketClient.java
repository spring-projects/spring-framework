/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
