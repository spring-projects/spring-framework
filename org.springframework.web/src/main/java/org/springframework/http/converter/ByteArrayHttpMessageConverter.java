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

package org.springframework.http.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can read and write byte arrays.
 *
 * <p>By default, this converter supports all media types (<code>&#42;&#47;&#42;</code>), and writes with a {@code
 * Content-Type} of {@code application/octet-stream}. This can be overridden by setting the {@link
 * #setSupportedMediaTypes(java.util.List) supportedMediaTypes} property.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<byte[]> {

	/** Creates a new instance of the {@code ByteArrayHttpMessageConverter}. */
	public ByteArrayHttpMessageConverter() {
		super(new MediaType("application", "octet-stream"), MediaType.ALL);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return byte[].class.equals(clazz);
	}

	@Override
	public byte[] readInternal(Class clazz, HttpInputMessage inputMessage) throws IOException {
		long contentLength = inputMessage.getHeaders().getContentLength();
		if (contentLength >= 0) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream((int) contentLength);
			FileCopyUtils.copy(inputMessage.getBody(), bos);
			return bos.toByteArray();
		}
		else {
			return FileCopyUtils.copyToByteArray(inputMessage.getBody());
		}
	}

	@Override
	protected Long getContentLength(byte[] bytes, MediaType contentType) {
		return (long) bytes.length;
	}

	@Override
	protected void writeInternal(byte[] bytes, HttpOutputMessage outputMessage) throws IOException {
		FileCopyUtils.copy(bytes, outputMessage.getBody());
	}

}
