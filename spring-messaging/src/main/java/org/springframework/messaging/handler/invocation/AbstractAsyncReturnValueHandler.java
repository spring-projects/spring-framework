/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.invocation;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * Convenient base class for {@link AsyncHandlerMethodReturnValueHandler}
 * implementations that support only asynchronous (Future-like) return values.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public abstract class AbstractAsyncReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		return true;
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, Message<?> message) {
		// Should never be called since we return "true" from isAsyncReturnValue
		throw new IllegalStateException("Unexpected invocation");
	}

}
