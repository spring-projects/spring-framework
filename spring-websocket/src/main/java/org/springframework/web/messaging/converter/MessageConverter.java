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

package org.springframework.web.messaging.converter;

import java.io.IOException;

import org.springframework.http.MediaType;

/**
 * Strategy for converting a byte array message payload to and from a typed object.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface MessageConverter {


	/**
	 * Whether instances of the given class can be converted to from a byte array.
	 *
	 * @param clazz the class to convert from
	 * @param mediaType the media type of the content, can be {@code null} if not
	 *        specified. Typically the value of a {@code Content-Type} header.
	 * @return {@code true} if it can be converted; {@code false} otherwise
	 */
	boolean canConvertFromPayload(Class<?> clazz, MediaType mediaType);

	/**
	 * Convert the byte array payload to the given type.
	 *
	 * @param clazz the type of object to return. This type must have previously been
	 *        passed to {@link #canConvertFromPayload(Class, MediaType)} and it must have
	 *        returned {@code true}.
	 * @param contentType the content type of the payload, can be {@code null}
	 * @param payload the payload to convert from
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 */
	Object convertFromPayload(Class<?> clazz, MediaType contentType, byte[] payload)
			throws IOException, ContentTypeNotSupportedException;

	/**
	 * Whether instances of the given class can be converted to a byte array.
	 *
	 * @param clazz the class to test
	 * @param mediaType the media type of the content, can be {@code null} if not specified.
	 * @return {@code true} if writable; {@code false} otherwise
	 */
	boolean canConvertToPayload(Class<?> clazz, MediaType mediaType);

	/**
	 * Convert the given object to a byte array.
	 *
	 * @param t the object to convert. The type of this object must have previously been
	 *        passed to {@link #canConvertToPayload(Class, MediaType)} and it must have returned
	 *        {@code true}.
	 * @param headers
	 * @return the output message
	 * @throws IOException in case of I/O errors
	 */
	byte[] convertToPayload(Object content, MediaType contentType) throws IOException, ContentTypeNotSupportedException;

}
