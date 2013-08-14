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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.BrokerAvailabilityEvent;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleBrokerMessageHandler implements MessageHandler, ApplicationEventPublisherAware,
		SmartLifecycle {

	private static final Log logger = LogFactory.getLog(SimpleBrokerMessageHandler.class);

	private final MessageChannel messageChannel;

	private List<String> destinationPrefixes;

	private SubscriptionRegistry subscriptionRegistry = new DefaultSubscriptionRegistry();

	private ApplicationEventPublisher eventPublisher;

	private volatile boolean running = false;


	/**
	 * @param messageChannel the channel to broadcast messages to
	 */
	public SimpleBrokerMessageHandler(MessageChannel messageChannel) {
		Assert.notNull(messageChannel, "messageChannel is required");
		this.messageChannel = messageChannel;
	}


	public void setDestinationPrefixes(List<String> destinationPrefixes) {
		this.destinationPrefixes = destinationPrefixes;
	}

	public List<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}

	public void setSubscriptionRegistry(SubscriptionRegistry subscriptionRegistry) {
		Assert.notNull(subscriptionRegistry, "subscriptionRegistry is required");
		this.subscriptionRegistry = subscriptionRegistry;
	}

	public SubscriptionRegistry getSubscriptionRegistry() {
		return this.subscriptionRegistry;
	}

	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		SimpMessageType messageType = headers.getMessageType();
		String destination = headers.getDestination();

		if (!checkDestinationPrefix(destination)) {
			return;
		}

		if (SimpMessageType.SUBSCRIBE.equals(messageType)) {
			preProcessMessage(message);
			this.subscriptionRegistry.registerSubscription(message);
		}
		else if (SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			preProcessMessage(message);
			this.subscriptionRegistry.unregisterSubscription(message);
		}
		else if (SimpMessageType.MESSAGE.equals(messageType)) {
			preProcessMessage(message);
			sendMessageToSubscribers(headers.getDestination(), message);
		}
		else if (SimpMessageType.DISCONNECT.equals(messageType)) {
			preProcessMessage(message);
			String sessionId = SimpMessageHeaderAccessor.wrap(message).getSessionId();
			this.subscriptionRegistry.unregisterAllSubscriptions(sessionId);
		}
	}

	private boolean checkDestinationPrefix(String destination) {
		if ((destination == null) || CollectionUtils.isEmpty(this.destinationPrefixes)) {
			return true;
		}
		for (String prefix : this.destinationPrefixes) {
			if (destination.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private void preProcessMessage(Message<?> message) {
		if (logger.isTraceEnabled()) {
			logger.trace("Processing " + message);
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
					this.messageChannel.send(clientMessage);
				}
				catch (Throwable ex) {
					logger.error("Failed to send message to destination=" + destination +
							", sessionId=" + sessionId + ", subscriptionId=" + subscriptionId, ex);
				}
			}
		}
	}

	@Override
	public void start() {
		this.eventPublisher.publishEvent(new BrokerAvailabilityEvent(true, this));
		this.running = true;
	}

	@Override
	public void stop() {
		this.running = false;
		this.eventPublisher.publishEvent(new BrokerAvailabilityEvent(false, this));
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		callback.run();
		this.stop();
	}

}
