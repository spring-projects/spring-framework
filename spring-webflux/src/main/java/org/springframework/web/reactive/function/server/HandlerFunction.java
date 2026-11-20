/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.server;

import reactor.core.publisher.Mono;

/**
 * Represents a function that handles a {@linkplain ServerRequest request}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> the type of the response of the function
 * @see RouterFunction
 */
@FunctionalInterface
public interface HandlerFunction<T extends ServerResponse> {

	/**
	 * Handle the given request.
	 * @param request the request to handle
	 * @return the response
	 */
	Mono<T> handle(ServerRequest request);

}
