/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.messaging.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;


/**
 * A messaging template for sending to and/or receiving messages from a
 * {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @since 4.0
 */
public class GenericMessagingTemplate extends AbstractDestinationResolvingMessagingTemplate<MessageChannel>
		implements BeanFactoryAware {

	private volatile long sendTimeout = -1;

	private volatile long receiveTimeout = -1;

	private volatile boolean throwExceptionOnLateReply = false;


	/**
	 * Specify the timeout value to use for send operations.
	 *
	 * @param sendTimeout the send timeout in milliseconds
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	/**
	 * Specify the timeout value to use for receive operations.
	 *
	 * @param receiveTimeout the receive timeout in milliseconds
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Specify whether or not an attempt to send on the reply channel throws an exception
	 * if no receiving thread will actually receive the reply. This can occur
	 * if the receiving thread has already timed out, or will never call receive()
	 * because it caught an exception, or has already received a reply.
	 * (default false - just a WARN log is emitted in these cases).
	 * @param throwExceptionOnLateReply TRUE or FALSE.
	 */
	public void setThrowExceptionOnLateReply(boolean throwExceptionOnLateReply) {
		this.throwExceptionOnLateReply = throwExceptionOnLateReply;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setDestinationResolver(new BeanFactoryMessageChannelDestinationResolver(beanFactory));
	}


	@Override
	protected final void doSend(MessageChannel destination, Message<?> message) {
		Assert.notNull(destination, "channel must not be null");
		long timeout = this.sendTimeout;
		boolean sent = (timeout >= 0)
				? destination.send(message, timeout)
				: destination.send(message);
		if (!sent) {
			throw new MessageDeliveryException(message,
					"failed to send message to channel '" + destination + "' within timeout: " + timeout);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected final <P> Message<P> doReceive(MessageChannel destination) {
		Assert.state(destination instanceof PollableChannel,
				"The 'destination' must be a PollableChannel for receive operations.");

		Assert.notNull(destination, "channel must not be null");
		long timeout = this.receiveTimeout;
		Message<?> message = (timeout >= 0)
				? ((PollableChannel) destination).receive(timeout)
				: ((PollableChannel) destination).receive();
		if (message == null && this.logger.isTraceEnabled()) {
			this.logger.trace("failed to receive message from channel '" + destination + "' within timeout: " + timeout);
		}
		return (Message<P>) message;
	}

	@Override
	protected final <S, R> Message<R> doSendAndReceive(MessageChannel destination, Message<S> requestMessage) {
		Object originalReplyChannelHeader = requestMessage.getHeaders().getReplyChannel();
		Object originalErrorChannelHeader = requestMessage.getHeaders().getErrorChannel();
		TemporaryReplyChannel replyChannel = new TemporaryReplyChannel(this.receiveTimeout, this.throwExceptionOnLateReply);
		requestMessage = MessageBuilder.fromMessage(requestMessage)
				.setReplyChannel(replyChannel)
				.setErrorChannel(replyChannel)
				.build();
		try {
			this.doSend(destination, requestMessage);
		}
		catch (RuntimeException e) {
			replyChannel.setClientWontReceive(true);
			throw e;
		}
		Message<R> reply = this.doReceive(replyChannel);
		if (reply != null) {
			reply = MessageBuilder.fromMessage(reply)
					.setHeader(MessageHeaders.REPLY_CHANNEL, originalReplyChannelHeader)
					.setHeader(MessageHeaders.ERROR_CHANNEL, originalErrorChannelHeader)
					.build();
		}
		return reply;
	}


	private static class TemporaryReplyChannel implements PollableChannel {

		private static final Log logger = LogFactory.getLog(TemporaryReplyChannel.class);

		private volatile Message<?> message;

		private final long receiveTimeout;

		private final CountDownLatch latch = new CountDownLatch(1);

		private final boolean throwExceptionOnLateReply;

		private volatile boolean clientTimedOut;

		private volatile boolean clientWontReceive;

		private volatile boolean clientHasReceived;


		public TemporaryReplyChannel(long receiveTimeout, boolean throwExceptionOnLateReply) {
			this.receiveTimeout = receiveTimeout;
			this.throwExceptionOnLateReply = throwExceptionOnLateReply;
		}

		public void setClientWontReceive(boolean clientWontReceive) {
			this.clientWontReceive = clientWontReceive;
		}


		@Override
		public Message<?> receive() {
			return this.receive(-1);
		}

		@Override
		public Message<?> receive(long timeout) {
			try {
				if (this.receiveTimeout < 0) {
					this.latch.await();
					this.clientHasReceived = true;
				}
				else {
					if (this.latch.await(this.receiveTimeout, TimeUnit.MILLISECONDS)) {
						this.clientHasReceived = true;
					}
					else {
						this.clientTimedOut = true;
					}
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return this.message;
		}

		@Override
		public boolean send(Message<?> message) {
			return this.send(message, -1);
		}

		@Override
		public boolean send(Message<?> message, long timeout) {
			this.message = message;
			this.latch.countDown();
			if (this.clientTimedOut || this.clientHasReceived || this.clientWontReceive) {
				String exceptionMessage = "";
				if (this.clientTimedOut) {
					exceptionMessage = "Reply message being sent, but the receiving thread has already timed out";
				}
				else if (this.clientHasReceived) {
					exceptionMessage = "Reply message being sent, but the receiving thread has already received a reply";
				}
				else if (this.clientWontReceive) {
					exceptionMessage = "Reply message being sent, but the receiving thread has already caught an exception and won't receive";
				}

				if (logger.isWarnEnabled()) {
					logger.warn(exceptionMessage + ":" + message);
				}
				if (this.throwExceptionOnLateReply) {
					throw new MessageDeliveryException(message, exceptionMessage);
				}
			}
			return true;
		}
	}

}
