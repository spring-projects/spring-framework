/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging;

import reactor.core.publisher.Mono;

/**
 * Reactive contract for handling a {@link Message}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see MessageHandler
 */
@FunctionalInterface
public interface ReactiveMessageHandler {

	/**
	 * Handle the given message.
	 * @param message the message to be handled
	 * @return a completion {@link Mono} for the result of the message handling
	 */
	Mono<Void> handleMessage(Message<?> message);

}
