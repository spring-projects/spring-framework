/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;
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
public abstract class AbstractMessageChannel implements MessageChannel, InterceptableChannel, BeanNameAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<ChannelInterceptor> interceptors = new ArrayList<>(5);

	private String beanName;


	public AbstractMessageChannel() {
		this.beanName = getClass().getSimpleName() + "@" + ObjectUtils.getIdentityHexString(this);
	}


	/**
	 * A message channel uses the bean name primarily for logging purposes.
	 */
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * Return the bean name for this message channel.
	 */
	public String getBeanName() {
		return this.beanName;
	}


	@Override
	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		this.interceptors.clear();
		this.interceptors.addAll(interceptors);
	}

	@Override
	public void addInterceptor(ChannelInterceptor interceptor) {
		this.interceptors.add(interceptor);
	}

	@Override
	public void addInterceptor(int index, ChannelInterceptor interceptor) {
		this.interceptors.add(index, interceptor);
	}

	@Override
	public List<ChannelInterceptor> getInterceptors() {
		return Collections.unmodifiableList(this.interceptors);
	}

	@Override
	public boolean removeInterceptor(ChannelInterceptor interceptor) {
		return this.interceptors.remove(interceptor);
	}

	@Override
	public ChannelInterceptor removeInterceptor(int index) {
		return this.interceptors.remove(index);
	}


	@Override
	public final boolean send(Message<?> message) {
		return send(message, INDEFINITE_TIMEOUT);
	}

	@Override
	public final boolean send(Message<?> message, long timeout) {
		Assert.notNull(message, "Message must not be null");
		Message<?> messageToUse = message;
		ChannelInterceptorChain chain = new ChannelInterceptorChain();
		boolean sent = false;
		try {
			messageToUse = chain.applyPreSend(messageToUse, this);
			if (messageToUse == null) {
				return false;
			}
			sent = sendInternal(messageToUse, timeout);
			chain.applyPostSend(messageToUse, this, sent);
			chain.triggerAfterSendCompletion(messageToUse, this, sent, null);
			return sent;
		}
		catch (Exception ex) {
			chain.triggerAfterSendCompletion(messageToUse, this, sent, ex);
			if (ex instanceof MessagingException) {
				throw (MessagingException) ex;
			}
			throw new MessageDeliveryException(messageToUse,"Failed to send message to " + this, ex);
		}
		catch (Throwable err) {
			MessageDeliveryException ex2 =
					new MessageDeliveryException(messageToUse, "Failed to send message to " + this, err);
			chain.triggerAfterSendCompletion(messageToUse, this, sent, ex2);
			throw ex2;
		}
	}

	protected abstract boolean sendInternal(Message<?> message, long timeout);


	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + this.beanName + "]";
	}


	/**
	 * Assists with the invocation of the configured channel interceptors.
	 */
	protected class ChannelInterceptorChain {

		private int sendInterceptorIndex = -1;

		private int receiveInterceptorIndex = -1;

		@Nullable
		public Message<?> applyPreSend(Message<?> message, MessageChannel channel) {
			Message<?> messageToUse = message;
			for (ChannelInterceptor interceptor : interceptors) {
				Message<?> resolvedMessage = interceptor.preSend(messageToUse, channel);
				if (resolvedMessage == null) {
					String name = interceptor.getClass().getSimpleName();
					if (logger.isDebugEnabled()) {
						logger.debug(name + " returned null from preSend, i.e. precluding the send.");
					}
					triggerAfterSendCompletion(messageToUse, channel, false, null);
					return null;
				}
				messageToUse = resolvedMessage;
				this.sendInterceptorIndex++;
			}
			return messageToUse;
		}

		public void applyPostSend(Message<?> message, MessageChannel channel, boolean sent) {
			for (ChannelInterceptor interceptor : interceptors) {
				interceptor.postSend(message, channel, sent);
			}
		}

		public void triggerAfterSendCompletion(Message<?> message, MessageChannel channel,
				boolean sent, @Nullable Exception ex) {

			for (int i = this.sendInterceptorIndex; i >= 0; i--) {
				ChannelInterceptor interceptor = interceptors.get(i);
				try {
					interceptor.afterSendCompletion(message, channel, sent, ex);
				}
				catch (Throwable ex2) {
					logger.error("Exception from afterSendCompletion in " + interceptor, ex2);
				}
			}
		}

		public boolean applyPreReceive(MessageChannel channel) {
			for (ChannelInterceptor interceptor : interceptors) {
				if (!interceptor.preReceive(channel)) {
					triggerAfterReceiveCompletion(null, channel, null);
					return false;
				}
				this.receiveInterceptorIndex++;
			}
			return true;
		}

		@Nullable
		public Message<?> applyPostReceive(Message<?> message, MessageChannel channel) {
			Message<?> messageToUse = message;
			for (ChannelInterceptor interceptor : interceptors) {
				messageToUse = interceptor.postReceive(messageToUse, channel);
				if (messageToUse == null) {
					return null;
				}
			}
			return messageToUse;
		}

		public void triggerAfterReceiveCompletion(
				@Nullable Message<?> message, MessageChannel channel, @Nullable Exception ex) {

			for (int i = this.receiveInterceptorIndex; i >= 0; i--) {
				ChannelInterceptor interceptor = interceptors.get(i);
				try {
					interceptor.afterReceiveCompletion(message, channel, ex);
				}
				catch (Throwable ex2) {
					if (logger.isErrorEnabled()) {
						logger.error("Exception from afterReceiveCompletion in " + interceptor, ex2);
					}
				}
			}
		}
	}

}
