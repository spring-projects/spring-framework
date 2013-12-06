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

package org.springframework.messaging.simp.broker;

import java.util.Collection;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * A "simple" message broker that recognizes the message types defined in
 * {@link SimpMessageType}, keeps track of subscriptions with the help of a
 * {@link SubscriptionRegistry} and sends messages to subscribers.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleBrokerMessageHandler extends AbstractBrokerMessageHandler {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private final SubscribableChannel clientInboundChannel;

	private final MessageChannel clientOutboundChannel;

	private final SubscribableChannel brokerChannel;

	private SubscriptionRegistry subscriptionRegistry = new DefaultSubscriptionRegistry();


	/**
	 * Create a SimpleBrokerMessageHandler instance with the given message channels
	 * and destination prefixes.
	 *
	 * @param clientInboundChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param clientOutboundChannel the channel for sending messages to clients (e.g. WebSocket clients)
	 * @param brokerChannel the channel for the application to send messages to the broker
	 */
	public SimpleBrokerMessageHandler(SubscribableChannel clientInboundChannel, MessageChannel clientOutboundChannel,
			SubscribableChannel brokerChannel, Collection<String> destinationPrefixes) {

		super(destinationPrefixes);

		Assert.notNull(clientInboundChannel, "'clientInboundChannel' must not be null");
		Assert.notNull(clientOutboundChannel, "'clientOutboundChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");


		this.clientInboundChannel = clientInboundChannel;
		this.clientOutboundChannel = clientOutboundChannel;
		this.brokerChannel = brokerChannel;
	}


	public SubscribableChannel getClientInboundChannel() {
		return this.clientInboundChannel;
	}

	public MessageChannel getClientOutboundChannel() {
		return this.clientOutboundChannel;
	}

	public SubscribableChannel getBrokerChannel() {
		return this.brokerChannel;
	}

	public void setSubscriptionRegistry(SubscriptionRegistry subscriptionRegistry) {
		Assert.notNull(subscriptionRegistry, "SubscriptionRegistry must not be null");
		this.subscriptionRegistry = subscriptionRegistry;
	}

	public SubscriptionRegistry getSubscriptionRegistry() {
		return this.subscriptionRegistry;
	}


	@Override
	public void startInternal() {
		publishBrokerAvailableEvent();
		this.clientInboundChannel.subscribe(this);
		this.brokerChannel.subscribe(this);
	}

	@Override
	public void stopInternal() {
		publishBrokerUnavailableEvent();
		this.clientInboundChannel.unsubscribe(this);
		this.brokerChannel.unsubscribe(this);
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		SimpMessageType messageType = headers.getMessageType();
		String destination = headers.getDestination();

		if (!checkDestinationPrefix(destination)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring message to destination=" + destination);
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
			this.clientOutboundChannel.send(connectAck);
		}
	}

	protected void sendMessageToSubscribers(String destination, Message<?> message) {
		MultiValueMap<String,String> subscriptions = this.subscriptionRegistry.findSubscriptions(message);
		if ((subscriptions.size() > 0) && logger.isDebugEnabled()) {
			logger.debug("Sending message with destination=" + destination
					+ " to " + subscriptions.size() + " subscriber(s)");
		}
		for (String sessionId : subscriptions.keySet()) {
			for (String subscriptionId : subscriptions.get(sessionId)) {
				SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
				headers.setSessionId(sessionId);
				headers.setSubscriptionId(subscriptionId);
				Object payload = message.getPayload();
				Message<?> clientMessage = MessageBuilder.withPayload(payload).setHeaders(headers).build();
				try {
					this.clientOutboundChannel.send(clientMessage);
				}
				catch (Throwable ex) {
					logger.error("Failed to send message to destination=" + destination +
							", sessionId=" + sessionId + ", subscriptionId=" + subscriptionId, ex);
				}
			}
		}
	}

}
