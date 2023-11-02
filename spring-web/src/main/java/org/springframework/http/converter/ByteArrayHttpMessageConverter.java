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

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can read and write byte arrays.
 *
 * <p>By default, this converter supports all media types (<code>&#42;/&#42;</code>), and
 * writes with a {@code Content-Type} of {@code application/octet-stream}. This can be
 * overridden by setting the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<byte[]> {

	/**
	 * Create a new instance of the {@code ByteArrayHttpMessageConverter}.
	 */
	public ByteArrayHttpMessageConverter() {
		super(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return byte[].class == clazz;
	}

	@Override
	public byte[] readInternal(Class<? extends byte[]> clazz, HttpInputMessage message) throws IOException {
		long length = message.getHeaders().getContentLength();
		return (length >= 0 && length < Integer.MAX_VALUE ?
				message.getBody().readNBytes((int) length) : message.getBody().readAllBytes());
	}

	@Override
	protected Long getContentLength(byte[] bytes, @Nullable MediaType contentType) {
		return (long) bytes.length;
	}

	@Override
	protected void writeInternal(byte[] bytes, HttpOutputMessage outputMessage) throws IOException {
		StreamUtils.copy(bytes, outputMessage.getBody());
	}

	@Override
	protected boolean supportsRepeatableWrites(byte[] bytes) {
		return true;
	}
}
