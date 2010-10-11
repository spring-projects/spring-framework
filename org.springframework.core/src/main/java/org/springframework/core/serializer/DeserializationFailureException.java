/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.serializer;

/**
 * Exception to be thrown when a failure occurs while deserializing an object.
 * 
 * @author Gary Russell
 * @since 3.0.5
 */
@SuppressWarnings("serial")
public class DeserializationFailureException extends SerializationException {

	/**
	 * Construct a <code>DeserializationFailureException</code> with the specified detail message.
	 * @param message the detail message
	 */
	public DeserializationFailureException(String message) {
		super(message);
	}

	/**
	 * Construct a <code>DeserializationFailureException</code> with the specified detail message
	 * and nested exception.
	 * @param message the detail message
	 * @param cause the nested exception
	 */
	public DeserializationFailureException(String message, Throwable cause) {
		super(message, cause);
	}

}
