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

package org.springframework.web.messaging.service.method;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.event.EventBus;
import org.springframework.web.messaging.service.AbstractMessageService;

import reactor.util.Assert;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageChannelArgumentResolver implements ArgumentResolver {

	private static Log logger = LogFactory.getLog(MessageChannelArgumentResolver.class);

	private final EventBus eventBus;


	public MessageChannelArgumentResolver(EventBus eventBus) {
		Assert.notNull(eventBus, "reactor is required");
		this.eventBus = eventBus;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return MessageChannel.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {

		final String sessionId = (String) message.getHeaders().get("sessionId");

		return new MessageChannel() {

			@Override
			public boolean send(Message<?> message) {

				Map<String, Object> headers = new HashMap<String, Object>(message.getHeaders());
				headers.put("messageType", MessageType.MESSAGE);
				headers.put("sessionId", sessionId);
				message = new GenericMessage<Object>(message.getPayload(), headers);

				if (logger.isTraceEnabled()) {
					logger.trace("Sending notification: " + message);
				}

				String key = AbstractMessageService.MESSAGE_KEY;
				MessageChannelArgumentResolver.this.eventBus.send(key, message);

				return true;
			}
		};
	}

}
