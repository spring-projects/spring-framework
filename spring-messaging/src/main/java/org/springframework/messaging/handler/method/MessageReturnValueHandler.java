/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.method;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;


/**
 * Strategy interface to handle the value returned from the invocation of a
 * method handling a {@link Message}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface MessageReturnValueHandler {

	/**
	 * Whether the given {@linkplain MethodParameter method return type} is
	 * supported by this handler.
	 *
	 * @param returnType the method return type to check
	 * @return {@code true} if this handler supports the supplied return type;
	 * {@code false} otherwise
	 */
	boolean supportsReturnType(MethodParameter returnType);

	/**
	 * Handle the given return value.
	 *
	 * @param returnValue the value returned from the handler method
	 * @param returnType the type of the return value. This type must have
	 * previously been passed to
	 * {@link #supportsReturnType(org.springframework.core.MethodParameter)}
	 * and it must have returned {@code true}
	 * @param message the message that caused this method to be called
	 * @throws Exception if the return value handling results in an error
	 */
	void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) throws Exception;

}
