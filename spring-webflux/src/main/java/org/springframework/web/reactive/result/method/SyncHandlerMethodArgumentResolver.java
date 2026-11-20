/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * An extension of {@link HandlerMethodArgumentResolver} for implementations
 * that are synchronous in nature and do not block to resolve values.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface SyncHandlerMethodArgumentResolver extends HandlerMethodArgumentResolver {

	/**
	 * {@inheritDoc}
	 * <p>By default this simply delegates to {@link #resolveArgumentValue} for
	 * synchronous resolution.
	 */
	@Override
	default Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		return Mono.justOrEmpty(resolveArgumentValue(parameter, bindingContext, exchange));
	}

	/**
	 * Resolve the value for the method parameter synchronously.
	 * @param parameter the method parameter
	 * @param bindingContext the binding context to use
	 * @param exchange the current exchange
	 * @return the resolved value, if any
	 */
	@Nullable Object resolveArgumentValue(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange);

}
