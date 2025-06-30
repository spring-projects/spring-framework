/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;

/**
 * An extension of {@link HandlerMethodReturnValueHandler} for handling async,
 * Future-like return value types that support success and error callbacks.
 *
 * <p>Implementations should consider extending the convenient base class
 * {@link AbstractAsyncReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see AbstractAsyncReturnValueHandler
 */
public interface AsyncHandlerMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

	/**
	 * Whether the return value represents an asynchronous, Future-like type
	 * with success and error callbacks. If this method returns {@code true},
	 * then {@link #toCompletableFuture} is invoked next. If it returns
	 * {@code false}, then {@link #handleReturnValue} is called.
	 * <p><strong>Note:</strong> this method will only be invoked after
	 * {@link #supportsReturnType(org.springframework.core.MethodParameter)}
	 * is called and it returns {@code true}.
	 * @param returnValue the value returned from the handler method
	 * @param returnType the type of the return value
	 * @return {@code true} if the return value type represents an async value
	 */
	boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType);

	/**
	 * Adapt the asynchronous return value to a {@link CompletableFuture}.
	 * <p>Return value handling will then continue when
	 * the CompletableFuture is completed with either success or error.
	 * <p><strong>Note:</strong> this method will only be invoked after
	 * {@link #supportsReturnType(org.springframework.core.MethodParameter)}
	 * is called and it returns {@code true}.
	 * @param returnValue the value returned from the handler method
	 * @param returnType the type of the return value
	 * @return the resulting CompletableFuture, or {@code null} in which case
	 * no further handling will be performed
	 * @since 6.0
	 */
	@Nullable CompletableFuture<?> toCompletableFuture(Object returnValue, MethodParameter returnType);

}
