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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

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

	private static final Log logger = LogFactory.getLog(UserDestinationMessageHandler.class);


	private final SubscribableChannel clientInboundChannel;

	private final MessageChannel clientOutboundChannel;

	private final SubscribableChannel brokerChannel;

	private final MessageSendingOperations<String> brokerMessagingTemplate;

	private final UserDestinationResolver userDestinationResolver;

	private Object lifecycleMonitor = new Object();

	private volatile boolean running = false;


	/**
	 * Create an instance of the handler with the given messaging template and a
	 * user destination resolver.
	 * @param clientInChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param clientOutChannel the channel for sending messages to clients (e.g. WebSocket clients)
	 * @param brokerChannel the channel for sending messages with translated user destinations
	 * @param userDestinationResolver the resolver to use to find queue suffixes for a user
	 */
	public UserDestinationMessageHandler(SubscribableChannel clientInChannel, MessageChannel clientOutChannel,
			SubscribableChannel brokerChannel, UserDestinationResolver userDestinationResolver) {

		Assert.notNull(clientInChannel, "'clientInChannel' must not be null");
		Assert.notNull(clientOutChannel, "'clientOutChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");
		Assert.notNull(userDestinationResolver, "DestinationResolver must not be null");

		this.clientInboundChannel = clientInChannel;
		this.clientOutboundChannel = clientOutChannel;
		this.brokerChannel = brokerChannel;
		this.brokerMessagingTemplate = new SimpMessagingTemplate(brokerChannel);
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
	public MessageSendingOperations<String> getBrokerMessagingTemplate() {
		return this.brokerMessagingTemplate;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
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

		Set<String> destinations = this.userDestinationResolver.resolveDestination(message);
		if (CollectionUtils.isEmpty(destinations)) {
			return;
		}

		for (String targetDestination : destinations) {
			if (logger.isDebugEnabled()) {
				logger.debug("Sending message to resolved destination=" + targetDestination);
			}
			this.brokerMessagingTemplate.send(targetDestination, message);
		}
	}

}
