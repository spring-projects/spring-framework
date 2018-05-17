/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Handler for a WebSocket session.
 *
 * <p>Use {@link WebSocketSession#receive()} to compose on the stream of
 * inbound messages and {@link WebSocketSession#send(Publisher)} to write the
 * stream of outbound messages.
 *
 * <p>You can handle inbound and outbound messages as independent streams, and
 * then join them:
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
 * <p>You can also create a single flow including inbound and outbound messages:
 * <pre class="code">
 * class ExampleHandler implements WebSocketHandler {

 * 	&#064;Override
 * 	public Mono&lt;Void&gt; handle(WebSocketSession session) {
 *
 * 		Flux&lt;WebSocketMessage&gt; input = session.receive()
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
 * <p>When the connection is closed, the inbound stream will receive a
 * completion/error signal, while the outbound stream will get a cancellation
 * signal. The above flows are composed in such a way that the
 * {@code Mono<Void>} returned from the {@code WebSocketHandler} won't complete
 * until the connection is closed.
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
	 * Handle the WebSocket session.
	 *
	 *
	 *
	 * @param session the session to handle
	 * @return completion {@code Mono<Void>} to indicate the outcome of the
	 * WebSocket session handling.
	 */
	Mono<Void> handle(WebSocketSession session);

}
