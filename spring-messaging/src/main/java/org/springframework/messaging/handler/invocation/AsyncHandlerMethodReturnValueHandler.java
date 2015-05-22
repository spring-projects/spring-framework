/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An extension of {@link HandlerMethodReturnValueHandler} for handling async
 * return value types.
 *
 * <p>Implementations only intended for asynchronous return value handling can extend
 * {@link AbstractAsyncReturnValueHandler}.</p>
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see AbstractAsyncReturnValueHandler
 */
public interface AsyncHandlerMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

	/**
	 * Whether the return value type represents a value that will be produced
	 * asynchronously. If this method returns {@code true}, the
	 * {@link #toListenableFuture(Object, MethodParameter)}  will be invoked next.
	 * @param returnValue the value returned from the handler method
	 * @param returnType the type of the return value. This type must have
	 * previously been passed to
	 * {@link #supportsReturnType(org.springframework.core.MethodParameter)}
	 * and it must have returned {@code true}
	 * @return true if the return value type represents an async value.
	 */
	boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType);

	/**
	 * Adapt the given asynchronous return value to a ListenableFuture.
	 * Implementations can return an instance of
	 * {@link org.springframework.util.concurrent.SettableListenableFuture} and
	 * then set it to an Object (success) or a Throwable (failure) to complete
	 * handling.
	 * @param returnValue the value returned from the handler method
	 * @param returnType the type of the return value. This type must have
	 * previously been passed to
	 * {@link #supportsReturnType(org.springframework.core.MethodParameter)}
	 * and it must have returned {@code true}
	 * @return a ListenableFuture
	 */
	ListenableFuture<?> toListenableFuture(Object returnValue, MethodParameter returnType);

}