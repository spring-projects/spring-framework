/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.simp.broker;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Abstract base class for a {@link MessageHandler} that broker messages to
 * registered subscribers.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractBrokerMessageHandler
		implements MessageHandler, ApplicationEventPublisherAware, SmartLifecycle {

	protected final Log logger = SimpLogging.forLogName(getClass());

	private final SubscribableChannel clientInboundChannel;

	private final MessageChannel clientOutboundChannel;

	private final SubscribableChannel brokerChannel;

	private final Collection<String> destinationPrefixes;

	@Nullable
	private Predicate<String> userDestinationPredicate;

	private boolean preservePublishOrder = false;

	@Nullable
	private ApplicationEventPublisher eventPublisher;

	private AtomicBoolean brokerAvailable = new AtomicBoolean();

	private final BrokerAvailabilityEvent availableEvent = new BrokerAvailabilityEvent(true, this);

	private final BrokerAvailabilityEvent notAvailableEvent = new BrokerAvailabilityEvent(false, this);

	private boolean autoStartup = true;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();

	private final ChannelInterceptor unsentDisconnectInterceptor = new UnsentDisconnectChannelInterceptor();


	/**
	 * Constructor with no destination prefixes (matches all destinations).
	 * @param inboundChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param outboundChannel the channel for sending messages to clients (e.g. WebSocket clients)
	 * @param brokerChannel the channel for the application to send messages to the broker
	 */
	public AbstractBrokerMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel) {

		this(inboundChannel, outboundChannel, brokerChannel, Collections.emptyList());
	}

	/**
	 * Constructor with destination prefixes to match to destinations of messages.
	 * @param inboundChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param outboundChannel the channel for sending messages to clients (e.g. WebSocket clients)
	 * @param brokerChannel the channel for the application to send messages to the broker
	 * @param destinationPrefixes prefixes to use to filter out messages
	 */
	public AbstractBrokerMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel, @Nullable Collection<String> destinationPrefixes) {

		Assert.notNull(inboundChannel, "'inboundChannel' must not be null");
		Assert.notNull(outboundChannel, "'outboundChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");

		this.clientInboundChannel = inboundChannel;
		this.clientOutboundChannel = outboundChannel;
		this.brokerChannel = brokerChannel;

		destinationPrefixes = (destinationPrefixes != null ? destinationPrefixes : Collections.emptyList());
		this.destinationPrefixes = Collections.unmodifiableCollection(destinationPrefixes);
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

	/**
	 * Return destination prefixes prefixes to use to filter messages to forward
	 * to the broker. Messages that have a destination and where the destination
	 * doesn't match are ignored.
	 * <p>By default this is not set.
	 */
	public Collection<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}

	/**
	 * Configure a Predicate to identify messages with a user destination. When
	 * no {@link #getDestinationPrefixes() destination prefixes} are configured,
	 * this helps to recognize and skip user destination messages that need to
	 * be pre-processed by the
	 * {@link org.springframework.messaging.simp.user.UserDestinationMessageHandler}
	 * before they reach the broker.
	 * @param predicate the predicate to identify user messages with a non-null
	 * destination as messages with a user destinations.
	 * @since 5.3.4
	 */
	public void setUserDestinationPredicate(@Nullable Predicate<String> predicate) {
		this.userDestinationPredicate = predicate;
	}

	/**
	 * Whether the client must receive messages in the order of publication.
	 * <p>By default messages sent to the {@code "clientOutboundChannel"} may
	 * not be processed in the same order because the channel is backed by a
	 * ThreadPoolExecutor that in turn does not guarantee processing in order.
	 * <p>When this flag is set to {@code true} messages within the same session
	 * will be sent to the {@code "clientOutboundChannel"} one at a time in
	 * order to preserve the order of publication. Enable this only if needed
	 * since there is some performance overhead to keep messages in order.
	 * @param preservePublishOrder whether to publish in order
	 * @since 5.1
	 */
	public void setPreservePublishOrder(boolean preservePublishOrder) {
		OrderedMessageChannelDecorator.configureInterceptor(this.clientOutboundChannel, preservePublishOrder);
		this.preservePublishOrder = preservePublishOrder;
	}

	/**
	 * Whether to ensure messages are received in the order of publication.
	 * @since 5.1
	 */
	public boolean isPreservePublishOrder() {
		return this.preservePublishOrder;
	}

	@Override
	public void setApplicationEventPublisher(@Nullable ApplicationEventPublisher publisher) {
		this.eventPublisher = publisher;
	}

	@Nullable
	public ApplicationEventPublisher getApplicationEventPublisher() {
		return this.eventPublisher;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}


	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			logger.info("Starting...");
			this.clientInboundChannel.subscribe(this);
			this.brokerChannel.subscribe(this);
			if (this.clientInboundChannel instanceof InterceptableChannel) {
				((InterceptableChannel) this.clientInboundChannel).addInterceptor(0, this.unsentDisconnectInterceptor);
			}
			startInternal();
			this.running = true;
			logger.info("Started.");
		}
	}

	protected void startInternal() {
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			logger.info("Stopping...");
			stopInternal();
			this.clientInboundChannel.unsubscribe(this);
			this.brokerChannel.unsubscribe(this);
			if (this.clientInboundChannel instanceof InterceptableChannel) {
				((InterceptableChannel) this.clientInboundChannel).removeInterceptor(this.unsentDisconnectInterceptor);
			}
			this.running = false;
			logger.info("Stopped.");
		}
	}

	protected void stopInternal() {
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	/**
	 * Check whether this message handler is currently running.
	 * <p>Note that even when this message handler is running the
	 * {@link #isBrokerAvailable()} flag may still independently alternate between
	 * being on and off depending on the concrete sub-class implementation.
	 */
	@Override
	public final boolean isRunning() {
		return this.running;
	}

	/**
	 * Whether the message broker is currently available and able to process messages.
	 * <p>Note that this is in addition to the {@link #isRunning()} flag, which
	 * indicates whether this message handler is running. In other words the message
	 * handler must first be running and then the {@code #isBrokerAvailable()} flag
	 * may still independently alternate between being on and off depending on the
	 * concrete sub-class implementation.
	 * <p>Application components may implement
	 * {@code org.springframework.context.ApplicationListener&lt;BrokerAvailabilityEvent&gt;}
	 * to receive notifications when broker becomes available and unavailable.
	 */
	public boolean isBrokerAvailable() {
		return this.brokerAvailable.get();
	}


	@Override
	public void handleMessage(Message<?> message) {
		if (!this.running) {
			if (logger.isTraceEnabled()) {
				logger.trace(this + " not running yet. Ignoring " + message);
			}
			return;
		}
		handleMessageInternal(message);
	}

	protected abstract void handleMessageInternal(Message<?> message);


	/**
	 * Whether a message with the given destination should be processed. This is
	 * the case if one of the following conditions is true:
	 * <ol>
	 * <li>The destination starts with one of the configured
	 * {@link #getDestinationPrefixes() destination prefixes}.
	 * <li>No prefixes are configured and the destination isn't matched
	 * by the {@link #setUserDestinationPredicate(Predicate)
	 * userDestinationPredicate}.
	 * <li>The message has no destination.
	 * </ol>
	 * @param destination the destination to check
	 * @return whether to process (true) or skip (false) the destination
	 */
	protected boolean checkDestinationPrefix(@Nullable String destination) {
		if (destination == null) {
			return true;
		}
		if (CollectionUtils.isEmpty(this.destinationPrefixes)) {
			return !isUserDestination(destination);
		}
		for (String prefix : this.destinationPrefixes) {
			if (destination.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private boolean isUserDestination(String destination) {
		return (this.userDestinationPredicate != null && this.userDestinationPredicate.test(destination));
	}

	protected void publishBrokerAvailableEvent() {
		boolean shouldPublish = this.brokerAvailable.compareAndSet(false, true);
		if (this.eventPublisher != null && shouldPublish) {
			if (logger.isInfoEnabled()) {
				logger.info(this.availableEvent);
			}
			this.eventPublisher.publishEvent(this.availableEvent);
		}
	}

	protected void publishBrokerUnavailableEvent() {
		boolean shouldPublish = this.brokerAvailable.compareAndSet(true, false);
		if (this.eventPublisher != null && shouldPublish) {
			if (logger.isInfoEnabled()) {
				logger.info(this.notAvailableEvent);
			}
			this.eventPublisher.publishEvent(this.notAvailableEvent);
		}
	}

	/**
	 * Get the MessageChannel to use for sending messages to clients, possibly
	 * a per-session wrapper when {@code preservePublishOrder=true}.
	 * @since 5.1
	 */
	protected MessageChannel getClientOutboundChannelForSession(String sessionId) {
		return this.preservePublishOrder ?
				new OrderedMessageChannelDecorator(getClientOutboundChannel(), logger) : getClientOutboundChannel();
	}


	/**
	 * Detect unsent DISCONNECT messages and process them anyway.
	 */
	private class UnsentDisconnectChannelInterceptor implements ChannelInterceptor {

		@Override
		public void afterSendCompletion(
				Message<?> message, MessageChannel channel, boolean sent, @Nullable Exception ex) {

			if (!sent) {
				SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
				if (SimpMessageType.DISCONNECT.equals(messageType)) {
					logger.debug("Detected unsent DISCONNECT message. Processing anyway.");
					handleMessage(message);
				}
			}
		}
	}

}
