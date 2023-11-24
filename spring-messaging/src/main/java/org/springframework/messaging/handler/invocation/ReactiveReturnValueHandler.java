/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.invocation;

import java.util.concurrent.CompletableFuture;

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
	public CompletableFuture<?> toCompletableFuture(Object returnValue, MethodParameter returnType) {
		ReactiveAdapter adapter = this.adapterRegistry.getAdapter(returnType.getParameterType(), returnValue);
		if (adapter != null) {
			return Mono.from(adapter.toPublisher(returnValue)).toFuture();
		}
		return null;
	}
}
