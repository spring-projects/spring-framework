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

package org.springframework.web.messaging.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.SessionSubscriptionRegistration;
import org.springframework.web.messaging.SessionSubscriptionRegistry;
import org.springframework.web.messaging.support.CachingSessionSubscriptionRegistry;
import org.springframework.web.messaging.support.DefaultSessionSubscriptionRegistry;
import org.springframework.web.messaging.support.WebMessageHeaderAccesssor;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleBrokerWebMessageHandler extends AbstractWebMessageHandler {

	private final MessageChannel clientChannel;

	private SessionSubscriptionRegistry subscriptionRegistry=
			new CachingSessionSubscriptionRegistry(new DefaultSessionSubscriptionRegistry());


	/**
	 * @param clientChannel the channel to which messages for clients should be sent
	 * @param observable an Observable to use to manage subscriptions
	 */
	public SimpleBrokerWebMessageHandler(MessageChannel clientChannel) {
		Assert.notNull(clientChannel, "clientChannel is required");
		this.clientChannel = clientChannel;
	}


	public void setSubscriptionRegistry(SessionSubscriptionRegistry subscriptionRegistry) {
		Assert.notNull(subscriptionRegistry, "subscriptionRegistry is required");
		this.subscriptionRegistry = subscriptionRegistry;
	}

	@Override
	protected Collection<MessageType> getSupportedMessageTypes() {
		return Arrays.asList(MessageType.MESSAGE, MessageType.SUBSCRIBE, MessageType.UNSUBSCRIBE);
	}

	@Override
	public void handleSubscribe(Message<?> message) {

		if (logger.isDebugEnabled()) {
			logger.debug("Subscribe " + message);
		}

		WebMessageHeaderAccesssor headers = WebMessageHeaderAccesssor.wrap(message);
		String sessionId = headers.getSessionId();
		String subscriptionId = headers.getSubscriptionId();
		String destination = headers.getDestination();

		SessionSubscriptionRegistration registration = this.subscriptionRegistry.getOrCreateRegistration(sessionId);
		registration.addSubscription(destination, subscriptionId);
	}

	@Override
	public void handlePublish(Message<?> message) {

		if (logger.isDebugEnabled()) {
			logger.debug("Message received: " + message);
		}

		String destination = WebMessageHeaderAccesssor.wrap(message).getDestination();

		Set<SessionSubscriptionRegistration> registrations =
				this.subscriptionRegistry.getRegistrationsByDestination(destination);

		if (registrations == null) {
			return;
		}

		for (SessionSubscriptionRegistration registration : registrations) {
			for (String subscriptionId : registration.getSubscriptionsByDestination(destination)) {

				WebMessageHeaderAccesssor headers = WebMessageHeaderAccesssor.wrap(message);
				headers.setSessionId(registration.getSessionId());
				headers.setSubscriptionId(subscriptionId);

				Message<?> clientMessage = MessageBuilder.withPayload(
						message.getPayload()).copyHeaders(headers.toMap()).build();

				try {
					this.clientChannel.send(clientMessage);
				}
				catch (Throwable ex) {
					logger.error("Failed to send message to destination=" + destination +
							", sessionId=" + registration.getSessionId() + ", subscriptionId=" + subscriptionId, ex);
				}
			}
		}
	}

	@Override
	public void handleDisconnect(Message<?> message) {
		String sessionId = WebMessageHeaderAccesssor.wrap(message).getSessionId();
		this.subscriptionRegistry.removeRegistration(sessionId);
	}

}
