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

/**
 * A base interface for {@link HandlerMethodArgumentResolver}s and {@link HandlerMethodReturnValueHandler}s.
 * 
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface HandlerMethodProcessor {

	/**
	 * Indicates whether the given {@linkplain org.springframework.core.MethodParameter method parameter},
	 * uses the response argument with the implication that the response will be handled directly by invoking 
	 * the method and will not require view name resolution followed by rendering.
	 * 
	 * @param parameter the method parameter, either a method argument or a return type
	 * @return {@code true} if the supplied parameter uses the response argument; {@code false} otherwise
	 */
	boolean usesResponseArgument(MethodParameter parameter);
	
}