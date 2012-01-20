/*
 * Copyright 2002-2011 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * Abstract base class for most {@link HttpMessageConverter} implementations.
 *
 * <p>This base class adds support for setting supported {@code MediaTypes}, through the
 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} bean property. It also adds
 * support for {@code Content-Type} and {@code Content-Length} when writing to output messages.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private List<MediaType> supportedMediaTypes = Collections.emptyList();


	/**
	 * Construct an {@code AbstractHttpMessageConverter} with no supported media types.
	 * @see #setSupportedMediaTypes
	 */
	protected AbstractHttpMessageConverter() {
	}

	/**
	 * Construct an {@code AbstractHttpMessageConverter} with one supported media type.
	 * @param supportedMediaType the supported media type
	 */
	protected AbstractHttpMessageConverter(MediaType supportedMediaType) {
		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
	}

	/**
	 * Construct an {@code AbstractHttpMessageConverter} with multiple supported media type.
	 * @param supportedMediaTypes the supported media types
	 */
	protected AbstractHttpMessageConverter(MediaType... supportedMediaTypes) {
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}


	/**
	 * Set the list of {@link MediaType} objects supported by this converter.
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "'supportedMediaTypes' must not be empty");
		this.supportedMediaTypes = new ArrayList<MediaType>(supportedMediaTypes);
	}

	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.supportedMediaTypes);
	}


	/**
	 * This implementation checks if the given class is {@linkplain #supports(Class) supported},
	 * and if the {@linkplain #getSupportedMediaTypes() supported media types}
	 * {@linkplain MediaType#includes(MediaType) include} the given media type.
	 */
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return supports(clazz) && canRead(mediaType);
	}

	/**
	 * Returns true if any of the {@linkplain #setSupportedMediaTypes(List) supported media types}
	 * include the given media type.
	 * @param mediaType the media type to read, can be {@code null} if not specified.
	 * Typically the value of a {@code Content-Type} header.
	 * @return {@code true} if the supported media types include the media type,
	 * or if the media type is {@code null}
	 */
	protected boolean canRead(MediaType mediaType) {
		if (mediaType == null) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.includes(mediaType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This implementation checks if the given class is {@linkplain #supports(Class) supported},
	 * and if the {@linkplain #getSupportedMediaTypes() supported media types}
	 * {@linkplain MediaType#includes(MediaType) include} the given media type.
	 */
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return supports(clazz) && canWrite(mediaType);
		}

	/**
	 * Returns {@code true} if the given media type includes any of the
	 * {@linkplain #setSupportedMediaTypes(List) supported media types}.
	 * @param mediaType the media type to write, can be {@code null} if not specified.
	 * Typically the value of an {@code Accept} header.
	 * @return {@code true} if the supported media types are compatible with the media type,
	 * or if the media type is {@code null}
	 */
	protected boolean canWrite(MediaType mediaType) {
		if (mediaType == null || MediaType.ALL.equals(mediaType)) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.isCompatibleWith(mediaType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This implementation simple delegates to {@link #readInternal(Class, HttpInputMessage)}.
	 * Future implementations might add some default behavior, however.
	 */
	public final T read(Class<? extends T> clazz, HttpInputMessage inputMessage) throws IOException {
		return readInternal(clazz, inputMessage);
	}

	/**
	 * This implementation delegates to {@link #getDefaultContentType(Object)} if a content
	 * type was not provided, calls {@link #getContentLength}, and sets the corresponding headers
	 * on the output message. It then calls {@link #writeInternal}.
	 */
	public final void write(T t, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		HttpHeaders headers = outputMessage.getHeaders();
		if (headers.getContentType() == null) {
			if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
				contentType = getDefaultContentType(t);
			}
			if (contentType != null) {
				headers.setContentType(contentType);
			}
		}
		if (headers.getContentLength() == -1) {
			Long contentLength = getContentLength(t, headers.getContentType());
			if (contentLength != null) {
				headers.setContentLength(contentLength);
			}
		}
		writeInternal(t, outputMessage);
		outputMessage.getBody().flush();
	}

	/**
	 * Returns the default content type for the given type. Called when {@link #write}
	 * is invoked without a specified content type parameter.
	 * <p>By default, this returns the first element of the
	 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} property, if any.
	 * Can be overridden in subclasses.
	 * @param t the type to return the content type for
	 * @return the content type, or <code>null</code> if not known
	 */
	protected MediaType getDefaultContentType(T t) throws IOException {
		List<MediaType> mediaTypes = getSupportedMediaTypes();
		return (!mediaTypes.isEmpty() ? mediaTypes.get(0) : null);
	}

	/**
	 * Returns the content length for the given type.
	 * <p>By default, this returns {@code null}, meaning that the content length is unknown.
	 * Can be overridden in subclasses.
	 * @param t the type to return the content length for
	 * @return the content length, or {@code null} if not known
	 */
	protected Long getContentLength(T t, MediaType contentType) throws IOException {
		return null;
	}


	/**
	 * Indicates whether the given class is supported by this converter.
	 * @param clazz the class to test for support
	 * @return <code>true</code> if supported; <code>false</code> otherwise
	 */
	protected abstract boolean supports(Class<?> clazz);

	/**
	 * Abstract template method that reads the actualy object. Invoked from {@link #read}.
	 * @param clazz the type of object to return
	 * @param inputMessage the HTTP input message to read from
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotReadableException in case of conversion errors
	 */
	protected abstract T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * Abstract template method that writes the actual body. Invoked from {@link #write}.
	 * @param t the object to write to the output message
	 * @param outputMessage the message to write to
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 */
	protected abstract void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
