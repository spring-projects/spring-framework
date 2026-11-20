/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * Strategy to resolve the argument value for a method parameter in the context
 * of the current HTTP request.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface HandlerMethodArgumentResolver {

	/**
	 * Whether this resolver supports the given method parameter.
	 * @param parameter the method parameter
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * Resolve the value for the method parameter.
	 * @param parameter the method parameter
	 * @param bindingContext the binding context to use
	 * @param exchange the current exchange
	 * @return {@code Mono} for the argument value, possibly empty
	 */
	Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange);

}
