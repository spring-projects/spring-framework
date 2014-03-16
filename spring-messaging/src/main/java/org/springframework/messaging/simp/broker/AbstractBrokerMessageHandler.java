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

package org.springframework.messaging.simp.broker;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.CollectionUtils;


/**
 * Abstract base class for a {@link MessageHandler} that broker messages to
 * registered subscribers.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractBrokerMessageHandler
		implements MessageHandler, SmartLifecycle, ApplicationEventPublisherAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Collection<String> destinationPrefixes;

	private ApplicationEventPublisher eventPublisher;

	private AtomicBoolean brokerAvailable = new AtomicBoolean(false);

	private boolean autoStartup = true;

	private Object lifecycleMonitor = new Object();

	private volatile boolean running = false;


	public AbstractBrokerMessageHandler() {
		this(Collections.<String>emptyList());
	}

	public AbstractBrokerMessageHandler(Collection<String> destinationPrefixes) {
		destinationPrefixes = (destinationPrefixes != null) ? destinationPrefixes : Collections.<String>emptyList();
		this.destinationPrefixes = Collections.unmodifiableCollection(destinationPrefixes);
	}


	public Collection<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.eventPublisher = publisher;
	}

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
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Check whether this message handler is currently running.
	 *
	 * <p>Note that even when this message handler is running the
	 * {@link #isBrokerAvailable()} flag may still independently alternate between
	 * being on and off depending on the concrete sub-class implementation.
	 */
	@Override
	public final boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	/**
	 * Whether the message broker is currently available and able to process messages.
	 *
	 * <p>Note that this is in addition to the {@link #isRunning()} flag, which
	 * indicates whether this message handler is running. In other words the message
	 * handler must first be running and then the {@link #isBrokerAvailable()} flag
	 * may still independently alternate between being on and off depending on the
	 * concrete sub-class implementation.
	 *
	 * <p>Application components may implement
	 * {@link org.springframework.context.ApplicationListener<BrokerAvailabilityEvent>>}
	 * to receive notifications when broker becomes available and unavailable.
	 */
	public boolean isBrokerAvailable() {
		return this.brokerAvailable.get();
	}

	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			if (logger.isDebugEnabled()) {
				logger.debug("Starting " + getClass().getSimpleName());
			}
			startInternal();
			this.running = true;
		}
	}

	protected void startInternal() {
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			if (logger.isDebugEnabled()) {
				logger.debug("Stopping " + getClass().getSimpleName());
			}
			stopInternal();
			this.running = false;
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

	@Override
	public final void handleMessage(Message<?> message) {
		if (!this.running) {
			if (logger.isTraceEnabled()) {
				logger.trace("Message broker is not running. Ignoring message id=" + message.getHeaders().getId());
			}
			return;
		}
		handleMessageInternal(message);
	}

	protected abstract void handleMessageInternal(Message<?> message);

	protected boolean checkDestinationPrefix(String destination) {
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

	protected void publishBrokerAvailableEvent() {
		boolean shouldPublish = this.brokerAvailable.compareAndSet(false, true);
		if (this.eventPublisher != null && shouldPublish) {
			if (logger.isDebugEnabled()) {
				logger.debug("Publishing BrokerAvailabilityEvent (available)");
			}
			this.eventPublisher.publishEvent(new BrokerAvailabilityEvent(true, this));
		}
	}

	protected void publishBrokerUnavailableEvent() {
		boolean shouldPublish = this.brokerAvailable.compareAndSet(true, false);
		if (this.eventPublisher != null && shouldPublish) {
			if (logger.isDebugEnabled()) {
				logger.debug("Publishing BrokerAvailabilityEvent (unavailable)");
			}
			this.eventPublisher.publishEvent(new BrokerAvailabilityEvent(false, this));
		}
	}

}
