/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
