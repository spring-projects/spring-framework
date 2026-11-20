/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeTypeUtils;

/**
 * A {@link MessageConverter} that supports MIME type "application/octet-stream" with the
 * payload converted to and from a byte[].
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ByteArrayMessageConverter extends AbstractMessageConverter {

	public ByteArrayMessageConverter() {
		super(MimeTypeUtils.APPLICATION_OCTET_STREAM);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return (byte[].class == clazz);
	}

	@Override
	protected @Nullable Object convertFromInternal(
			Message<?> message, @Nullable Class<?> targetClass, @Nullable Object conversionHint) {

		return message.getPayload();
	}

	@Override
	protected @Nullable Object convertToInternal(
			Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {

		return payload;
	}

}
