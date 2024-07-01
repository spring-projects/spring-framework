/*
 * Copyright 2002-2023 the original author or authors.
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
import java.io.OutputStream;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;

/**
 * Abstract base class for most {@link SmartHttpMessageConverter} implementations.
 *
 * @author Sebastien Deleuze
 * @since 6.2
 * @param <T> the converted object type
 */
public abstract class AbstractSmartHttpMessageConverter<T> extends AbstractHttpMessageConverter<T>
		implements SmartHttpMessageConverter<T> {

	/**
	 * Construct an {@code AbstractSmartHttpMessageConverter} with no supported media types.
	 * @see #setSupportedMediaTypes
	 */
	protected AbstractSmartHttpMessageConverter() {
	}

	/**
	 * Construct an {@code AbstractSmartHttpMessageConverter} with one supported media type.
	 * @param supportedMediaType the supported media type
	 */
	protected AbstractSmartHttpMessageConverter(MediaType supportedMediaType) {
		super(supportedMediaType);
	}

	/**
	 * Construct an {@code AbstractSmartHttpMessageConverter} with multiple supported media type.
	 * @param supportedMediaTypes the supported media types
	 */
	protected AbstractSmartHttpMessageConverter(MediaType... supportedMediaTypes) {
		super(supportedMediaTypes);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return true;
	}

	@Override
	public boolean canRead(ResolvableType type, @Nullable MediaType mediaType) {
		Class<?> clazz = type.resolve();
		return (clazz != null ? canRead(clazz, mediaType) : canRead(mediaType));
	}

	@Override
	public boolean canWrite(ResolvableType type, Class<?> clazz, @Nullable MediaType mediaType) {
		return canWrite(clazz, mediaType);
	}

	/**
	 * This implementation sets the default headers by calling {@link #addDefaultHeaders},
	 * and then calls {@link #writeInternal}.
	 */
	@Override
	public final void write(T t, ResolvableType type, @Nullable MediaType contentType,
			HttpOutputMessage outputMessage, @Nullable Map<String, Object> hints)
			throws IOException, HttpMessageNotWritableException {

		HttpHeaders headers = outputMessage.getHeaders();
		addDefaultHeaders(headers, t, contentType);

		if (outputMessage instanceof StreamingHttpOutputMessage streamingOutputMessage) {
			streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
				@Override
				public void writeTo(OutputStream outputStream) throws IOException {
					writeInternal(t, type, new HttpOutputMessage() {
						@Override
						public OutputStream getBody() {
							return outputStream;
						}

						@Override
						public HttpHeaders getHeaders() {
							return headers;
						}
					}, hints);
				}

				@Override
				public boolean repeatable() {
					return supportsRepeatableWrites(t);
				}
			});
		}
		else {
			writeInternal(t, type, outputMessage, hints);
			outputMessage.getBody().flush();
		}
	}

	@Override
	protected void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		writeInternal(t, ResolvableType.NONE, outputMessage, null);
	}

	/**
	 * Abstract template method that writes the actual body. Invoked from
	 * {@link #write(Object, ResolvableType, MediaType, HttpOutputMessage, Map)}.
	 * @param t the object to write to the output message
	 * @param type the type of object to write
	 * @param outputMessage the HTTP output message to write to
	 * @param hints additional information about how to encode
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 */
	protected abstract void writeInternal(T t, ResolvableType type, HttpOutputMessage outputMessage,
			@Nullable Map<String, Object> hints) throws IOException, HttpMessageNotWritableException;

	@Override
	protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return read(ResolvableType.forClass(clazz), inputMessage, null);
	}
}
