/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.bind.annotation.support;

import java.lang.reflect.Method;

import org.springframework.core.NestedRuntimeException;

/**
 * Exception indicating that the execution of an annotated MVC handler method failed.
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 * @see HandlerMethodInvoker#invokeHandlerMethod
 */
public class HandlerMethodInvocationException extends NestedRuntimeException {

	/**
	 * Create a new HandlerMethodInvocationException for the given Method handle and cause.
	 * @param handlerMethod the handler method handle
	 * @param cause the cause of the invocation failure
	 */
	public HandlerMethodInvocationException(Method handlerMethod, Throwable cause) {
		super("Failed to invoke handler method [" + handlerMethod + "]", cause);
	}

}
