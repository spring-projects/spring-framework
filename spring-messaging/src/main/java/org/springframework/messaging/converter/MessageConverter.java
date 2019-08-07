/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.converter;

import org.springframework.lang.Nullable;
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
	@Nullable
	Object fromMessage(Message<?> message, Class<?> targetClass);

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
	@Nullable
	Message<?> toMessage(Object payload, @Nullable MessageHeaders headers);

}
