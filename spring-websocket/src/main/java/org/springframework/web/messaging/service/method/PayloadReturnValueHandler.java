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
public class PayloadReturnValueHandler implements ReturnValueHandler {

	private MessageChannel clientChannel;


	public PayloadReturnValueHandler(MessageChannel clientChannel) {
		Assert.notNull(clientChannel, "clientChannel is required");
		this.clientChannel = clientChannel;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return true;
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		Assert.notNull(this.clientChannel, "No clientChannel to send messages to");

		if (returnValue == null) {
			return;
		}

		WebMessageHeaderAccesssor headers = WebMessageHeaderAccesssor.wrap(message);

		WebMessageHeaderAccesssor returnHeaders = WebMessageHeaderAccesssor.create();
		returnHeaders.setDestination(headers.getDestination());
		returnHeaders.setSessionId(headers.getSessionId());
		returnHeaders.setSubscriptionId(headers.getSubscriptionId());

		Message<?> returnMessage = MessageBuilder.withPayload(
				returnValue).copyHeaders(returnHeaders.toMap()).build();

		this.clientChannel.send(returnMessage);
 	}

}
