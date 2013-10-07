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

import java.lang.annotation.Annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.method.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;


/**
 * A {@link HandlerMethodReturnValueHandler} for sending to destinations specified in a
 * {@link SendTo} or {@link SendToUser} method-level annotations.
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
public class SendToMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final SimpMessageSendingOperations messagingTemplate;

	private final boolean annotationRequired;


	public SendToMethodReturnValueHandler(SimpMessageSendingOperations messagingTemplate, boolean annotationRequired) {
		Assert.notNull(messagingTemplate, "messagingTemplate is required");
		this.messagingTemplate = messagingTemplate;
		this.annotationRequired = annotationRequired;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		if ((returnType.getMethodAnnotation(SendTo.class) != null) ||
				(returnType.getMethodAnnotation(SendToUser.class) != null)) {
			return true;
		}
		return (!this.annotationRequired);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> inputMessage)
			throws Exception {

		if (returnValue == null) {
			return;
		}

		SimpMessageHeaderAccessor inputHeaders = SimpMessageHeaderAccessor.wrap(inputMessage);

		String sessionId = inputHeaders.getSessionId();
		MessagePostProcessor postProcessor = new SessionHeaderPostProcessor(sessionId);

		SendToUser sendToUser = returnType.getMethodAnnotation(SendToUser.class);
		if (sendToUser != null) {
			if (inputHeaders.getUser() == null) {
				throw new MissingSessionUserException(inputMessage);
			}
			String user = inputHeaders.getUser().getName();
			for (String destination : getDestinations(sendToUser, inputHeaders.getDestination())) {
				this.messagingTemplate.convertAndSendToUser(user, destination, returnValue, postProcessor);
			}
			return;
		}

		SendTo sendTo = returnType.getMethodAnnotation(SendTo.class);
		if (sendTo != null) {
			for (String destination : getDestinations(sendTo, inputHeaders.getDestination())) {
				this.messagingTemplate.convertAndSend(destination, returnValue, postProcessor);
			}
			return;
		}

		this.messagingTemplate.convertAndSend(inputHeaders.getDestination(), returnValue, postProcessor);
	}

	private String[] getDestinations(Annotation annot, String inputDestination) {
		String[] destinations = (String[]) AnnotationUtils.getValue(annot);
		return ObjectUtils.isEmpty(destinations) ? new String[] { inputDestination } : destinations;
	}


	private final class SessionHeaderPostProcessor implements MessagePostProcessor {

		private final String sessionId;

		public SessionHeaderPostProcessor(String sessionId) {
			this.sessionId = sessionId;
		}

		@Override
		public Message<?> postProcessMessage(Message<?> message) {
			SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
			headers.setSessionId(this.sessionId);
			return MessageBuilder.withPayload(message.getPayload()).setHeaders(headers).build();
		}
	}

	@Override
	public String toString() {
		return "SendToMethodReturnValueHandler [annotationRequired=" + annotationRequired + "]";
	}

}
