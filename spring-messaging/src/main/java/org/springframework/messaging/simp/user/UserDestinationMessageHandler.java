/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.util.Assert;

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
public class UserDestinationMessageHandler implements MessageHandler, SmartLifecycle {

	public static final String SUBSCRIBE_DESTINATION = "subscribeDestination";

	private static final Log logger = LogFactory.getLog(UserDestinationMessageHandler.class);


	private final SubscribableChannel clientInboundChannel;

	private final SubscribableChannel brokerChannel;

	private final MessageSendingOperations<String> brokerMessagingTemplate;

	private final UserDestinationResolver userDestinationResolver;

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running = false;


	/**
	 * Create an instance of the handler with the given messaging template and a
	 * user destination resolver.
	 * @param clientInChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param brokerChannel the channel for sending messages with translated user destinations
	 * @param userDestinationResolver the resolver to use to find queue suffixes for a user
	 */
	public UserDestinationMessageHandler(SubscribableChannel clientInChannel,
			SubscribableChannel brokerChannel, UserDestinationResolver userDestinationResolver) {

		Assert.notNull(clientInChannel, "'clientInChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");
		Assert.notNull(userDestinationResolver, "DestinationResolver must not be null");

		this.clientInboundChannel = clientInChannel;
		this.brokerChannel = brokerChannel;
		this.brokerMessagingTemplate = new SimpMessagingTemplate(brokerChannel);
		this.userDestinationResolver = userDestinationResolver;
	}


	/**
	 * Return the configured messaging template for sending messages with
	 * translated destinations.
	 */
	public MessageSendingOperations<String> getBrokerMessagingTemplate() {
		return this.brokerMessagingTemplate;
	}

	/**
	 * Return the configured {@link UserDestinationResolver}.
	 */
	public UserDestinationResolver getUserDestinationResolver() {
		return this.userDestinationResolver;
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
		UserDestinationResult result = this.userDestinationResolver.resolveDestination(message);
		if (result == null) {
			return;
		}
		Set<String> destinations = result.getTargetDestinations();
		if (destinations.isEmpty()) {
			return;
		}
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(message);
		if (SimpMessageType.MESSAGE.equals(headerAccessor.getMessageType())) {
			headerAccessor.setHeader(SUBSCRIBE_DESTINATION, result.getSubscribeDestination());
			message = MessageBuilder.withPayload(message.getPayload()).setHeaders(headerAccessor).build();
		}
		for (String targetDestination : destinations) {
			if (logger.isDebugEnabled()) {
				logger.debug("Sending message to resolved destination=" + targetDestination);
			}
			this.brokerMessagingTemplate.send(targetDestination, message);
		}
	}

}
