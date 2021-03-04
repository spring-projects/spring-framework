/*
 * Copyright 2002-2016 the original author or authors.
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
		return Collections.<MessageHandler>unmodifiableSet(this.handlers);
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
