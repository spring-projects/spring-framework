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

package org.springframework.messaging.simp.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Set;


/**
 * Provides support for messages sent to "user" destinations, translating the
 * destination to one or more user-specific destination(s) and then sending message(s)
 * with the updated target destination using the provided messaging template.
 * <p>
 * See {@link UserDestinationResolver} for more details and examples.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class UserDestinationMessageHandler implements MessageHandler {

	private static final Log logger = LogFactory.getLog(UserDestinationMessageHandler.class);


	private final MessageSendingOperations<String> messagingTemplate;

	private final UserDestinationResolver userDestinationResolver;


	/**
	 * Create an instance of the handler with the given messaging template and a
	 * user destination resolver.
	 *
	 * @param messagingTemplate a messaging template to use for sending messages
	 *		with translated user destinations
	 * @param userDestinationResolver the resolver to use to find queue suffixes for a user
	 */
	public UserDestinationMessageHandler(MessageSendingOperations<String> messagingTemplate,
			UserDestinationResolver userDestinationResolver) {

		Assert.notNull(messagingTemplate, "messagingTemplate is required");
		Assert.notNull(userDestinationResolver, "destinationResolver is required");

		this.messagingTemplate = messagingTemplate;
		this.userDestinationResolver = userDestinationResolver;
	}

	/**
	 * Return the configured {@link UserDestinationResolver}.
	 */
	public UserDestinationResolver getUserDestinationResolver() {
		return this.userDestinationResolver;
	}

	/**
	 * Return the configured messaging template for sending messages with
	 * translated destinations.
	 */
	public MessageSendingOperations<String> getMessagingTemplate() {
		return this.messagingTemplate;
	}


	@Override
	public void handleMessage(Message<?> message) throws MessagingException {

		if (logger.isTraceEnabled()) {
			logger.trace("Handling message " + message);
		}

		Set<String> destinations = this.userDestinationResolver.resolveDestination(message);
		if (CollectionUtils.isEmpty(destinations)) {
			return;
		}

		for (String targetDestination : destinations) {
			if (logger.isTraceEnabled()) {
				logger.trace("Sending message to resolved user destination: " + targetDestination);
			}
			SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
			headers.setDestination(targetDestination);
			message = MessageBuilder.withPayload(message.getPayload()).setHeaders(headers).build();
			this.messagingTemplate.send(targetDestination, message);
		}
	}

}
