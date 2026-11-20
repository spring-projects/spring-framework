/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.invocation;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;

/**
 * Support for single-value reactive types (like {@code Mono} or {@code Single})
 * as a return value type.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 */
public class ReactiveReturnValueHandler extends AbstractAsyncReturnValueHandler {

	private final ReactiveAdapterRegistry adapterRegistry;


	public ReactiveReturnValueHandler() {
		this(ReactiveAdapterRegistry.getSharedInstance());
	}

	public ReactiveReturnValueHandler(ReactiveAdapterRegistry adapterRegistry) {
		this.adapterRegistry = adapterRegistry;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (this.adapterRegistry.getAdapter(returnType.getParameterType()) != null);
	}

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		ReactiveAdapter adapter = this.adapterRegistry.getAdapter(returnType.getParameterType(), returnValue);
		return (adapter != null && !adapter.isMultiValue() && !adapter.isNoValue());
	}

	@Override
	public @Nullable CompletableFuture<?> toCompletableFuture(Object returnValue, MethodParameter returnType) {
		ReactiveAdapter adapter = this.adapterRegistry.getAdapter(returnType.getParameterType(), returnValue);
		if (adapter != null) {
			return Mono.from(adapter.toPublisher(returnValue)).toFuture();
		}
		return null;
	}
}
