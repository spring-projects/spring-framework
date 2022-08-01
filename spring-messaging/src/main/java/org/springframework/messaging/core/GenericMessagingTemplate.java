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

package org.springframework.messaging.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * A messaging template that resolves destinations names to {@link MessageChannel}'s
 * to send and receive messages from.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Gary Russell
 * @since 4.0
 */
public class GenericMessagingTemplate extends AbstractDestinationResolvingMessagingTemplate<MessageChannel>
		implements BeanFactoryAware {

	/**
	 * The default header key used for a send timeout.
	 */
	public static final String DEFAULT_SEND_TIMEOUT_HEADER = "sendTimeout";

	/**
	 * The default header key used for a receive timeout.
	 */
	public static final String DEFAULT_RECEIVE_TIMEOUT_HEADER = "receiveTimeout";

	private volatile long sendTimeout = -1;

	private volatile long receiveTimeout = -1;

	private String sendTimeoutHeader = DEFAULT_SEND_TIMEOUT_HEADER;

	private String receiveTimeoutHeader = DEFAULT_RECEIVE_TIMEOUT_HEADER;

	private volatile boolean throwExceptionOnLateReply;


	/**
	 * Configure the default timeout value to use for send operations.
	 * May be overridden for individual messages.
	 * @param sendTimeout the send timeout in milliseconds
	 * @see #setSendTimeoutHeader(String)
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	/**
	 * Return the configured default send operation timeout value.
	 */
	public long getSendTimeout() {
		return this.sendTimeout;
	}

	/**
	 * Configure the default timeout value to use for receive operations.
	 * May be overridden for individual messages when using sendAndReceive
	 * operations.
	 * @param receiveTimeout the receive timeout in milliseconds
	 * @see #setReceiveTimeoutHeader(String)
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Return the configured receive operation timeout value.
	 */
	public long getReceiveTimeout() {
		return this.receiveTimeout;
	}

	/**
	 * Set the name of the header used to determine the send timeout (if present).
	 * Default {@value #DEFAULT_SEND_TIMEOUT_HEADER}.
	 * <p>The header is removed before sending the message to avoid propagation.
	 * @since 5.0
	 */
	public void setSendTimeoutHeader(String sendTimeoutHeader) {
		Assert.notNull(sendTimeoutHeader, "'sendTimeoutHeader' cannot be null");
		this.sendTimeoutHeader = sendTimeoutHeader;
	}

	/**
	 * Return the configured send-timeout header.
	 * @since 5.0
	 */
	public String getSendTimeoutHeader() {
		return this.sendTimeoutHeader;
	}

	/**
	 * Set the name of the header used to determine the send timeout (if present).
	 * Default {@value #DEFAULT_RECEIVE_TIMEOUT_HEADER}.
	 * The header is removed before sending the message to avoid propagation.
	 * @since 5.0
	 */
	public void setReceiveTimeoutHeader(String receiveTimeoutHeader) {
		Assert.notNull(receiveTimeoutHeader, "'receiveTimeoutHeader' cannot be null");
		this.receiveTimeoutHeader = receiveTimeoutHeader;
	}

	/**
	 * Return the configured receive-timeout header.
	 * @since 5.0
	 */
	public String getReceiveTimeoutHeader() {
		return this.receiveTimeoutHeader;
	}

	/**
	 * Whether the thread sending a reply should have an exception raised if the
	 * receiving thread isn't going to receive the reply either because it timed out,
	 * or because it already received a reply, or because it got an exception while
	 * sending the request message.
	 * <p>The default value is {@code false} in which case only a WARN message is logged.
	 * If set to {@code true} a {@link MessageDeliveryException} is raised in addition
	 * to the log message.
	 * @param throwExceptionOnLateReply whether to throw an exception or not
	 */
	public void setThrowExceptionOnLateReply(boolean throwExceptionOnLateReply) {
		this.throwExceptionOnLateReply = throwExceptionOnLateReply;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		setDestinationResolver(new BeanFactoryMessageChannelDestinationResolver(beanFactory));
	}


	@Override
	protected final void doSend(MessageChannel channel, Message<?> message) {
		doSend(channel, message, sendTimeout(message));
	}

	protected final void doSend(MessageChannel channel, Message<?> message, long timeout) {
		Assert.notNull(channel, "MessageChannel is required");

		Message<?> messageToSend = message;
		MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
		if (accessor != null && accessor.isMutable()) {
			accessor.removeHeader(this.sendTimeoutHeader);
			accessor.removeHeader(this.receiveTimeoutHeader);
			accessor.setImmutable();
		}
		else if (message.getHeaders().containsKey(this.sendTimeoutHeader)
				|| message.getHeaders().containsKey(this.receiveTimeoutHeader)) {
			messageToSend = MessageBuilder.fromMessage(message)
					.setHeader(this.sendTimeoutHeader, null)
					.setHeader(this.receiveTimeoutHeader, null)
					.build();
		}

		boolean sent = (timeout >= 0 ? channel.send(messageToSend, timeout) : channel.send(messageToSend));

		if (!sent) {
			throw new MessageDeliveryException(message,
					"Failed to send message to channel '" + channel + "' within timeout: " + timeout);
		}
	}

	@Override
	@Nullable
	protected final Message<?> doReceive(MessageChannel channel) {
		return doReceive(channel, this.receiveTimeout);
	}

	@Nullable
	protected final Message<?> doReceive(MessageChannel channel, long timeout) {
		Assert.notNull(channel, "MessageChannel is required");
		Assert.state(channel instanceof PollableChannel, "A PollableChannel is required to receive messages");

		Message<?> message = (timeout >= 0 ?
				((PollableChannel) channel).receive(timeout) : ((PollableChannel) channel).receive());

		if (message == null && logger.isTraceEnabled()) {
			logger.trace("Failed to receive message from channel '" + channel + "' within timeout: " + timeout);
		}

		return message;
	}

	@Override
	@Nullable
	protected final Message<?> doSendAndReceive(MessageChannel channel, Message<?> requestMessage) {
		Assert.notNull(channel, "'channel' is required");
		Object originalReplyChannelHeader = requestMessage.getHeaders().getReplyChannel();
		Object originalErrorChannelHeader = requestMessage.getHeaders().getErrorChannel();

		long sendTimeout = sendTimeout(requestMessage);
		long receiveTimeout = receiveTimeout(requestMessage);

		TemporaryReplyChannel tempReplyChannel = new TemporaryReplyChannel(this.throwExceptionOnLateReply);
		requestMessage = MessageBuilder.fromMessage(requestMessage).setReplyChannel(tempReplyChannel)
				.setHeader(this.sendTimeoutHeader, null)
				.setHeader(this.receiveTimeoutHeader, null)
				.setErrorChannel(tempReplyChannel).build();

		try {
			doSend(channel, requestMessage, sendTimeout);
		}
		catch (RuntimeException ex) {
			tempReplyChannel.setSendFailed(true);
			throw ex;
		}

		Message<?> replyMessage = this.doReceive(tempReplyChannel, receiveTimeout);
		if (replyMessage != null) {
			replyMessage = MessageBuilder.fromMessage(replyMessage)
					.setHeader(MessageHeaders.REPLY_CHANNEL, originalReplyChannelHeader)
					.setHeader(MessageHeaders.ERROR_CHANNEL, originalErrorChannelHeader)
					.build();
		}

		return replyMessage;
	}

	private long sendTimeout(Message<?> requestMessage) {
		Long sendTimeout = headerToLong(requestMessage.getHeaders().get(this.sendTimeoutHeader));
		return (sendTimeout != null ? sendTimeout : this.sendTimeout);
	}

	private long receiveTimeout(Message<?> requestMessage) {
		Long receiveTimeout = headerToLong(requestMessage.getHeaders().get(this.receiveTimeoutHeader));
		return (receiveTimeout != null ? receiveTimeout : this.receiveTimeout);
	}

	@Nullable
	private Long headerToLong(@Nullable Object headerValue) {
		if (headerValue instanceof Number) {
			return ((Number) headerValue).longValue();
		}
		else if (headerValue instanceof String) {
			return Long.parseLong((String) headerValue);
		}
		else {
			return null;
		}
	}


	/**
	 * A temporary channel for receiving a single reply message.
	 */
	private static final class TemporaryReplyChannel implements PollableChannel {

		private final Log logger = LogFactory.getLog(TemporaryReplyChannel.class);

		private final CountDownLatch replyLatch = new CountDownLatch(1);

		private final boolean throwExceptionOnLateReply;

		@Nullable
		private volatile Message<?> replyMessage;

		private volatile boolean hasReceived;

		private volatile boolean hasTimedOut;

		private volatile boolean hasSendFailed;

		TemporaryReplyChannel(boolean throwExceptionOnLateReply) {
			this.throwExceptionOnLateReply = throwExceptionOnLateReply;
		}

		public void setSendFailed(boolean hasSendError) {
			this.hasSendFailed = hasSendError;
		}

		@Override
		@Nullable
		public Message<?> receive() {
			return this.receive(-1);
		}

		@Override
		@Nullable
		public Message<?> receive(long timeout) {
			try {
				if (timeout < 0) {
					this.replyLatch.await();
					this.hasReceived = true;
				}
				else {
					if (this.replyLatch.await(timeout, TimeUnit.MILLISECONDS)) {
						this.hasReceived = true;
					}
					else {
						this.hasTimedOut = true;
					}
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return this.replyMessage;
		}

		@Override
		public boolean send(Message<?> message) {
			return this.send(message, -1);
		}

		@Override
		public boolean send(Message<?> message, long timeout) {
			this.replyMessage = message;
			boolean alreadyReceivedReply = this.hasReceived;
			this.replyLatch.countDown();

			String errorDescription = null;
			if (this.hasTimedOut) {
				errorDescription = "Reply message received but the receiving thread has exited due to a timeout";
			}
			else if (alreadyReceivedReply) {
				errorDescription = "Reply message received but the receiving thread has already received a reply";
			}
			else if (this.hasSendFailed) {
				errorDescription = "Reply message received but the receiving thread has exited due to " +
						"an exception while sending the request message";
			}

			if (errorDescription != null) {
				if (logger.isWarnEnabled()) {
					logger.warn(errorDescription + ": " + message);
				}
				if (this.throwExceptionOnLateReply) {
					throw new MessageDeliveryException(message, errorDescription);
				}
			}

			return true;
		}
	}

}
