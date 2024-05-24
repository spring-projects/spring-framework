/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * {@code @ExceptionHandler} mapping information. It contains:
 * <ul>
 *     <li>the supported exception types
 *     <li>the producible media types, if any
 *     <li>the method in charge of handling the exception
 * </ul>
 * @author Brian Clozel
 * @since 6.2
 */
public class ExceptionHandlerMappingInfo {

	private final Set<Class<? extends Throwable>> exceptionTypes;

	private final Set<MediaType> producibleTypes;

	private final Method handlerMethod;


	ExceptionHandlerMappingInfo(Set<Class<? extends Throwable>> exceptionTypes, Set<MediaType> producibleMediaTypes, Method handlerMethod) {
		Assert.notNull(exceptionTypes, "exceptionTypes should not be null");
		Assert.notNull(producibleMediaTypes, "producibleMediaTypes should not be null");
		Assert.notNull(handlerMethod, "handlerMethod should not be null");
		this.exceptionTypes = exceptionTypes;
		this.producibleTypes = producibleMediaTypes;
		this.handlerMethod = handlerMethod;
	}


	/**
	 * Return the method responsible for handling the exception.
	 */
	public Method getHandlerMethod() {
		return this.handlerMethod;
	}

	/**
	 * Return the exception types supported by this handler.
	 */
	public Set<Class<? extends Throwable>> getExceptionTypes() {
		return this.exceptionTypes;
	}

	/**
	 * Return the producible media types by this handler. Can be empty.
	 */
	public Set<MediaType> getProducibleTypes() {
		return this.producibleTypes;
	}

}
