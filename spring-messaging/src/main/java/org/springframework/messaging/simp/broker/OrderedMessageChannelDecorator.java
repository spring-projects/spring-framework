/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.messaging.simp.broker;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * Decorator for an {@link ExecutorSubscribableChannel} that ensures messages
 * are processed in the order they were published to the channel. Messages are
 * sent one at a time with the next one released when the previous has been
 * processed. This decorator is intended to be applied per session.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class OrderedMessageChannelDecorator implements MessageChannel {

	private static final String NEXT_MESSAGE_TASK_HEADER = "simpNextMessageTask";


	private final MessageChannel channel;

	private final int subscriberCount;

	private final Log logger;

	private final Queue<Message<?>> messages = new ConcurrentLinkedQueue<>();

	private final AtomicBoolean sendInProgress = new AtomicBoolean();


	public OrderedMessageChannelDecorator(MessageChannel channel, Log logger) {
		this.channel = channel;
		this.subscriberCount = (channel instanceof ExecutorSubscribableChannel ch ? ch.getSubscribers().size() : 0);
		this.logger = logger;
	}


	@Override
	public boolean send(Message<?> message) {
		return send(message, -1);
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		this.messages.add(message);
		trySend();
		return true;
	}

	private void trySend() {
		// Take sendInProgress flag only if queue is not empty
		if (this.messages.isEmpty()) {
			return;
		}

		if (this.sendInProgress.compareAndSet(false, true)) {
			sendNextMessage();
		}
	}

	private void sendNextMessage() {
		for (;;) {
			Message<?> message = this.messages.peek();
			if (message != null) {
				try {
					setTaskHeader(message, new PostHandleTask(message));
					if (this.channel.send(message)) {
						return;
					}
				}
				catch (Throwable ex) {
					if (logger.isErrorEnabled()) {
						logger.error("Failed to send " + message, ex);
					}
				}
				removeMessage(message);
			}
			else {
				this.sendInProgress.set(false);
				trySend();
				break;
			}
		}
	}

	/**
	 * Remove the message from the top of the queue, but only if it matches,
	 * i.e. hasn't been removed already.
	 */
	private boolean removeMessage(Message<?> message) {
		// Remove only if not removed already
		Message<?> next = this.messages.peek();
		if (next == message) {
			this.messages.remove();
			return true;
		}
		else {
			return false;
		}
	}

	private static void setTaskHeader(Message<?> message, Runnable task) {
		SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
		Assert.isTrue(accessor != null && accessor.isMutable(), "Expected mutable SimpMessageHeaderAccessor");
		accessor.setHeader(NEXT_MESSAGE_TASK_HEADER, task);
	}


	/**
	 * Install or remove an {@link ExecutorChannelInterceptor} that invokes a
	 * completion task, if found in the headers of the message.
	 * @param channel the channel to configure
	 * @param preserveOrder whether preserve the order or publication; when
	 * "true" an interceptor is inserted, when "false" it removed.
	 */
	public static void configureInterceptor(MessageChannel channel, boolean preserveOrder) {
		if (preserveOrder) {
			Assert.isInstanceOf(ExecutorSubscribableChannel.class, channel,
					"An ExecutorSubscribableChannel is required for 'preservePublishOrder'");
			ExecutorSubscribableChannel execChannel = (ExecutorSubscribableChannel) channel;
			if (execChannel.getInterceptors().stream().noneMatch(CallbackTaskInterceptor.class::isInstance)) {
				execChannel.addInterceptor(0, new CallbackTaskInterceptor());
			}
		}
		else if (channel instanceof ExecutorSubscribableChannel execChannel) {
			execChannel.getInterceptors().stream().filter(CallbackTaskInterceptor.class::isInstance)
					.findFirst().map(execChannel::removeInterceptor);

		}
	}

	/**
	 * Whether the channel has been {@link #configureInterceptor configured}
	 * with an interceptor for sequential handling.
	 * @since 6.1
	 */
	public static boolean supportsOrderedMessages(MessageChannel channel) {
		return (channel instanceof ExecutorSubscribableChannel ch &&
				ch.getInterceptors().stream().anyMatch(CallbackTaskInterceptor.class::isInstance));
	}

	/**
	 * Obtain the task to release the next message, if found.
	 */
	@Nullable
	public static Runnable getNextMessageTask(Message<?> message) {
		return (Runnable) message.getHeaders().get(OrderedMessageChannelDecorator.NEXT_MESSAGE_TASK_HEADER);
	}


	/**
	 * Remove handled message from queue, and send next message.
	 */
	private final class PostHandleTask implements Runnable {

		private final Message<?> message;

		@Nullable
		private final AtomicInteger handledCount;

		private PostHandleTask(Message<?> message) {
			this.message = message;
			this.handledCount = (subscriberCount > 1 ? new AtomicInteger(0) : null);
		}

		@Override
		public void run() {
			if (this.handledCount == null || this.handledCount.addAndGet(1) == subscriberCount) {
				if (OrderedMessageChannelDecorator.this.removeMessage(this.message)) {
					sendNextMessage();
				}
			}
		}
	}


	private static final class CallbackTaskInterceptor implements ExecutorChannelInterceptor {

		@Override
		public void afterMessageHandled(
				Message<?> message, MessageChannel ch, MessageHandler handler, @Nullable Exception ex) {

			Runnable task = getNextMessageTask(message);
			if (task != null) {
				task.run();
			}
		}
	}

}
