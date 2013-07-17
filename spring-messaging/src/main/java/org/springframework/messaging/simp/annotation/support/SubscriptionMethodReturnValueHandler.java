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
import org.springframework.messaging.handler.annotation.ReplyTo;
import org.springframework.messaging.handler.method.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.ReplyToUser;
import org.springframework.messaging.simp.annotation.SubscribeEvent;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;


/**
 * A {@link HandlerMethodReturnValueHandler} for replying directly to a subscription. It
 * supports methods annotated with {@link SubscribeEvent} that do not also annotated with
 * neither {@link ReplyTo} nor {@link ReplyToUser}.
 *
 * <p>The value returned from the method is converted, and turned to a {@link Message} and
 * then enriched with the sessionId, subscriptionId, and destination of the input message.
 * The message is then sent directly back to the connected client.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SubscriptionMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final MessageSendingOperations<String> messagingTemplate;


	public SubscriptionMethodReturnValueHandler(MessageSendingOperations<String> messagingTemplate) {
		Assert.notNull(messagingTemplate, "messagingTemplate is required");
		this.messagingTemplate = messagingTemplate;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return ((returnType.getMethodAnnotation(SubscribeEvent.class) != null)
				&& (returnType.getMethodAnnotation(ReplyTo.class) == null)
				&& (returnType.getMethodAnnotation(ReplyToUser.class) == null));
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		if (returnValue == null) {
			return;
		}

		SimpMessageHeaderAccessor inputHeaders = SimpMessageHeaderAccessor.wrap(message);
		String destination = inputHeaders.getDestination();

		Assert.state(inputHeaders.getSubscriptionId() != null,
				"No subsriptiondId in input message. Add @ReplyTo or @ReplyToUser to method: "
						+ returnType.getMethod());

		MessagePostProcessor postProcessor = new InputHeaderCopyingPostProcessor(inputHeaders);
		this.messagingTemplate.convertAndSend(destination, returnValue, postProcessor);
	}


	private final class InputHeaderCopyingPostProcessor implements MessagePostProcessor {

		private final SimpMessageHeaderAccessor inputHeaders;


		public InputHeaderCopyingPostProcessor(SimpMessageHeaderAccessor inputHeaders) {
			this.inputHeaders = inputHeaders;
		}

		@Override
		public Message<?> postProcessMessage(Message<?> message) {
			SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
			return MessageBuilder.fromMessage(message)
					.setHeader(SimpMessageHeaderAccessor.SESSION_ID, this.inputHeaders.getSessionId())
					.setHeader(SimpMessageHeaderAccessor.SUBSCRIPTION_ID, this.inputHeaders.getSubscriptionId())
					.copyHeaders(headers.toMap()).build();
		}
	}
}
