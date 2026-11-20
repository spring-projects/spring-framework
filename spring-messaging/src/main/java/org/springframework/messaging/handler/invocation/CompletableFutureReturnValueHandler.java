/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.invocation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.springframework.core.MethodParameter;

/**
 * Support for {@link CompletableFuture} (and as of 4.3.7 also {@link CompletionStage})
 * as a return value type.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.2
 */
public class CompletableFutureReturnValueHandler extends AbstractAsyncReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return CompletionStage.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public CompletableFuture<?> toCompletableFuture(Object returnValue, MethodParameter returnType) {
		return ((CompletionStage<?>) returnValue).toCompletableFuture();
	}
}
