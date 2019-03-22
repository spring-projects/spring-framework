/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.handler.invocation.reactive;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Handle the return value from the invocation of an annotated {@link Message}
 * handling method.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public interface HandlerMethodReturnValueHandler {

	/** Header containing a DataBufferFactory for use in return value handling. */
	String DATA_BUFFER_FACTORY_HEADER = "dataBufferFactory";


	/**
	 * Whether the given {@linkplain MethodParameter method return type} is
	 * supported by this handler.
	 * @param returnType the method return type to check
	 * @return {@code true} if this handler supports the supplied return type;
	 * {@code false} otherwise
	 */
	boolean supportsReturnType(MethodParameter returnType);

	/**
	 * Handle the given return value.
	 * @param returnValue the value returned from the handler method
	 * @param returnType the type of the return value. This type must have previously
	 * been passed to {@link #supportsReturnType(MethodParameter)}
	 * and it must have returned {@code true}.
	 * @return {@code Mono<Void>} to indicate when handling is complete.
	 */
	Mono<Void> handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, Message<?> message);

}
