/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.messaging.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * Abstract base class for most {@link MessageConverter} implementations.
 *
 * <p>This base class adds support for setting supported {@code MediaTypes}, through the
 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} property.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 4.0
 */
public abstract class AbstractMessageConverter implements MessageConverter {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private List<MediaType> supportedMediaTypes = Collections.emptyList();


	/**
	 * Construct an {@code AbstractMessageConverter} with no supported media types.
	 * @see #setSupportedMediaTypes
	 */
	protected AbstractMessageConverter() {
	}

	/**
	 * Construct an {@code AbstractMessageConverter} with one supported media type.
	 *
	 * @param supportedMediaType the supported media type
	 */
	protected AbstractMessageConverter(MediaType supportedMediaType) {
		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
	}

	/**
	 * Construct an {@code AbstractMessageConverter} with multiple supported media type.
	 *
	 * @param supportedMediaTypes the supported media types
	 */
	protected AbstractMessageConverter(MediaType... supportedMediaTypes) {
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
	 * This implementation checks if the given class is {@linkplain #supports(Class)
	 * supported}, and if the {@linkplain #getSupportedMediaTypes() supported media types}
	 * {@linkplain MediaType#includes(MediaType) include} the given media type.
	 */
	@Override
	public boolean canConvertFromPayload(Class<?> clazz, MediaType mediaType) {
		return supports(clazz) && canConvertFrom(mediaType);
	}

	/**
	 * Indicates whether the given class is supported by this converter.
	 * @param clazz the class to test for support
	 * @return {@code true} if supported; {@code false} otherwise
	 */
	protected abstract boolean supports(Class<?> clazz);

	/**
	 * Returns true if any of the {@linkplain #setSupportedMediaTypes(List) supported
	 * media types} include the given media type.
	 *
	 * @param mediaType the media type to read, can be {@code null} if not specified.
	 *        Typically the value of a {@code Content-Type} header.
	 * @return {@code true} if the supported media types include the media type, or if the
	 *         media type is {@code null}
	 */
	protected boolean canConvertFrom(MediaType mediaType) {
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
	 * This implementation checks if the given class is {@linkplain #supports(Class)
	 * supported}, and if the {@linkplain #getSupportedMediaTypes() supported media types}
	 * {@linkplain MediaType#includes(MediaType) include} the given media type.
	 */
	@Override
	public boolean canConvertToPayload(Class<?> clazz, MediaType mediaType) {
		return supports(clazz) && canConvertTo(mediaType);
		}

	/**
	 * Returns {@code true} if the given media type includes any of the
	 * {@linkplain #setSupportedMediaTypes(List) supported media types}.
	 *
	 * @param mediaType the media type to write, can be {@code null} if not specified.
	 *        Typically the value of an {@code Accept} header.
	 * @return {@code true} if the supported media types are compatible with the media
	 *         type, or if the media type is {@code null}
	 */
	protected boolean canConvertTo(MediaType mediaType) {
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
	 * This implementation simply delegates to
	 * {@link #convertFromPayloadInternal(Class, MediaType, byte[])}. Future
	 * implementations might add some default behavior, however.
	 */
	@Override
	public Object convertFromPayload(Class<?> clazz, MediaType contentType, byte[] payload)
			throws IOException, ContentTypeNotSupportedException {

		return convertFromPayloadInternal(clazz, contentType, payload);
	}

	/**
	 * Abstract template method that reads the actual object. Invoked from {@link #read}.
	 * @param clazz the type of object to return
	 * @param contentType
	 * @param payload the content to convert from
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 */
	protected abstract Object convertFromPayloadInternal(Class<?> clazz, MediaType contentType, byte[] payload)
			throws IOException, ContentTypeNotSupportedException;

	/**
	 * This implementation simply delegates to
	 * {@link #convertToPayloadInternal(Object, MediaType)}. Future
	 * implementations might add some default behavior, however.
	 */
	@Override
	public byte[] convertToPayload(Object content, MediaType contentType)
			throws IOException, ContentTypeNotSupportedException {

		return convertToPayloadInternal(content, contentType);
	}

	protected abstract byte[] convertToPayloadInternal(Object content, MediaType contentType)
			throws IOException, ContentTypeNotSupportedException;

}
