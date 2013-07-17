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
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleBrokerMessageHandler implements MessageHandler {

	private static final Log logger = LogFactory.getLog(SimpleBrokerMessageHandler.class);

	private final MessageChannel outboundChannel;

	private SubscriptionRegistry subscriptionRegistry = new DefaultSubscriptionRegistry();


	/**
	 * @param outboundChannel the channel to which messages for clients should be sent
	 * @param observable an Observable to use to manage subscriptions
	 */
	public SimpleBrokerMessageHandler(MessageChannel outboundChannel) {
		Assert.notNull(outboundChannel, "outboundChannel is required");
		this.outboundChannel = outboundChannel;
	}


	public void setSubscriptionRegistry(SubscriptionRegistry subscriptionRegistry) {
		Assert.notNull(subscriptionRegistry, "subscriptionRegistry is required");
		this.subscriptionRegistry = subscriptionRegistry;
	}

	public SubscriptionRegistry getSubscriptionRegistry() {
		return this.subscriptionRegistry;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		SimpMessageType messageType = headers.getMessageType();

		if (SimpMessageType.SUBSCRIBE.equals(messageType)) {
			// TODO: need a way to communicate back if subscription was successfully created or
			// not in which case an ERROR should be sent back and close the connection
			// http://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE
			this.subscriptionRegistry.registerSubscription(message);
		}
		else if (SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			this.subscriptionRegistry.unregisterSubscription(message);
		}
		else if (SimpMessageType.MESSAGE.equals(messageType)) {
			sendMessageToSubscribers(headers.getDestination(), message);
		}
		else if (SimpMessageType.DISCONNECT.equals(messageType)) {
			String sessionId = SimpMessageHeaderAccessor.wrap(message).getSessionId();
			this.subscriptionRegistry.unregisterAllSubscriptions(sessionId);
		}
	}

	protected void sendMessageToSubscribers(String destination, Message<?> message) {
		MultiValueMap<String,String> subscriptions = this.subscriptionRegistry.findSubscriptions(message);
		for (String sessionId : subscriptions.keySet()) {
			for (String subscriptionId : subscriptions.get(sessionId)) {

				SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
				headers.setSessionId(sessionId);
				headers.setSubscriptionId(subscriptionId);

				Object payload = message.getPayload();
				Message<?> clientMessage = MessageBuilder.withPayloadAndHeaders(payload, headers).build();
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
}
