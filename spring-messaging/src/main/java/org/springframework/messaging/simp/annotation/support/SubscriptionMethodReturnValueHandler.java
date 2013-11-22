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

package org.springframework.messaging.simp.annotation.support;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.method.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;


/**
 * A {@link HandlerMethodReturnValueHandler} for replying directly to a subscription. It
 * supports methods annotated with {@link org.springframework.messaging.simp.annotation.SubscribeMapping} unless they're also annotated
 * with {@link SendTo} or {@link SendToUser}.
 * <p>
 * The value returned from the method is converted, and turned to a {@link Message} and
 * then enriched with the sessionId, subscriptionId, and destination of the input message.
 * The message is then sent directly back to the connected client.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SubscriptionMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final MessageSendingOperations<String> messagingTemplate;


	/**
	 * @param messagingTemplate a messaging template for sending messages directly
	 *	to clients, e.g. in response to a subscription
	 */
	public SubscriptionMethodReturnValueHandler(MessageSendingOperations<String> messagingTemplate) {
		Assert.notNull(messagingTemplate, "messagingTemplate is required");
		this.messagingTemplate = messagingTemplate;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return ((returnType.getMethodAnnotation(SubscribeMapping.class) != null)
				&& (returnType.getMethodAnnotation(SendTo.class) == null)
				&& (returnType.getMethodAnnotation(SendToUser.class) == null));
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		if (returnValue == null) {
			return;
		}

		SimpMessageHeaderAccessor inputHeaders = SimpMessageHeaderAccessor.wrap(message);
		String sessionId = inputHeaders.getSessionId();
		String subscriptionId = inputHeaders.getSubscriptionId();
		String destination = inputHeaders.getDestination();

		Assert.state(inputHeaders.getSubscriptionId() != null,
				"No subsriptiondId in input message to method " + returnType.getMethod());

		MessagePostProcessor postProcessor = new SubscriptionHeaderPostProcessor(sessionId, subscriptionId);
		this.messagingTemplate.convertAndSend(destination, returnValue, postProcessor);
	}


	private final class SubscriptionHeaderPostProcessor implements MessagePostProcessor {

		private final String sessionId;

		private final String subscriptionId;


		public SubscriptionHeaderPostProcessor(String sessionId, String subscriptionId) {
			this.sessionId = sessionId;
			this.subscriptionId = subscriptionId;
		}

		@Override
		public Message<?> postProcessMessage(Message<?> message) {
			SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
			headers.setSessionId(this.sessionId);
			headers.setSubscriptionId(this.subscriptionId);
			headers.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
			return MessageBuilder.withPayload(message.getPayload()).setHeaders(headers).build();
		}
	}
}
