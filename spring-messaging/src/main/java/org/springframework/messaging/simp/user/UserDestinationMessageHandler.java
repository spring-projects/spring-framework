/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.user;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;

import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.OrderedMessageChannelDecorator;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@code MessageHandler} with support for "user" destinations.
 *
 * <p>Listen for messages with "user" destinations, translate the destination to
 * a target destination that's unique to the active user session(s), and send
 * to the broker channel for delivery.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class UserDestinationMessageHandler implements MessageHandler, SmartLifecycle {

	private static final Log logger = SimpLogging.forLogName(UserDestinationMessageHandler.class);


	private final SubscribableChannel clientInboundChannel;

	private final SubscribableChannel brokerChannel;

	private final UserDestinationResolver destinationResolver;

	private final SendHelper sendHelper;

	@Nullable
	private BroadcastHandler broadcastHandler;

	@Nullable
	private MessageHeaderInitializer headerInitializer;

	private volatile boolean running;

	@Nullable
	private Integer phase;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Create an instance with the given client and broker channels to subscribe to,
	 * and then send resolved messages to the broker channel.
	 * @param clientInboundChannel messages received from clients.
	 * @param brokerChannel messages sent to the broker.
	 * @param destinationResolver the resolver for "user" destinations.
	 */
	public UserDestinationMessageHandler(
			SubscribableChannel clientInboundChannel, SubscribableChannel brokerChannel,
			UserDestinationResolver destinationResolver) {

		Assert.notNull(clientInboundChannel, "'clientInChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");
		Assert.notNull(destinationResolver, "resolver must not be null");

		this.clientInboundChannel = clientInboundChannel;
		this.brokerChannel = brokerChannel;
		this.sendHelper = new SendHelper(clientInboundChannel, brokerChannel);
		this.destinationResolver = destinationResolver;
	}


	/**
	 * Return the configured {@link UserDestinationResolver}.
	 */
	public UserDestinationResolver getUserDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * Set a destination to broadcast messages to that remain unresolved because
	 * the user is not connected. In a multi-application server scenario this
	 * gives other application servers a chance to try.
	 * <p>By default this is not set.
	 * @param destination the target destination.
	 */
	public void setBroadcastDestination(@Nullable String destination) {
		this.broadcastHandler = (StringUtils.hasText(destination) ?
				new BroadcastHandler(this.sendHelper.getMessagingTemplate(), destination) : null);
	}

	/**
	 * Return the configured destination for unresolved messages.
	 */
	@Nullable
	public String getBroadcastDestination() {
		return (this.broadcastHandler != null ? this.broadcastHandler.getBroadcastDestination() : null);
	}

	/**
	 * Return the messaging template used to send resolved messages to the
	 * broker channel.
	 */
	public MessageSendingOperations<String> getBrokerMessagingTemplate() {
		return this.sendHelper.getMessagingTemplate();
	}

	/**
	 * Configure a custom {@link MessageHeaderInitializer} to initialize the
	 * headers of resolved target messages.
	 * <p>By default this is not set.
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * Return the configured header initializer.
	 */
	@Nullable
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}

	/**
	 * Set the phase that this handler should run in.
	 * <p>By default, this is {@link SmartLifecycle#DEFAULT_PHASE}, but with
	 * {@code @EnableWebSocketMessageBroker} configuration it is set to 0.
	 * @since 6.1.4
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public int getPhase() {
		return (this.phase != null ? this.phase : SmartLifecycle.super.getPhase());
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
	public final boolean isRunning() {
		return this.running;
	}


	@Override
	public void handleMessage(Message<?> sourceMessage) throws MessagingException {
		Message<?> message = sourceMessage;
		if (this.broadcastHandler != null) {
			message = this.broadcastHandler.preHandle(sourceMessage);
			if (message == null) {
				return;
			}
		}

		UserDestinationResult result = this.destinationResolver.resolveDestination(message);
		if (result == null) {
			this.sendHelper.checkDisconnect(message);
			return;
		}

		if (result.getTargetDestinations().isEmpty()) {
			if (logger.isTraceEnabled()) {
				logger.trace("No active sessions for user destination: " + result.getSourceDestination());
			}
			if (this.broadcastHandler != null) {
				this.broadcastHandler.handleUnresolved(message);
			}
			return;
		}

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
		initHeaders(accessor);
		accessor.setNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, result.getSubscribeDestination());
		accessor.setLeaveMutable(true);

		message = MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
		if (logger.isTraceEnabled()) {
			logger.trace("Translated " + result.getSourceDestination() + " -> " + result.getTargetDestinations());
		}

		this.sendHelper.send(result, message);
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


	private static class SendHelper {

		private final MessageChannel brokerChannel;

		private final MessageSendingOperations<String> messagingTemplate;

		@Nullable
		private final Map<String, MessageSendingOperations<String>> orderedMessagingTemplates;

		SendHelper(MessageChannel clientInboundChannel, MessageChannel brokerChannel) {
			this.brokerChannel = brokerChannel;
			this.messagingTemplate = new SimpMessagingTemplate(brokerChannel);
			if (OrderedMessageChannelDecorator.supportsOrderedMessages(clientInboundChannel)) {
				this.orderedMessagingTemplates = new ConcurrentHashMap<>();
				OrderedMessageChannelDecorator.configureInterceptor(brokerChannel, true);
			}
			else {
				this.orderedMessagingTemplates = null;
			}
		}

		public MessageSendingOperations<String> getMessagingTemplate() {
			return this.messagingTemplate;
		}

		public void send(UserDestinationResult destinationResult, Message<?> message) throws MessagingException {
			Set<String> sessionIds = destinationResult.getSessionIds();
			Iterator<String> itr = (sessionIds != null ? sessionIds.iterator() : null);

			for (String target : destinationResult.getTargetDestinations()) {
				String sessionId = (itr != null ? itr.next() : null);
				getTemplateToUse(sessionId).send(target, message);
			}
		}

		private MessageSendingOperations<String> getTemplateToUse(@Nullable String sessionId) {
			if (this.orderedMessagingTemplates != null && sessionId != null) {
				return this.orderedMessagingTemplates.computeIfAbsent(sessionId, id ->
						new SimpMessagingTemplate(new OrderedMessageChannelDecorator(this.brokerChannel, logger)));
			}
			return this.messagingTemplate;
		}

		public void checkDisconnect(Message<?> message) {
			if (this.orderedMessagingTemplates != null) {
				MessageHeaders headers = message.getHeaders();
				if (SimpMessageHeaderAccessor.getMessageType(headers) == SimpMessageType.DISCONNECT) {
					String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
					if (sessionId != null) {
						this.orderedMessagingTemplates.remove(sessionId);
					}
				}
			}
		}
	}


	/**
	 * A handler that broadcasts locally unresolved messages to the broker and
	 * also handles similar broadcasts received from the broker.
	 */
	private static class BroadcastHandler {

		private static final List<String> NO_COPY_LIST = Arrays.asList("subscription", "message-id");

		private final MessageSendingOperations<String> messagingTemplate;

		private final String broadcastDestination;

		public BroadcastHandler(MessageSendingOperations<String> template, String destination) {
			this.messagingTemplate = template;
			this.broadcastDestination = destination;
		}

		public String getBroadcastDestination() {
			return this.broadcastDestination;
		}

		@Nullable
		public Message<?> preHandle(Message<?> message) throws MessagingException {
			String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
			if (!getBroadcastDestination().equals(destination)) {
				return message;
			}
			SimpMessageHeaderAccessor accessor =
					MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
			Assert.state(accessor != null, "No SimpMessageHeaderAccessor");
			if (accessor.getSessionId() == null) {
				// Our own broadcast
				return null;
			}
			destination = accessor.getFirstNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION);
			if (logger.isTraceEnabled()) {
				logger.trace("Checking unresolved user destination: " + destination);
			}
			SimpMessageHeaderAccessor newAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
			for (String name : accessor.toNativeHeaderMap().keySet()) {
				if (NO_COPY_LIST.contains(name)) {
					continue;
				}
				newAccessor.setNativeHeader(name, accessor.getFirstNativeHeader(name));
			}
			if (destination != null) {
				newAccessor.setDestination(destination);
			}
			newAccessor.setHeader(SimpMessageHeaderAccessor.IGNORE_ERROR, true); // ensure send doesn't block
			return MessageBuilder.createMessage(message.getPayload(), newAccessor.getMessageHeaders());
		}

		public void handleUnresolved(Message<?> message) {
			MessageHeaders headers = message.getHeaders();
			if (NativeMessageHeaderAccessor.getFirstNativeHeader(
					SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, headers) != null) {
				// Re-broadcast
				return;
			}
			SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
			String destination = accessor.getDestination();
			accessor.setNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, destination);
			accessor.setLeaveMutable(true);
			message = MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
			if (logger.isTraceEnabled()) {
				logger.trace("Translated " + destination + " -> " + getBroadcastDestination());
			}
			this.messagingTemplate.send(getBroadcastDestination(), message);
		}
	}

}
