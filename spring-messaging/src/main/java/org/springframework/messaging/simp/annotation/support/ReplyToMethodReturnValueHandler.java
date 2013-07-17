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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.ReplyTo;
import org.springframework.messaging.handler.method.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.ReplyToUser;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;


/**
 * A {@link HandlerMethodReturnValueHandler} for replying to destinations specified in a
 * {@link ReplyTo} or {@link ReplyToUser} method-level annotations.
 * <p>
 * The value returned from the method is converted, and turned to a {@link Message} and
 * sent through the provided {@link MessageChannel}. The
 * message is then enriched with the sessionId of the input message as well as the
 * destination from the annotation(s). If multiple destinations are specified, a copy of
 * the message is sent to each destination.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ReplyToMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final MessageSendingOperations<String> messagingTemplate;


	public ReplyToMethodReturnValueHandler(MessageSendingOperations<String> messagingTemplate) {
		Assert.notNull(messagingTemplate, "messagingTemplate is required");
		this.messagingTemplate = messagingTemplate;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return ((returnType.getMethodAnnotation(ReplyTo.class) != null)
				|| (returnType.getMethodAnnotation(ReplyToUser.class) != null));
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> inputMessage)
			throws Exception {

		if (returnValue == null) {
			return;
		}

		ReplyTo replyTo = returnType.getMethodAnnotation(ReplyTo.class);
		ReplyToUser replyToUser = returnType.getMethodAnnotation(ReplyToUser.class);

		List<String> destinations = new ArrayList<String>();
		if (replyTo != null) {
			destinations.addAll(Arrays.asList(replyTo.value()));
		}
		if (replyToUser != null) {
			Principal user = getUser(inputMessage);
			for (String destination : replyToUser.value()) {
				destinations.add("/user/" + user.getName() + destination);
			}
		}

		MessagePostProcessor postProcessor = new SessionIdHeaderPostProcessor(inputMessage);

		for (String destination : destinations) {
			this.messagingTemplate.convertAndSend(destination, returnValue, postProcessor);
		}
	}

	private Principal getUser(Message<?> inputMessage) {
		SimpMessageHeaderAccessor inputHeaders = SimpMessageHeaderAccessor.wrap(inputMessage);
		Principal user = inputHeaders.getUser();
		if (user == null) {
			throw new MissingSessionUserException(inputMessage);
		}
		return user;
	}


	private final class SessionIdHeaderPostProcessor implements MessagePostProcessor {

		private final Message<?> inputMessage;


		public SessionIdHeaderPostProcessor(Message<?> inputMessage) {
			this.inputMessage = inputMessage;
		}

		@Override
		public Message<?> postProcessMessage(Message<?> message) {
			String headerName = SimpMessageHeaderAccessor.SESSION_ID;
			String sessionId = (String) this.inputMessage.getHeaders().get(headerName);
			return MessageBuilder.fromMessage(message).setHeader(headerName, sessionId).build();
		}
	}
}
