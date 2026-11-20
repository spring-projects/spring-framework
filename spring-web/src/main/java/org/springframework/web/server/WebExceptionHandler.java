/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server;

import reactor.core.publisher.Mono;

/**
 * Contract for handling exceptions during web server exchange processing.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebExceptionHandler {

	/**
	 * Handle the given exception. A completion signal through the return value
	 * indicates error handling is complete while an error signal indicates the
	 * exception is still not handled.
	 * @param exchange the current exchange
	 * @param ex the exception to handle
	 * @return {@code Mono<Void>} to indicate when exception handling is complete
	 */
	Mono<Void> handle(ServerWebExchange exchange, Throwable ex);

}
