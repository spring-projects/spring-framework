/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can read/write {@link Resource Resources}
 * and supports byte range requests.
 *
 * <p>By default, this converter can read all media types. The {@link MediaTypeFactory} is used
 * to determine the {@code Content-Type} of written resources.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Kazuki Shimizu
 * @since 3.0.2
 */
public class ResourceHttpMessageConverter extends AbstractHttpMessageConverter<Resource> {

	private final boolean supportsReadStreaming;


	/**
	 * Create a new instance of the {@code ResourceHttpMessageConverter}
	 * that supports read streaming, i.e. can convert an
	 * {@code HttpInputMessage} to {@code InputStreamResource}.
	 */
	public ResourceHttpMessageConverter() {
		super(MediaType.ALL);
		this.supportsReadStreaming = true;
	}

	/**
	 * Create a new instance of the {@code ResourceHttpMessageConverter}.
	 * @param supportsReadStreaming whether the converter should support
	 * read streaming, i.e. convert to {@code InputStreamResource}
	 * @since 5.0
	 */
	public ResourceHttpMessageConverter(boolean supportsReadStreaming) {
		super(MediaType.ALL);
		this.supportsReadStreaming = supportsReadStreaming;
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return Resource.class.isAssignableFrom(clazz);
	}

	@Override
	protected Resource readInternal(Class<? extends Resource> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		if (this.supportsReadStreaming && InputStreamResource.class == clazz) {
			return new InputStreamResource(inputMessage.getBody()) {
				@Override
				public @Nullable String getFilename() {
					return inputMessage.getHeaders().getContentDisposition().getFilename();
				}
				@Override
				public long contentLength() throws IOException {
					long length = inputMessage.getHeaders().getContentLength();
					return (length != -1 ? length : super.contentLength());
				}
			};
		}
		else if (Resource.class == clazz || ByteArrayResource.class.isAssignableFrom(clazz)) {
			byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
			return new ByteArrayResource(body) {
				@Override
				public @Nullable String getFilename() {
					return inputMessage.getHeaders().getContentDisposition().getFilename();
				}
			};
		}
		else {
			throw new HttpMessageNotReadableException("Unsupported resource class: " + clazz, inputMessage);
		}
	}

	@Override
	protected void writeInternal(Resource resource, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		writeContent(resource, outputMessage);
	}

	/**
	 * Add the default headers for the given resource to the given message.
	 * @since 6.0
	 */
	public void addDefaultHeaders(HttpOutputMessage message, Resource resource, @Nullable MediaType contentType)
			throws IOException {

		addDefaultHeaders(message.getHeaders(), resource, contentType);
	}

	@Override
	protected MediaType getDefaultContentType(Resource resource) {
		return MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Override
	protected @Nullable Long getContentLength(Resource resource, @Nullable MediaType contentType) throws IOException {
		// Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
		// Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
		if (InputStreamResource.class == resource.getClass()) {
			return null;
		}
		long contentLength = resource.contentLength();
		return (contentLength < 0 ? null : contentLength);
	}

	@Override
	protected boolean supportsRepeatableWrites(Resource resource) {
		return !(resource instanceof InputStreamResource);
	}


	protected void writeContent(Resource resource, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		// We cannot use try-with-resources here for the InputStream, since we have
		// custom handling of the close() method in a finally-block.
		try {
			InputStream in = resource.getInputStream();
			try {
				OutputStream out = outputMessage.getBody();
				in.transferTo(out);
				out.flush();
			}
			catch (NullPointerException ignored) {
				// see SPR-13620
			}
			finally {
				try {
					in.close();
				}
				catch (Throwable ignored) {
					// see SPR-12999
				}
			}
		}
		catch (FileNotFoundException ignored) {
			// see SPR-12999
		}
	}

}
