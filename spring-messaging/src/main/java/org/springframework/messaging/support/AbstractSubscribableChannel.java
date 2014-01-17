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

package org.springframework.messaging.support;

import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

/**
 * Abstract base class for {@link SubscribableChannel} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSubscribableChannel extends AbstractMessageChannel implements SubscribableChannel {

	@Override
	public final boolean subscribe(MessageHandler handler) {
		boolean result = subscribeInternal(handler);
		if (result) {
			if (logger.isDebugEnabled()) {
				logger.debug("[" + getBeanName() + "] subscribed " + handler);
			}
		}
		return result;
	}

	@Override
	public final boolean unsubscribe(MessageHandler handler) {
		boolean result = unsubscribeInternal(handler);
		if (result) {
			if (logger.isDebugEnabled()) {
				logger.debug("[" + getBeanName() + "] unsubscribed " + handler);
			}
		}
		return result;
	}


	/**
	 * Whether the given {@link MessageHandler} is already subscribed.
	 */
	public abstract boolean hasSubscription(MessageHandler handler);

	/**
	 * Subscribe the given {@link MessageHandler}.
	 */
	protected abstract boolean subscribeInternal(MessageHandler handler);

	/**
	 * Unsubscribe the given {@link MessageHandler}.
	 */
	protected abstract boolean unsubscribeInternal(MessageHandler handler);

}
