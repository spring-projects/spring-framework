/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.invocation;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.messaging.support.MonoToListenableFutureAdapter;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

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
		return this.adapterRegistry.getAdapter(returnType.getParameterType()) != null;
	}

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		ReactiveAdapter adapter = this.adapterRegistry.getAdapter(returnType.getParameterType(), returnValue);
		return (adapter != null && !adapter.isMultiValue() && !adapter.isNoValue());
	}

	@Override
	public ListenableFuture<?> toListenableFuture(Object returnValue, MethodParameter returnType) {
		ReactiveAdapter adapter = this.adapterRegistry.getAdapter(returnType.getParameterType(), returnValue);
		Assert.state(adapter != null, () -> "No ReactiveAdapter found for " + returnType.getParameterType());
		return new MonoToListenableFutureAdapter<>(Mono.from(adapter.toPublisher(returnValue)));
	}

}
