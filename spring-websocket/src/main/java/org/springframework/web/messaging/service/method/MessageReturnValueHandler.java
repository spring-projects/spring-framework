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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.messaging.support.WebMessageHeaderAccesssor;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageReturnValueHandler implements ReturnValueHandler {

	private MessageChannel outboundChannel;


	public MessageReturnValueHandler(MessageChannel outboundChannel) {
		Assert.notNull(outboundChannel, "outboundChannel is required");
		this.outboundChannel = outboundChannel;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// TODO: List<Message> return value
		Class<?> paramType = returnType.getParameterType();
		return Message.class.isAssignableFrom(paramType);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		Assert.notNull(this.outboundChannel, "No clientChannel to send messages to");

		Message<?> returnMessage = (Message<?>) returnValue;
		if (message == null) {
			return;
		}

		WebMessageHeaderAccesssor headers = WebMessageHeaderAccesssor.wrap(message);
		Assert.notNull(headers.getSubscriptionId(), "No subscription id: " + message);

		WebMessageHeaderAccesssor returnHeaders = WebMessageHeaderAccesssor.wrap(returnMessage);
		returnHeaders.setSessionId(headers.getSessionId());
		returnHeaders.setSubscriptionId(headers.getSubscriptionId());

		if (returnHeaders.getDestination() == null) {
			returnHeaders.setDestination(headers.getDestination());
		}

		returnMessage = MessageBuilder.withPayload(
				returnMessage.getPayload()).copyHeaders(returnHeaders.toMap()).build();

		this.outboundChannel.send(returnMessage);
 	}

}
