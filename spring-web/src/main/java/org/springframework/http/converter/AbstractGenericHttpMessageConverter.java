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
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;

/**
 * Abstract base class for most {@link GenericHttpMessageConverter} implementations.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.2
 * @param <T> the converted object type
 */
public abstract class AbstractGenericHttpMessageConverter<T> extends AbstractHttpMessageConverter<T>
		implements GenericHttpMessageConverter<T> {

	/**
	 * Construct an {@code AbstractGenericHttpMessageConverter} with no supported media types.
	 * @see #setSupportedMediaTypes
	 */
	protected AbstractGenericHttpMessageConverter() {
	}

	/**
	 * Construct an {@code AbstractGenericHttpMessageConverter} with one supported media type.
	 * @param supportedMediaType the supported media type
	 */
	protected AbstractGenericHttpMessageConverter(MediaType supportedMediaType) {
		super(supportedMediaType);
	}

	/**
	 * Construct an {@code AbstractGenericHttpMessageConverter} with multiple supported media type.
	 * @param supportedMediaTypes the supported media types
	 */
	protected AbstractGenericHttpMessageConverter(MediaType... supportedMediaTypes) {
		super(supportedMediaTypes);
	}

	/**
	 * Construct an {@code AbstractGenericHttpMessageConverter} with a default charset and
	 * multiple supported media types.
	 * @param defaultCharset the default character set
	 * @param supportedMediaTypes the supported media types
	 * @since 6.2
	 */
	protected AbstractGenericHttpMessageConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
		super(defaultCharset, supportedMediaTypes);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return true;
	}

	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		return (type instanceof Class<?> clazz ? canRead(clazz, mediaType) : canRead(mediaType));
	}

	@Override
	public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
		return canWrite(clazz, mediaType);
	}

	/**
	 * This implementation sets the default headers by calling {@link #addDefaultHeaders},
	 * and then calls {@link #writeInternal}.
	 */
	@Override
	public final void write(final T t, final @Nullable Type type, @Nullable MediaType contentType,
			HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

		final HttpHeaders headers = outputMessage.getHeaders();
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
					});
				}

				@Override
				public boolean repeatable() {
					return supportsRepeatableWrites(t);
				}
			});
		}
		else {
			writeInternal(t, type, outputMessage);
			outputMessage.getBody().flush();
		}
	}

	@Override
	protected void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		writeInternal(t, null, outputMessage);
	}

	/**
	 * Abstract template method that writes the actual body. Invoked from {@link #write}.
	 * @param t the object to write to the output message
	 * @param type the type of object to write (may be {@code null})
	 * @param outputMessage the HTTP output message to write to
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 */
	protected abstract void writeInternal(T t, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
