/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.serializer.support;

import org.springframework.core.NestedRuntimeException;

/**
 * Wrapper for the native IOException (or similar) when a
 * {@link org.springframework.core.serializer.Serializer} or
 * {@link org.springframework.core.serializer.Deserializer} failed.
 * Thrown by {@link SerializingConverter} and {@link DeserializingConverter}.
 *
 * @author Gary Russell
 * @author Juergen Hoeller
 * @since 3.0.5
 */
@SuppressWarnings("serial")
public class SerializationFailedException extends NestedRuntimeException {

	/**
	 * Construct a {@code SerializationException} with the specified detail message.
	 * @param message the detail message
	 */
	public SerializationFailedException(String message) {
		super(message);
	}

	/**
	 * Construct a {@code SerializationException} with the specified detail message
	 * and nested exception.
	 * @param message the detail message
	 * @param cause the nested exception
	 */
	public SerializationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
