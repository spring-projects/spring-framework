/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Abstract base class for {@link MessageConverter} implementations including support for
 * common properties and a partial implementation of the conversion methods mainly to
 * check if the converter supports the conversion based on the payload class and MIME
 * type.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractMessageConverter implements MessageConverter {

	protected final Log logger = LogFactory.getLog(getClass());


	private final List<MimeType> supportedMimeTypes;

	private ContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();

	private boolean strictContentTypeMatch = false;

	private Class<?> serializedPayloadClass = byte[].class;


	/**
	 * Construct an {@code AbstractMessageConverter} supporting a single MIME type.
	 * @param supportedMimeType the supported MIME type
	 */
	protected AbstractMessageConverter(MimeType supportedMimeType) {
		Assert.notNull(supportedMimeType, "supportedMimeType is required");
		this.supportedMimeTypes = Collections.<MimeType>singletonList(supportedMimeType);
	}

	/**
	 * Construct an {@code AbstractMessageConverter} supporting multiple MIME types.
	 * @param supportedMimeTypes the supported MIME types
	 */
	protected AbstractMessageConverter(Collection<MimeType> supportedMimeTypes) {
		Assert.notNull(supportedMimeTypes, "SupportedMimeTypes must not be null");
		this.supportedMimeTypes = new ArrayList<MimeType>(supportedMimeTypes);
	}


	/**
	 * Return the supported MIME types.
	 */
	public List<MimeType> getSupportedMimeTypes() {
		return Collections.unmodifiableList(this.supportedMimeTypes);
	}

	/**
	 * Configure the {@link ContentTypeResolver} to use to resolve the content
	 * type of an input message.
	 * <p>
	 * Note that if no resolver is configured, then
	 * {@link #setStrictContentTypeMatch(boolean) strictContentTypeMatch} should
	 * be left as {@code false} (the default) or otherwise this converter will
	 * ignore all messages.
	 * <p>
	 * By default, a {@code DefaultContentTypeResolver} instance is used.
	 */
	public void setContentTypeResolver(ContentTypeResolver resolver) {
		this.contentTypeResolver = resolver;
	}

	/**
	 * Return the configured {@link ContentTypeResolver}.
	 */
	public ContentTypeResolver getContentTypeResolver() {
		return this.contentTypeResolver;
	}

	/**
	 * Whether this converter should convert messages for which no content type
	 * could be resolved through the configured
	 * {@link org.springframework.messaging.converter.ContentTypeResolver}.
	 * A converter can configured to be strict only when a
	 * {@link #setContentTypeResolver(ContentTypeResolver) contentTypeResolver}
	 * is  configured and the list of {@link #getSupportedMimeTypes() supportedMimeTypes}
	 * is not be empty.
	 *
	 * then requires the content type of a message to be resolved
	 *
	 * When set to true, #supportsMimeType(MessageHeaders) will return false if the
	 * contentTypeResolver is not defined or if no content-type header is present.
	 */
	public void setStrictContentTypeMatch(boolean strictContentTypeMatch) {
		if (strictContentTypeMatch) {
			Assert.notEmpty(getSupportedMimeTypes(),
					"A strict converter requires a non-empty list of supported mime types");
			Assert.notNull(getContentTypeResolver(),
					"A strict converter requires a ContentTypeResolver");
		}
		this.strictContentTypeMatch = strictContentTypeMatch;
	}

	/**
	 * Whether content type resolution must produce a value that matches one of
	 * the supported MIME types.
	 */
	public boolean isStrictContentTypeMatch() {
		return this.strictContentTypeMatch;
	}

	/**
	 * Configure the preferred serialization class to use (byte[] or String) when
	 * converting an Object payload to a {@link Message}.
	 * <p>The default value is byte[].
	 * @param payloadClass either byte[] or String
	 */
	public void setSerializedPayloadClass(Class<?> payloadClass) {
		Assert.isTrue(byte[].class.equals(payloadClass) || String.class.equals(payloadClass),
				"Payload class must be byte[] or String: " + payloadClass);
		this.serializedPayloadClass = payloadClass;
	}

	/**
	 * Return the configured preferred serialization payload class.
	 */
	public Class<?> getSerializedPayloadClass() {
		return this.serializedPayloadClass;
	}


	/**
	 * Returns the default content type for the payload. Called when
	 * {@link #toMessage(Object, MessageHeaders)} is invoked without message headers or
	 * without a content type header.
	 * <p>By default, this returns the first element of the {@link #getSupportedMimeTypes()
	 * supportedMimeTypes}, if any. Can be overridden in sub-classes.
	 * @param payload the payload being converted to message
	 * @return the content type, or {@code null} if not known
	 */
	protected MimeType getDefaultContentType(Object payload) {
		List<MimeType> mimeTypes = getSupportedMimeTypes();
		return (!mimeTypes.isEmpty() ? mimeTypes.get(0) : null);
	}

	/**
	 * Whether the given class is supported by this converter.
	 * @param clazz the class to test for support
	 * @return {@code true} if supported; {@code false} otherwise
	 */
	protected abstract boolean supports(Class<?> clazz);


	@Override
	public final Object fromMessage(Message<?> message, Class<?> targetClass) {
		if (!canConvertFrom(message, targetClass)) {
			return null;
		}
		return convertFromInternal(message, targetClass);
	}

	protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
		return (supports(targetClass) && supportsMimeType(message.getHeaders()));
	}

	/**
	 * Convert the message payload from serialized form to an Object.
	 */
	public abstract Object convertFromInternal(Message<?> message, Class<?> targetClass);

	@Override
	public final Message<?> toMessage(Object payload, MessageHeaders headers) {
		if (!canConvertTo(payload, headers)) {
			return null;
		}
		payload = convertToInternal(payload, headers);
		MessageBuilder<?> builder = MessageBuilder.withPayload(payload);
		if (headers != null) {
			builder.copyHeaders(headers);
		}
		MimeType mimeType = getDefaultContentType(payload);
		if (mimeType != null) {
			builder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, mimeType);
		}
		return builder.build();
	}

	protected boolean canConvertTo(Object payload, MessageHeaders headers) {
		Class<?> clazz = (payload != null) ? payload.getClass() : null;
		return (supports(clazz) && supportsMimeType(headers));
	}

	/**
	 * Convert the payload object to serialized form.
	 */
	public abstract Object convertToInternal(Object payload, MessageHeaders headers);

	protected boolean supportsMimeType(MessageHeaders headers) {
		if (getSupportedMimeTypes().isEmpty()) {
			return true;
		}
		MimeType mimeType = getMimeType(headers);
		if (mimeType == null) {
			if (isStrictContentTypeMatch()) {
				return false;
			}
			else {
				return true;
			}
		}
		for (MimeType supported : getSupportedMimeTypes()) {
			if (supported.getType().equals(mimeType.getType()) &&
					supported.getSubtype().equals(mimeType.getSubtype())) {
				return true;
			}
		}
		return false;
	}

	protected MimeType getMimeType(MessageHeaders headers) {
		return (this.contentTypeResolver != null) ? this.contentTypeResolver.resolve(headers) : null;
	}

}
