/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.converter;

import java.io.IOException;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

/**
 * A specialization of {@link HttpMessageConverter} that can convert an HTTP request
 * into a target object of a specified {@link ResolvableType} and a source object of
 * a specified {@link ResolvableType} into an HTTP response with optional hints.
 *
 * <p>It provides default methods for {@link HttpMessageConverter} in order to allow
 * subclasses to only have to implement the smart APIs.
 *
 * @author Sebastien Deleuze
 * @since 6.2
 * @param <T> the converted object type
 */
public interface SmartHttpMessageConverter<T> extends HttpMessageConverter<T> {

	/**
	 * Indicates whether the given type can be read by this converter.
	 * This method should perform the same checks as
	 * {@link HttpMessageConverter#canRead(Class, MediaType)} with additional ones
	 * related to the generic type.
	 * @param type the (potentially generic) type to test for readability. The
	 * {@linkplain ResolvableType#getSource() type source} may be used for retrieving
	 * additional information (the related method signature for example) when relevant.
	 * @param mediaType the media type to read, can be {@code null} if not specified.
	 * Typically, the value of a {@code Content-Type} header.
	 * @return {@code true} if readable; {@code false} otherwise
	 */
	boolean canRead(ResolvableType type, @Nullable MediaType mediaType);

	@Override
	default boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return canRead(ResolvableType.forClass(clazz), mediaType);
	}

	/**
	 * Read an object of the given type from the given input message, and returns it.
	 * @param type the (potentially generic) type of object to return. This type must have
	 * previously been passed to the {@link #canRead(ResolvableType, MediaType) canRead}
	 * method of this interface, which must have returned {@code true}. The
	 * {@linkplain ResolvableType#getSource() type source} may be used for retrieving
	 * additional information (the related method signature for example) when relevant.
	 * @param inputMessage the HTTP input message to read from
	 * @param hints additional information about how to encode
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotReadableException in case of conversion errors
	 */
	T read(ResolvableType type, HttpInputMessage inputMessage, @Nullable Map<String, Object> hints)
			throws IOException, HttpMessageNotReadableException;

	@Override
	default T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return read(ResolvableType.forClass(clazz), inputMessage, null);
	}

	/**
	 * Indicates whether the given class can be written by this converter.
	 * <p>This method should perform the same checks as
	 * {@link HttpMessageConverter#canWrite(Class, MediaType)} with additional ones
	 * related to the generic type.
	 * @param targetType the (potentially generic) target type to test for writability
	 * (can be {@link ResolvableType#NONE} if not specified). The {@linkplain ResolvableType#getSource() type source}
	 * may be used for retrieving additional information (the related method signature for example) when relevant.
	 * @param valueClass the source object class to test for writability
	 * @param mediaType the media type to write (can be {@code null} if not specified);
	 * typically the value of an {@code Accept} header.
	 * @return {@code true} if writable; {@code false} otherwise
	 */
	boolean canWrite(ResolvableType targetType, Class<?> valueClass, @Nullable MediaType mediaType);

	@Override
	default boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return canWrite(ResolvableType.forClass(clazz), clazz, mediaType);
	}

	/**
	 * Write a given object to the given output message.
	 * @param t the object to write to the output message. The type of this object must
	 * have previously been passed to the {@link #canWrite canWrite} method of this
	 * interface, which must have returned {@code true}.
	 * @param type the (potentially generic) type of object to write. This type must have
	 * previously been passed to the {@link #canWrite canWrite} method of this interface,
	 * which must have returned {@code true}. Can be {@link ResolvableType#NONE} if not specified.
	 * The {@linkplain ResolvableType#getSource() type source} may be used for retrieving additional
	 * information (the related method signature for example) when relevant.
	 * @param contentType the content type to use when writing. May be {@code null} to
	 * indicate that the default content type of the converter must be used. If not
	 * {@code null}, this media type must have previously been passed to the
	 * {@link #canWrite canWrite} method of this interface, which must have returned
	 * {@code true}.
	 * @param outputMessage the message to write to
	 * @param hints additional information about how to encode
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 */
	void write(T t, ResolvableType type, @Nullable MediaType contentType, HttpOutputMessage outputMessage,
			@Nullable Map<String, Object> hints) throws IOException, HttpMessageNotWritableException;

	@Override
	default void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		write(t, ResolvableType.forInstance(t), contentType, outputMessage, null);
	}
}
