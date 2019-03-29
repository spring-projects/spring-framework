/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Handler for a WebSocket session.
 *
 * <p>A server {@code WebSocketHandler} is mapped to requests with
 * {@link org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
 * SimpleUrlHandlerMapping} and
 * {@link org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
 * WebSocketHandlerAdapter}. A client {@code WebSocketHandler} is passed to the
 * {@link org.springframework.web.reactive.socket.client.WebSocketClient
 * WebSocketClient} execute method.
 *
 * <p>Use {@link WebSocketSession#receive() session.receive()} to compose on
 * the inbound message stream, and {@link WebSocketSession#send(Publisher)
 * session.send(publisher)} for the outbound message stream. Below is an
 * example, combined flow to process inbound and to send outbound messages:
 *
 * <pre class="code">
 * class ExampleHandler implements WebSocketHandler {

 * 	&#064;Override
 * 	public Mono&lt;Void&gt; handle(WebSocketSession session) {
 *
 * 		Flux&lt;WebSocketMessage&gt; output = session.receive()
 *			.doOnNext(message -> {
 * 				// ...
 * 			})
 * 			.concatMap(message -> {
 * 				// ...
 * 			})
 * 			.map(value -> session.textMessage("Echo " + value));
 *
 * 		return session.send(output);
 * 	}
 * }
 * </pre>
 *
 * <p>If processing inbound and sending outbound messages are independent
 * streams, they can be joined together with the "zip" operator:
 *
 * <pre class="code">
 * class ExampleHandler implements WebSocketHandler {

 * 	&#064;Override
 * 	public Mono&lt;Void&gt; handle(WebSocketSession session) {
 *
 * 		Mono&lt;Void&gt; input = session.receive()
 *			.doOnNext(message -> {
 * 				// ...
 * 			})
 * 			.concatMap(message -> {
 * 				// ...
 * 			})
 * 			.then();
 *
 *		Flux&lt;String&gt; source = ... ;
 * 		Mono&lt;Void&gt; output = session.send(source.map(session::textMessage));
 *
 * 		return Mono.zip(input, output).then();
 * 	}
 * }
 * </pre>
 *
 * <p>A {@code WebSocketHandler} must compose the inbound and outbound streams
 * into a unified flow and return a {@code Mono<Void>} that reflects the
 * completion of that flow. That means there is no need to check if the
 * connection is open, since Reactive Streams signals will terminate activity.
 * The inbound stream receives a completion/error signal, and the outbound
 * stream receives a cancellation signal.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketHandler {

	/**
	 * Return the list of sub-protocols supported by this handler.
	 * <p>By default an empty list is returned.
	 */
	default List<String> getSubProtocols() {
		return Collections.emptyList();
	}

	/**
	 * Invoked when a new WebSocket connection is established, and allows
	 * handling of the session.
	 *
	 * <p>See the class-level doc and the reference for more details and
	 * examples of how to handle the session.
	 *
	 * @param session the session to handle
	 * @return indicates when appilcation handling of the session is complete,
	 * which should reflect the completion of the inbound message stream
	 * (i.e. connection closing) and possibly the completion of the outbound
	 * message stream and the writing of messages.
	 */
	Mono<Void> handle(WebSocketSession session);

}
