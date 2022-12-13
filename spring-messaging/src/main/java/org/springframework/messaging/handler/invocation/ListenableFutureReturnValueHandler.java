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

import org.springframework.core.MethodParameter;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Support for {@link ListenableFuture} as a return value type.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @deprecated as of 6.0, in favor of {@link CompletableFutureReturnValueHandler}
 */
@Deprecated(since = "6.0")
public class ListenableFutureReturnValueHandler extends AbstractAsyncReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return ListenableFuture.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	@SuppressWarnings("unchecked")
	public ListenableFuture<?> toListenableFuture(Object returnValue, MethodParameter returnType) {
		return (ListenableFuture<?>) returnValue;
	}

	@Override
	public CompletableFuture<?> toCompletableFuture(Object returnValue, MethodParameter returnType) {
		return ((ListenableFuture<?>) returnValue).completable();
	}
}
