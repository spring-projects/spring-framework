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

package org.springframework.web.messaging.service.broker;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.service.AbstractWebMessageHandler;
import org.springframework.web.messaging.support.WebMessageHeaderAccesssor;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleBrokerWebMessageHandler extends AbstractWebMessageHandler {

	private final MessageChannel outboundChannel;

	private SubscriptionRegistry subscriptionRegistry = new DefaultSubscriptionRegistry();


	/**
	 * @param outboundChannel the channel to which messages for clients should be sent
	 * @param observable an Observable to use to manage subscriptions
	 */
	public SimpleBrokerWebMessageHandler(MessageChannel outboundChannel) {
		Assert.notNull(outboundChannel, "outboundChannel is required");
		this.outboundChannel = outboundChannel;
	}


	public void setSubscriptionRegistry(SubscriptionRegistry subscriptionRegistry) {
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

		this.subscriptionRegistry.addSubscription(message);

		// TODO: need a way to communicate back if subscription was successfully created or
		// not in which case an ERROR should be sent back and close the connection
		// http://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE
	}

	@Override
	protected void handleUnsubscribe(Message<?> message) {
		this.subscriptionRegistry.removeSubscription(message);
	}

	@Override
	public void handlePublish(Message<?> message) {

		if (logger.isDebugEnabled()) {
			logger.debug("Message received: " + message);
		}

		String destination = WebMessageHeaderAccesssor.wrap(message).getDestination();

		MultiValueMap<String,String> subscriptions = this.subscriptionRegistry.findSubscriptions(message);

		for (String sessionId : subscriptions.keySet()) {
			for (String subscriptionId : subscriptions.get(sessionId)) {

				WebMessageHeaderAccesssor headers = WebMessageHeaderAccesssor.wrap(message);
				headers.setSessionId(sessionId);
				headers.setSubscriptionId(subscriptionId);

				Message<?> clientMessage = MessageBuilder.withPayload(
						message.getPayload()).copyHeaders(headers.toMap()).build();

				try {
					this.outboundChannel.send(clientMessage);
				}
				catch (Throwable ex) {
					logger.error("Failed to send message to destination=" + destination +
							", sessionId=" + sessionId + ", subscriptionId=" + subscriptionId, ex);
				}
			}
		}
	}

	@Override
	public void handleDisconnect(Message<?> message) {
		String sessionId = WebMessageHeaderAccesssor.wrap(message).getSessionId();
		this.subscriptionRegistry.removeSessionSubscriptions(sessionId);
	}

}
