/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Strategy interface to process the value returned from a handler method invocation. 
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface HandlerMethodReturnValueHandler extends HandlerMethodProcessor {

	/**
	 * Indicates whether the given {@linkplain MethodParameter method return type} is supported by this handler.
	 *
	 * @param returnType the method return type to check
	 * @return {@code true} if this handler supports the supplied return type; {@code false} otherwise
	 */
	boolean supportsReturnType(MethodParameter returnType);

	/**
	 * Handles the given value returned by a handler method invocation by writing directly to the response
	 * or by using the {@code mavContainer} argument to add model attributes and/or set the view. 
	 * 
	 * @param returnValue the return value to handle
	 * @param returnType the return type to handle. This type must have previously been passed to 
	 * {@link #supportsReturnType(org.springframework.core.MethodParameter)} and it must have returned {@code true}
	 * @param mavContainer records model and view choices
	 * @param webRequest the current request
	 * @throws Exception in case of errors
	 */
	void handleReturnValue(Object returnValue,
						   MethodParameter returnType,
						   ModelAndViewContainer mavContainer,
						   NativeWebRequest webRequest) throws Exception;

}