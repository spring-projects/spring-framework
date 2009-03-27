/*
 * Copyright 2002-2009 the original author or authors.
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
 * <p>This base class adds support for setting supported {@code MediaTypes}, through the {@link
 * #setSupportedMediaTypes(List) supportedMediaTypes} bean property. It also adds support for {@code Content-Type} and
 * {@code Content-Length} when writing to output messages.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private List<MediaType> supportedMediaTypes = Collections.emptyList();

	/**
	 * Construct an {@code AbstractHttpMessageConverter} with no supported media types.
	 *
	 * @see #setSupportedMediaTypes
	 */
	protected AbstractHttpMessageConverter() {
	}

	/** Construct an {@code AbstractHttpMessageConverter} with one supported media type. */
	protected AbstractHttpMessageConverter(MediaType supportedMediaType) {
		this.supportedMediaTypes = Collections.singletonList(supportedMediaType);
	}

	/** Construct an {@code AbstractHttpMessageConverter} with multiple supported media type. */
	protected AbstractHttpMessageConverter(MediaType... supportedMediaTypes) {
		this.supportedMediaTypes = Arrays.asList(supportedMediaTypes);
	}

	/** Set the list of {@link MediaType} objects supported by this converter. */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "'supportedMediaTypes' must not be empty");
		this.supportedMediaTypes = new ArrayList<MediaType>(supportedMediaTypes);
	}

	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.supportedMediaTypes);
	}

	/**
	 * {@inheritDoc} <p>This implementation simple delegates to {@link #readInternal(Class, HttpInputMessage)}. Future
	 * implementations might add some default behavior, however.
	 */
	public final T read(Class<T> clazz, HttpInputMessage inputMessage) throws IOException {
		return readInternal(clazz, inputMessage);
	}

	/**
	 * Abstract template method that reads the actualy object. Invoked from {@link #read(Class, HttpInputMessage)}.
	 *
	 * @param clazz the type of object to return
	 * @param inputMessage the HTTP input message to read from
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotReadableException in case of conversion errors
	 */
	protected abstract T readInternal(Class<T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * {@inheritDoc} <p>This implementation delegates to {@link #getContentType(Object)} and {@link
	 * #getContentLength(Object)}, and sets the corresponding headers on the output message. It then calls {@link
	 * #writeInternal(Object, HttpOutputMessage)}.
	 */
	public final void write(T t, HttpOutputMessage outputMessage) throws IOException {
		HttpHeaders headers = outputMessage.getHeaders();
		MediaType contentType = getContentType(t);
		if (contentType != null) {
			headers.setContentType(contentType);
		}
		Long contentLength = getContentLength(t);
		if (contentLength != null) {
			headers.setContentLength(contentLength);
		}
		writeInternal(t, outputMessage);
		outputMessage.getBody().flush();
	}

	/**
	 * Returns the content type for the given type. <p>By default, this returns the first element of the {@link
	 * #setSupportedMediaTypes(List) supportedMediaTypes} property, if any. Can be overriden in subclasses.
	 *
	 * @param t the type to return the content type for
	 * @return the content type, or <code>null</code> if not known
	 */
	protected MediaType getContentType(T t) {
		List<MediaType> mediaTypes = getSupportedMediaTypes();
		return (!mediaTypes.isEmpty() ? mediaTypes.get(0) : null);
	}

	/**
	 * Returns the content length for the given type. <p>By default, this returns <code>null</code>. Can be overriden in
	 * subclasses.
	 *
	 * @param t the type to return the content length for
	 * @return the content length, or <code>null</code> if not known
	 */
	protected Long getContentLength(T t) {
		return null;
	}

	/**
	 * Abstract template method that writes the actualy body. Invoked from {@link #write(Object, HttpOutputMessage)}.
	 *
	 * @param t the object to write to the output message
	 * @param outputMessage the message to write to
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 */
	protected abstract void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
