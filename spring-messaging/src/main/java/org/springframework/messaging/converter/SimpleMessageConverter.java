/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.ClassUtils;

/**
 * A simple converter that simply unwraps the message payload as long as it matches the
 * expected target class. Or reversely, simply wraps the payload in a message.
 *
 * <p>Note that this converter ignores any content type information that may be present in
 * message headers and should not be used if payload conversion is actually required.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleMessageConverter implements MessageConverter {

	@Override
	public @Nullable Object fromMessage(Message<?> message, Class<?> targetClass) {
		Object payload = message.getPayload();
		return (ClassUtils.isAssignableValue(targetClass, payload) ? payload : null);
	}

	@Override
	public Message<?> toMessage(Object payload, @Nullable MessageHeaders headers) {
		if (headers != null) {
			MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(headers, MessageHeaderAccessor.class);
			if (accessor != null && accessor.isMutable()) {
				return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
			}
		}
		return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
	}

}
