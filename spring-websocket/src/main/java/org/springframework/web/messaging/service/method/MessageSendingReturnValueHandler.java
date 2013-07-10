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
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.messaging.support.WebMessageHeaderAccesssor;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageSendingReturnValueHandler implements ReturnValueHandler {

	private MessageChannel outboundChannel;

	private final MessageConverter converter;


	public MessageSendingReturnValueHandler(MessageChannel outboundChannel, MessageConverter<?> converter) {
		Assert.notNull(outboundChannel, "outboundChannel is required");
		Assert.notNull(converter, "converter is required");
		this.outboundChannel = outboundChannel;
		this.converter = converter;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		if (returnValue == null) {
			return;
		}

		WebMessageHeaderAccesssor inputHeaders = WebMessageHeaderAccesssor.wrap(message);
		Message<?> returnMessage = (returnValue instanceof Message) ? (Message<?>) returnValue : null;
		Object returnPayload = (returnMessage != null) ? returnMessage.getPayload() : returnValue;

		WebMessageHeaderAccesssor returnHeaders = (returnMessage != null) ?
				WebMessageHeaderAccesssor.wrap(returnMessage) : WebMessageHeaderAccesssor.create();

		returnHeaders.setSessionId(inputHeaders.getSessionId());
		returnHeaders.setSubscriptionId(inputHeaders.getSubscriptionId());
		if (returnHeaders.getDestination() == null) {
			returnHeaders.setDestination(inputHeaders.getDestination());
		}

		returnMessage = this.converter.toMessage(returnPayload);
		returnMessage = MessageBuilder.fromMessage(returnMessage).copyHeaders(returnHeaders.toMap()).build();

		this.outboundChannel.send(returnMessage);
 	}

}
