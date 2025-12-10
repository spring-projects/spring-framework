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
import java.util.function.Predicate;

import kotlinx.serialization.BinaryFormat;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializationException;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

/**
 * Abstract base class for {@link HttpMessageConverter} implementations that
 * defer to Kotlin {@linkplain BinaryFormat binary serializers}.
 *
 * <p>As of Spring Framework 7.0, by default it only encodes types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics level
 * since it allows combined usage with other general purpose converters without conflicts.
 * Alternative constructors with a {@code Predicate<ResolvableType>} parameter can be used
 * to customize this behavior.
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Iain Henderson
 * @author Arjen Poutsma
 * @since 6.0
 * @param <T> the type of {@link BinaryFormat}
 */
public abstract class KotlinSerializationBinaryHttpMessageConverter<T extends BinaryFormat>
		extends AbstractKotlinSerializationHttpMessageConverter<T> {

	/**
	 * Creates a new instance with the given format and supported mime types
	 * which only converters types annotated with
	 * {@link kotlinx.serialization.Serializable @Serializable} at type or
	 * generics level.
	 */
	protected KotlinSerializationBinaryHttpMessageConverter(T format, MediaType... supportedMediaTypes) {
		super(format, supportedMediaTypes);
	}

	/**
	 * Creates a new instance with the given format and supported mime types
	 * which only converts types for which the specified predicate returns
	 * {@code true}.
	 * @since 7.0
	 */
	protected KotlinSerializationBinaryHttpMessageConverter(T format, Predicate<ResolvableType> typePredicate, MediaType... supportedMediaTypes) {
		super(format, typePredicate, supportedMediaTypes);
	}

	@Override
	protected Object readInternal(KSerializer<Object> serializer, T format, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		byte[] bytes = StreamUtils.copyToByteArray(inputMessage.getBody());
		try {
			return format.decodeFromByteArray(serializer, bytes);
		}
		catch (SerializationException ex) {
			throw new HttpMessageNotReadableException("Could not read " + format + ": " + ex.getMessage(), ex,
					inputMessage);
		}
	}

	@Override
	protected void writeInternal(Object object, KSerializer<Object> serializer, T format,
			HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

		try {
			byte[] bytes = format.encodeToByteArray(serializer, object);
			outputMessage.getBody().write(bytes);
			outputMessage.getBody().flush();
		}
		catch (SerializationException ex) {
			throw new HttpMessageNotWritableException("Could not write " + format + ": " + ex.getMessage(), ex);
		}
	}
}
