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

import java.util.Collection;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleBrokerMessageHandler extends AbstractBrokerMessageHandler {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private final MessageChannel messageChannel;

	private SubscriptionRegistry subscriptionRegistry = new DefaultSubscriptionRegistry();


	/**
	 * @param messageChannel the channel to broadcast messages to
	 */
	public SimpleBrokerMessageHandler(MessageChannel messageChannel, Collection<String> destinationPrefixes) {
		super(destinationPrefixes);
		Assert.notNull(messageChannel, "messageChannel is required");
		this.messageChannel = messageChannel;
	}


	public MessageChannel getMessageChannel() {
		return this.messageChannel;
	}

	public void setSubscriptionRegistry(SubscriptionRegistry subscriptionRegistry) {
		Assert.notNull(subscriptionRegistry, "subscriptionRegistry is required");
		this.subscriptionRegistry = subscriptionRegistry;
	}

	public SubscriptionRegistry getSubscriptionRegistry() {
		return this.subscriptionRegistry;
	}


	@Override
	public void startInternal() {
		publishBrokerAvailableEvent();
	}

	@Override
	public void stopInternal() {
		publishBrokerUnavailableEvent();
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		SimpMessageType messageType = headers.getMessageType();
		String destination = headers.getDestination();

		if (!checkDestinationPrefix(destination)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Ingoring message with destination " + destination);
			}
			return;
		}

		if (SimpMessageType.SUBSCRIBE.equals(messageType)) {
			this.subscriptionRegistry.registerSubscription(message);
		}
		else if (SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			this.subscriptionRegistry.unregisterSubscription(message);
		}
		else if (SimpMessageType.MESSAGE.equals(messageType)) {
			sendMessageToSubscribers(headers.getDestination(), message);
		}
		else if (SimpMessageType.DISCONNECT.equals(messageType)) {
			String sessionId = headers.getSessionId();
			this.subscriptionRegistry.unregisterAllSubscriptions(sessionId);
		}
		else if (SimpMessageType.CONNECT.equals(messageType)) {
			SimpMessageHeaderAccessor replyHeaders = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT_ACK);
			replyHeaders.setSessionId(headers.getSessionId());
			replyHeaders.setHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER, message);

			Message<byte[]> connectAck = MessageBuilder.withPayload(EMPTY_PAYLOAD).setHeaders(replyHeaders).build();
			this.messageChannel.send(connectAck);
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
				Message<?> clientMessage = MessageBuilder.withPayload(payload).setHeaders(headers).build();
				try {
					this.messageChannel.send(clientMessage);
				}
				catch (Throwable ex) {
					logger.error("Failed to send message to destination=" + destination +
							", sessionId=" + sessionId + ", subscriptionId=" + subscriptionId, ex);
				}
			}
		}
	}

}
