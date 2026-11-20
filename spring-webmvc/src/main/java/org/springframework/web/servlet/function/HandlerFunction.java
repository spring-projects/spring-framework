/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function;

/**
 * Represents a function that handles a {@linkplain ServerRequest request}.
 *
 * @author Arjen Poutsma
 * @since 5.2
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
	T handle(ServerRequest request) throws Exception;

}
