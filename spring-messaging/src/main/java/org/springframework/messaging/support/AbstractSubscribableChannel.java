/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.support;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

/**
 * Abstract base class for {@link SubscribableChannel} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSubscribableChannel extends AbstractMessageChannel implements SubscribableChannel {

	private final Set<MessageHandler> handlers = new CopyOnWriteArraySet<>();


	public Set<MessageHandler> getSubscribers() {
		return Collections.unmodifiableSet(this.handlers);
	}

	public boolean hasSubscription(MessageHandler handler) {
		return this.handlers.contains(handler);
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		boolean result = this.handlers.add(handler);
		if (result) {
			if (logger.isDebugEnabled()) {
				logger.debug(getBeanName() + " added " + handler);
			}
		}
		return result;
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		boolean result = this.handlers.remove(handler);
		if (result) {
			if (logger.isDebugEnabled()) {
				logger.debug(getBeanName() + " removed " + handler);
			}
		}
		return result;
	}

}
