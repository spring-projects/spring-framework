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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.BrokerAvailabilityEvent;
import org.springframework.util.CollectionUtils;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractBrokerMessageHandler
		implements MessageHandler, SmartLifecycle, ApplicationEventPublisherAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private Collection<String> destinationPrefixes;

	private ApplicationEventPublisher eventPublisher;

	private AtomicBoolean brokerAvailable = new AtomicBoolean(false);

	private boolean autoStartup = true;

	private Object lifecycleMonitor = new Object();

	private volatile boolean running = false;


	public AbstractBrokerMessageHandler(Collection<String> destinationPrefixes) {
		this.destinationPrefixes = (destinationPrefixes != null)
				? destinationPrefixes : Collections.<String>emptyList();
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

	@Override
	public final boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
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
				logger.trace("STOMP broker relay not running. Ignoring message id=" + message.getHeaders().getId());
			}
			return;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Processing message: " + message);
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
		if ((this.eventPublisher != null) && this.brokerAvailable.compareAndSet(false, true)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Publishing BrokerAvailabilityEvent (available)");
			}
			this.eventPublisher.publishEvent(new BrokerAvailabilityEvent(true, this));
		}
	}

	protected void publishBrokerUnavailableEvent() {
		if ((this.eventPublisher != null) && this.brokerAvailable.compareAndSet(true, false)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Publishing BrokerAvailabilityEvent (unavailable)");
			}
			this.eventPublisher.publishEvent(new BrokerAvailabilityEvent(false, this));
		}
	}

}
