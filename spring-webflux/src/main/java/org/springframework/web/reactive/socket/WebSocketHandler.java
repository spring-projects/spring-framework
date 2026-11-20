/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket;

import java.util.Collections;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Handler for a WebSocket messages. You can use it as follows:
 * <ul>
 * <li>On the server side, {@code WebSocketHandler} is mapped to requests with
 * {@link org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
 * SimpleUrlHandlerMapping} and
 * {@link org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
 * WebSocketHandlerAdapter}.
 * <li>On the client side, {@code WebSocketHandler} is passed into the
 * {@link org.springframework.web.reactive.socket.client.WebSocketClient
 * WebSocketClient} execute method.
 * </ul>
 *
 * <p>{@link WebSocketSession#receive() session.receive()} handles inbound
 * messages, while {@link WebSocketSession#send(Publisher) session.send}
 * sends outbound messages. Below is an example of handling inbound messages
 * and responding to every message:
 *
 * <pre class="code">
 *	class ExampleHandler implements WebSocketHandler {
 *
 *		&#064;Override
 *		public Mono&lt;Void&gt; handle(WebSocketSession session) {
 *			Flux&lt;WebSocketMessage&gt; output = session.receive()
 * 				.doOnNext(message -&gt; {
 * 					// Imperative calls without a return value:
 * 					// perform access checks, log, validate, update metrics.
 * 					// ...
 * 				})
 * 				.concatMap(message -&gt; {
 * 					// Async, non-blocking calls:
 * 					// parse messages, call a database, make remote calls.
 * 					// Return the same message, or a transformed value
 * 					// ...
 * 				});
 * 			return session.send(output);
 *		}
 *	}
 * </pre>
 *
 * <p>If processing inbound and sending outbound messages are independent
 * streams, they can be joined together with the "and" operator:
 *
 * <pre class="code">
 *	class ExampleHandler implements WebSocketHandler {
 *
 *		&#064;Override
 *		public Mono&lt;Void&gt; handle(WebSocketSession session) {
 *
 *			Mono&lt;Void&gt; input = session.receive()
 *				.doOnNext(message -&gt; {
 * 					// ...
 * 				})
 * 				.concatMap(message -&gt; {
 * 					// ...
 * 				})
 * 				.then();
 *
 *			Flux&lt;String&gt; source = ... ;
 * 			Mono&lt;Void&gt; output = session.send(source.map(session::textMessage));
 *
 * 			return input.and(output);
 *		}
 *	}
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
	 * <p>See the class-level doc and the reference manual for more details and
	 * examples of how to handle the session.
	 * @param session the session to handle
	 * @return indicates when application handling of the session is complete,
	 * which should reflect the completion of the inbound message stream
	 * (i.e. connection closing) and possibly the completion of the outbound
	 * message stream and the writing of messages
	 */
	Mono<Void> handle(WebSocketSession session);

}
