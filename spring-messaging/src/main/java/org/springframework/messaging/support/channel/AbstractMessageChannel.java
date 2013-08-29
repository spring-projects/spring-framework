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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;


/**
 * Abstract base class for {@link MessageChannel} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractMessageChannel implements MessageChannel, BeanNameAware  {

	protected Log logger = LogFactory.getLog(getClass());

	private String beanName;

	private final ChannelInterceptorChain interceptorChain = new ChannelInterceptorChain();


	public AbstractMessageChannel() {
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

	/**
	 * Set the list of channel interceptors. This will clear any existing interceptors.
	 */
	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		this.interceptorChain.set(interceptors);
	}

	/**
	 * Add a channel interceptor to the end of the list.
	 */
	public void addInterceptor(ChannelInterceptor interceptor) {
		this.interceptorChain.add(interceptor);
	}

	/**
	 * Return a read-only list of the configured interceptors.
	 */
	public List<ChannelInterceptor> getInterceptors() {
		return this.interceptorChain.getInterceptors();
	}

	/**
	 * Exposes the interceptor list for subclasses.
	 */
	protected ChannelInterceptorChain getInterceptorChain() {
		return this.interceptorChain;
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

		message = this.interceptorChain.preSend(message, this);
		if (message == null) {
			return false;
		}

		try {
			boolean sent = sendInternal(message, timeout);
			this.interceptorChain.postSend(message, this, sent);
			return sent;
		}
		catch (Exception e) {
			if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			throw new MessageDeliveryException(message,
					"Failed to send message to channel '" + this.getBeanName() + "'", e);
		}
	}

	protected abstract boolean sendInternal(Message<?> message, long timeout);

}
