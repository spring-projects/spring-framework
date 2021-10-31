/*
 * Copyright 2021-2021 the original author or authors.
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

import kotlinx.serialization.BinaryFormat;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializationException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write bytes using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This converter can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Iain Henderson
 * @since 5.3
 */
public class KotlinSerializationBinaryHttpMessageConverter extends AbstractKotlinSerializationHttpMessageConverter{

	private final BinaryFormat format;

	/**
	 * Construct an {@code KotlinSerializationBinaryHttpMessageConverter} with multiple supported media type.
	 * @param supportedMediaTypes the supported media types
	 */
	protected KotlinSerializationBinaryHttpMessageConverter(final BinaryFormat format, final MediaType... supportedMediaTypes) {
		super(supportedMediaTypes);
		this.format = format;
	}

	@Override
	protected Object decode(final KSerializer<Object> serializer, final HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		try {
			// TODO Use stream based API when available
			return this.format.decodeFromByteArray(serializer, StreamUtils.copyToByteArray(inputMessage.getBody()));
		}
		catch (SerializationException ex) {
			throw new HttpMessageNotReadableException("Could not read " + this.format + ": " + ex.getMessage(), ex, inputMessage);
		}
	}

	@Override
	protected void encode(final Object object, final KSerializer<Object> serializer, final HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		try {
			outputMessage.getBody().write(this.format.encodeToByteArray(serializer, object));
			outputMessage.getBody().flush();
		}
		catch (IOException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HttpMessageNotWritableException("Could not write " + this.format + ": " + ex.getMessage(), ex);
		}
	}
}
