/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.invocation.reactive;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * An extension of {@link HandlerMethodArgumentResolver} for implementations
 * that are synchronous in nature and do not block to resolve values.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public interface SyncHandlerMethodArgumentResolver extends HandlerMethodArgumentResolver {

	/**
	 * {@inheritDoc}
	 * <p>By default this simply delegates to {@link #resolveArgumentValue} for
	 * synchronous resolution.
	 */
	@Override
	default Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
		return Mono.justOrEmpty(resolveArgumentValue(parameter, message));
	}

	/**
	 * Resolve the value for the method parameter synchronously.
	 * @param parameter the method parameter
	 * @param message the currently processed message
	 * @return the resolved value, if any
	 */
	@Nullable Object resolveArgumentValue(MethodParameter parameter, Message<?> message);

}
