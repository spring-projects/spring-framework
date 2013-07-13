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

package org.springframework.messaging.support.channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;


/**
 * Abstract base class for {@link SubscribableChannel} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSubscribableChannel implements SubscribableChannel, BeanNameAware  {

	protected Log logger = LogFactory.getLog(getClass());

	private String beanName;


	public AbstractSubscribableChannel() {
		this.beanName = getClass().getSimpleName() + "@" + ObjectUtils.getIdentityHexString(this);
	}

	/**
	 * {@inheritDoc}
	 * <p>Used primarily for logging purposes.
	 */
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * @return the name for this channel.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	@Override
	public final boolean send(Message<?> message) {
		return send(message, INDEFINITE_TIMEOUT);
	}

	@Override
	public final boolean send(Message<?> message, long timeout) {
		Assert.notNull(message, "Message must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("[" + this.beanName + "] sending message " + message);
		}
		return sendInternal(message, timeout);
	}

	protected abstract boolean sendInternal(Message<?> message, long timeout);

	@Override
	public final boolean subscribe(MessageHandler handler) {
		if (hasSubscription(handler)) {
			logger.warn("[" + this.beanName + "] handler already subscribed " + handler);
			return false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.beanName + "] subscribing " + handler);
		}
		return subscribeInternal(handler);
	}

	protected abstract boolean hasSubscription(MessageHandler handler);

	protected abstract boolean subscribeInternal(MessageHandler handler);

	@Override
	public final boolean unsubscribe(MessageHandler handler) {
		if (logger.isDebugEnabled()) {
			logger.debug("[" + this.beanName + "] unsubscribing " + handler);
		}
		return unsubscribeInternal(handler);
	}

	protected abstract boolean unsubscribeInternal(MessageHandler handler);

}
