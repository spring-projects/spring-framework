/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.user;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.util.Assert;

/**
 * {@code MessageHandler} with support for "user" destinations.
 *
 * <p>Listens for messages with "user" destinations, translates their destination
 * to actual target destinations unique to the active session(s) of a user, and
 * then sends the resolved messages to the broker channel to be delivered.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class UserDestinationMessageHandler implements MessageHandler, SmartLifecycle {

	private static final Log logger = LogFactory.getLog(UserDestinationMessageHandler.class);


	private final SubscribableChannel clientInboundChannel;

	private final SubscribableChannel brokerChannel;

	private final MessageSendingOperations<String> messagingTemplate;

	private final UserDestinationResolver destinationResolver;

	private MessageHeaderInitializer headerInitializer;

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running = false;


	/**
	 * Create an instance with the given client and broker channels subscribing
	 * to handle messages from each and then sending any resolved messages to the
	 * broker channel.
	 * @param clientInboundChannel messages received from clients.
	 * @param brokerChannel messages sent to the broker.
	 * @param resolver the resolver for "user" destinations.
	 */
	public UserDestinationMessageHandler(SubscribableChannel clientInboundChannel,
			SubscribableChannel brokerChannel, UserDestinationResolver resolver) {

		Assert.notNull(clientInboundChannel, "'clientInChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");
		Assert.notNull(resolver, "resolver must not be null");

		this.clientInboundChannel = clientInboundChannel;
		this.brokerChannel = brokerChannel;
		this.messagingTemplate = new SimpMessagingTemplate(brokerChannel);
		this.destinationResolver = resolver;
	}


	/**
	 * Return the configured {@link UserDestinationResolver}.
	 */
	public UserDestinationResolver getUserDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * Return the messaging template used to send resolved messages to the
	 * broker channel.
	 */
	public MessageSendingOperations<String> getBrokerMessagingTemplate() {
		return this.messagingTemplate;
	}

	/**
	 * Configure a custom {@link MessageHeaderInitializer} to initialize the
	 * headers of resolved target messages.
	 * <p>By default this is not set.
	 */
	public void setHeaderInitializer(MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * Return the configured header initializer.
	 */
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}


	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public final boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			this.clientInboundChannel.subscribe(this);
			this.brokerChannel.subscribe(this);
			this.running = true;
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.clientInboundChannel.unsubscribe(this);
			this.brokerChannel.unsubscribe(this);
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}


	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		UserDestinationResult result = this.destinationResolver.resolveDestination(message);
		if (result == null) {
			return;
		}
		Set<String> destinations = result.getTargetDestinations();
		if (destinations.isEmpty()) {
			if (logger.isTraceEnabled()) {
				logger.trace("No user destinations found for " + result.getSourceDestination());
			}
			return;
		}
		if (SimpMessageType.MESSAGE.equals(SimpMessageHeaderAccessor.getMessageType(message.getHeaders()))) {
			SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
			initHeaders(accessor);
			String header = SimpMessageHeaderAccessor.ORIGINAL_DESTINATION;
			accessor.setNativeHeader(header, result.getSubscribeDestination());
			message = MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Translated " + result.getSourceDestination() + " -> " + destinations);
		}
		for (String destination : destinations) {
			this.messagingTemplate.send(destination, message);
		}
	}

	private void initHeaders(SimpMessageHeaderAccessor headerAccessor) {
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(headerAccessor);
		}
	}

	@Override
	public String toString() {
		return "UserDestinationMessageHandler[" + this.destinationResolver + "]";
	}

}
