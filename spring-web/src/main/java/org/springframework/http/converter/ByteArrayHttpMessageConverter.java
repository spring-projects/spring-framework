/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

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
		outputMessage.getBody().write(bytes);
	}

	@Override
	public boolean canWriteRepeatedly(byte[] bytes, @Nullable MediaType contentType) {
		return true;
	}

	@Override
	@SuppressWarnings("removal")
	protected boolean supportsRepeatableWrites(byte[] bytes) {
		return true;
	}

}
