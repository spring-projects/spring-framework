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

import org.springframework.core.MethodParameter;
import org.springframework.messaging.GenericMessageFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageFactory;
import org.springframework.util.Assert;
import org.springframework.web.messaging.PubSubHeaders;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageChannelArgumentResolver implements ArgumentResolver {

	private final MessageChannel publishChannel;

	private MessageFactory messageFactory;

	public MessageChannelArgumentResolver(MessageChannel publishChannel) {
		Assert.notNull(publishChannel, "publishChannel is required");
		this.publishChannel = publishChannel;
		this.messageFactory = new GenericMessageFactory();
	}

	public void setMessageFactory(MessageFactory messageFactory) {
		this.messageFactory = messageFactory;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return MessageChannel.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {

		final String sessionId = PubSubHeaders.fromMessageHeaders(message.getHeaders()).getSessionId();

		return new MessageChannel<Message<?>>() {

			@Override
			public boolean send(Message<?> message) {
				return send(message, -1);
			}

			@SuppressWarnings("unchecked")
			@Override
			public boolean send(Message<?> message, long timeout) {
				PubSubHeaders headers = PubSubHeaders.fromMessageHeaders(message.getHeaders());
				headers.setSessionId(sessionId);
				publishChannel.send(messageFactory.createMessage(message.getPayload(), headers.toMessageHeaders()));
				return true;
			}
		};
	}

}
