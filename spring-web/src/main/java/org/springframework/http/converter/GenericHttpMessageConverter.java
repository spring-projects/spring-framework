/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;

/**
 * A specialization of {@link HttpMessageConverter} that can convert an HTTP
 * request into a target object of a specified generic type.
 *
 * @author Arjen Poutsma
 * @since 3.2
 *
 * @see org.springframework.core.ParameterizedTypeReference
 */
public interface GenericHttpMessageConverter<T> extends HttpMessageConverter<T> {

	/**
	 * Indicates whether the given type can be read by this converter.
	 * @param type the type to test for readability
	 * @param mediaType the media type to read, can be {@code null} if not specified.
	 * Typically the value of a {@code Content-Type} header.
	 * @return {@code true} if readable; {@code false} otherwise
	 */
	boolean canRead(Type type, MediaType mediaType);

	/**
	 * Read an object of the given type form the given input message, and returns it.
	 * @param type the type of object to return. This type must have previously
	 * been passed to the {@link #canRead canRead} method of this interface,
	 * which must have returned {@code true}.
	 * @param type the type of the target object
	 * @param inputMessage the HTTP input message to read from
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotReadableException in case of conversion errors
	 */
	T read(Type type, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

}
