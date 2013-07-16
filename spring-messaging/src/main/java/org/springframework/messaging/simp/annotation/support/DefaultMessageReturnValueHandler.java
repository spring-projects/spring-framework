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

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.ReplyTo;
import org.springframework.messaging.handler.method.MessageReturnValueHandler;
import org.springframework.messaging.handler.method.MissingSessionUserException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.converter.MessageConverter;
import org.springframework.util.Assert;


/**
 * Expects return values to be either a {@link Message} or the payload of a message to be
 * converted and sent on a {@link MessageChannel}.
 *
 * <p>This {@link MessageReturnValueHandler} should be ordered last as it supports all
 * return value types.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultMessageReturnValueHandler implements MessageReturnValueHandler {

	private MessageChannel inboundChannel;

	private MessageChannel outboundChannel;

	private final MessageConverter converter;


	public DefaultMessageReturnValueHandler(MessageChannel inboundChannel, MessageChannel outboundChannel,
			MessageConverter<?> converter) {

		Assert.notNull(inboundChannel, "inboundChannel is required");
		Assert.notNull(outboundChannel, "outboundChannel is required");
		Assert.notNull(converter, "converter is required");

		this.inboundChannel = inboundChannel;
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

		SimpMessageHeaderAccessor inputHeaders = SimpMessageHeaderAccessor.wrap(message);

		Message<?> returnMessage = (returnValue instanceof Message) ? (Message<?>) returnValue : null;
		Object returnPayload = (returnMessage != null) ? returnMessage.getPayload() : returnValue;

		SimpMessageHeaderAccessor returnHeaders = (returnMessage != null) ?
				SimpMessageHeaderAccessor.wrap(returnMessage) : SimpMessageHeaderAccessor.create();

		returnHeaders.setSessionId(inputHeaders.getSessionId());
		returnHeaders.setSubscriptionId(inputHeaders.getSubscriptionId());

		String destination = getDestination(message, returnType, inputHeaders, returnHeaders);
		returnHeaders.setDestination(destination);

		returnMessage = this.converter.toMessage(returnPayload);
		returnMessage = MessageBuilder.fromMessage(returnMessage).copyHeaders(returnHeaders.toMap()).build();

		if (destination.startsWith("/user/")) {
			this.inboundChannel.send(returnMessage);
		}
		else {
			this.outboundChannel.send(returnMessage);
		}
 	}

	protected String getDestination(Message<?> inputMessage, MethodParameter returnType,
			SimpMessageHeaderAccessor inputHeaders, SimpMessageHeaderAccessor returnHeaders) {

		ReplyTo annot = returnType.getMethodAnnotation(ReplyTo.class);

		if (returnHeaders.getDestination() != null) {
			return returnHeaders.getDestination();
		}
		else if (annot != null) {
			Principal user = inputHeaders.getUser();
			if (user == null) {
				throw new MissingSessionUserException(inputMessage);
			}
			return "/user/" + user.getName() + annot.value();
		}
		else if (inputHeaders.getDestination() != null) {
			return inputHeaders.getDestination();
		}
		else {
			return null;
		}

	}

}
