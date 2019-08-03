/*
 * Copyright 2002-2014 the original author or authors.
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
	@Nullable
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
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
