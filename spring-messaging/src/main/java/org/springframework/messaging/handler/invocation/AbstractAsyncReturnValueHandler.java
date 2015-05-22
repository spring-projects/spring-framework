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
import org.springframework.messaging.Message;

/**
 * Abstract base class for {@link AsyncHandlerMethodReturnValueHandler} implementations
 * only intended for asynchronous return value handling.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public abstract class AbstractAsyncReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) throws Exception {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		return true;
	}
}
