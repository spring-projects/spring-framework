/*
 * Copyright 2002-2018 the original author or authors.
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
 * Submit messages to an {@link ExecutorSubscribableChannel}, one at a time.
 * The channel must have been configured with {@link #configureOutboundChannel}.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
class OrderedMessageSender implements MessageChannel {

	static final String COMPLETION_TASK_HEADER = "simpSendCompletionTask";


	private final MessageChannel channel;

	private final Log logger;

	private final Queue<Message<?>> messages = new ConcurrentLinkedQueue<>();

	private final AtomicBoolean sendInProgress = new AtomicBoolean(false);


	public OrderedMessageSender(MessageChannel channel, Log logger) {
		this.channel = channel;
		this.logger = logger;
	}


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
			Message<?> message = this.messages.poll();
			if (message != null) {
				try {
					addCompletionCallback(message);
					if (this.channel.send(message)) {
						return;
					}
				}
				catch (Throwable ex) {
					if (logger.isErrorEnabled()) {
						logger.error("Failed to send " + message, ex);
					}
				}
			}
			else {
				// We ran out of messages..
				this.sendInProgress.set(false);
				trySend();
				break;
			}
		}
	}

	private void addCompletionCallback(Message<?> msg) {
		SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(msg, SimpMessageHeaderAccessor.class);
		Assert.isTrue(accessor != null && accessor.isMutable(), "Expected mutable SimpMessageHeaderAccessor");
		accessor.setHeader(COMPLETION_TASK_HEADER, (Runnable) this::sendNextMessage);
	}


	/**
	 * Install or remove an {@link ExecutorChannelInterceptor} that invokes a
	 * completion task once the message is handled.
	 * @param channel the channel to configure
	 * @param preservePublishOrder whether preserve order is on or off based on
	 * which an interceptor is either added or removed.
	 */
	static void configureOutboundChannel(MessageChannel channel, boolean preservePublishOrder) {
		if (preservePublishOrder) {
			Assert.isInstanceOf(ExecutorSubscribableChannel.class, channel,
					"An ExecutorSubscribableChannel is required for `preservePublishOrder`");
			ExecutorSubscribableChannel execChannel = (ExecutorSubscribableChannel) channel;
			if (execChannel.getInterceptors().stream().noneMatch(i -> i instanceof CallbackInterceptor)) {
				execChannel.addInterceptor(0, new CallbackInterceptor());
			}
		}
		else if (channel instanceof ExecutorSubscribableChannel) {
			ExecutorSubscribableChannel execChannel = (ExecutorSubscribableChannel) channel;
			execChannel.getInterceptors().stream().filter(i -> i instanceof CallbackInterceptor)
					.findFirst()
					.map(execChannel::removeInterceptor);

		}
	}


	private static class CallbackInterceptor implements ExecutorChannelInterceptor {

		@Override
		public void afterMessageHandled(
				Message<?> msg, MessageChannel ch, MessageHandler handler, @Nullable Exception ex) {

			Runnable task = (Runnable) msg.getHeaders().get(OrderedMessageSender.COMPLETION_TASK_HEADER);
			if (task != null) {
				task.run();
			}
		}
	}

}
