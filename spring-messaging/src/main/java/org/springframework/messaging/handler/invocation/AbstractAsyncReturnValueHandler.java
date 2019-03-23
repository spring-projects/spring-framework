/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * Convenient base class for {@link AsyncHandlerMethodReturnValueHandler}
 * implementations that support only asynchronous (Future-like) return values
 * and merely serve as adapters of such types to Spring's
 * {@link org.springframework.util.concurrent.ListenableFuture ListenableFuture}.
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
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) {
		// Should never be called since we return "true" from isAsyncReturnValue
		throw new IllegalStateException("Unexpected invocation");
	}

}
