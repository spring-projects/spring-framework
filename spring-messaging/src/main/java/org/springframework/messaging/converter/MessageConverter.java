/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * A converter to turn the payload of a {@link Message} from serialized form to a typed
 * Object and vice versa. The {@link MessageHeaders#CONTENT_TYPE} message header may be
 * used to specify the media type of the message content.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface MessageConverter {

	/**
	 * Convert the payload of a {@link Message} from a serialized form to a typed Object
	 * of the specified target class. The {@link MessageHeaders#CONTENT_TYPE} header
	 * should indicate the MIME type to convert from.
	 * <p>If the converter does not support the specified media type or cannot perform
	 * the conversion, it should return {@code null}.
	 * @param message the input message
	 * @param targetClass the target class for the conversion
	 * @return the result of the conversion, or {@code null} if the converter cannot
	 * perform the conversion
	 */
	@Nullable Object fromMessage(Message<?> message, Class<?> targetClass);

	/**
	 * Create a {@link Message} whose payload is the result of converting the given
	 * payload Object to serialized form. The optional {@link MessageHeaders} parameter
	 * may contain a {@link MessageHeaders#CONTENT_TYPE} header to specify the target
	 * media type for the conversion and it may contain additional headers to be added
	 * to the message.
	 * <p>If the converter does not support the specified media type or cannot perform
	 * the conversion, it should return {@code null}.
	 * @param payload the Object to convert
	 * @param headers optional headers for the message (may be {@code null})
	 * @return the new message, or {@code null} if the converter does not support the
	 * Object type or the target media type
	 */
	@Nullable Message<?> toMessage(Object payload, @Nullable MessageHeaders headers);

}
