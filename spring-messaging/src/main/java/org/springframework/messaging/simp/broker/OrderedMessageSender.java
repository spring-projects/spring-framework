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

package org.springframework.messaging.simp.broker;

import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
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
 * {@code MessageChannel} decorator that ensures messages from the same session
 * are sent and processed in the same order. This would not normally be the case
 * with an {@code Executor} backed {@code MessageChannel} since the executor
 * is free to submit tasks in any order.
 *
 * <p>To provide ordering, inbound messages are placed in a queue and sent one
 * one at a time per session. Once a message is processed, a callback is used to
 * notify that the next message from the same session can be sent through.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
class OrderedMessageSender implements MessageChannel {

	private static final CompletionTaskInterceptor COMPLETION_INTERCEPTOR = new CompletionTaskInterceptor();


	private final MessageChannel channel;

	private final Log logger;

	private final Control control = new Control();


	public OrderedMessageSender(MessageChannel channel, Log logger) {
		this.channel = channel;
		this.logger = logger;
	}


	@Override
	public boolean send(Message<?> message) {
		return send(message, -1);
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		this.control.addMessage(message);
		trySend();
		return true;
	}

	private void trySend() {
		if (this.control.acquireSendLock()) {
			sendMessages();
		}
	}

	private void sendMessages() {
		for ( ; ; ) {
			Set<String> skipSet = new HashSet<>();
			for (Message<?> message : this.control.getMessagesToSend()) {
				String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
				Assert.notNull(sessionId, () -> "No session id in " + message.getHeaders());
				if (skipSet.contains(sessionId)) {
					continue;
				}
				if (!this.control.acquireSessionLock(sessionId)) {
					skipSet.add(sessionId);
					continue;
				}
				this.control.removeMessage(message);
				try {
					CompletionTaskInterceptor.instrumentMessage(message, () -> {
						this.control.releaseSessionLock(sessionId);
						if (this.control.hasRemainingWork()) {
							trySend();
						}
					});
					if (this.channel.send(message)) {
						continue;
					}
				}
				catch (Throwable ex) {
					if (logger.isErrorEnabled()) {
						logger.error("Failed to send " + message, ex);
					}
				}
				// We didn't send
				this.control.releaseSessionLock(sessionId);
			}

			if (this.control.shouldYield()) {
				this.control.releaseSendLock();
				if (!this.control.shouldYield()) {
					trySend();
				}
				return;
			}
		}
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
			ExecutorSubscribableChannel executorChannel = (ExecutorSubscribableChannel) channel;
			if (executorChannel.getInterceptors().stream().noneMatch(i -> i == COMPLETION_INTERCEPTOR)) {
				executorChannel.addInterceptor(0, COMPLETION_INTERCEPTOR);
			}
		}
		else if (channel instanceof ExecutorSubscribableChannel) {
			ExecutorSubscribableChannel execChannel = (ExecutorSubscribableChannel) channel;
			execChannel.getInterceptors().stream().filter(i -> i == COMPLETION_INTERCEPTOR)
					.findFirst()
					.map(execChannel::removeInterceptor);

		}
	}


	/**
	 * Provides locks required for ordered message sending and execution within
	 * a session as well as storage for messages waiting to be sent.
	 */
	private static class Control {

		private final Queue<Message<?>> messages = new ConcurrentLinkedQueue<>();

		private final ConcurrentMap<String, Boolean> sessionsInProgress = new ConcurrentHashMap<>();

		private final AtomicBoolean workInProgress = new AtomicBoolean(false);


		public void addMessage(Message<?> message) {
			this.messages.add(message);
		}

		public void removeMessage(Message<?> message) {
			if (!this.messages.remove(message)) {
				throw new IllegalStateException(
						"Message " + message.getHeaders() + " was expected in the queue.");
			}
		}

		public Collection<Message<?>> getMessagesToSend() {
			return this.messages;
		}

		public boolean acquireSendLock() {
			return this.workInProgress.compareAndSet(false, true);
		}

		public void releaseSendLock() {
			this.workInProgress.set(false);
		}

		public boolean acquireSessionLock(String sessionId) {
			return (this.sessionsInProgress.put(sessionId, Boolean.TRUE) == null);
		}

		public void releaseSessionLock(String sessionId) {
			this.sessionsInProgress.remove(sessionId);
		}

		public boolean hasRemainingWork() {
			return !this.messages.isEmpty();
		}

		public boolean shouldYield() {
			// No remaining work, or others can pick it up
			return (!hasRemainingWork() || this.sessionsInProgress.size() > 0);
		}
	}


	private static class CompletionTaskInterceptor implements ExecutorChannelInterceptor {

		private static final String COMPLETION_TASK_HEADER = "simpSendCompletionTask";

		@Override
		public void afterMessageHandled(
				Message<?> message, MessageChannel ch, MessageHandler handler, @Nullable Exception ex) {

			Runnable task = (Runnable) message.getHeaders().get(COMPLETION_TASK_HEADER);
			if (task != null) {
				task.run();
			}
		}

		public static void instrumentMessage(Message<?> message, Runnable task) {
			SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
			Assert.isTrue(accessor != null && accessor.isMutable(), "Expected a mutable SimpMessageHeaderAccessor");
			accessor.setHeader(COMPLETION_TASK_HEADER, task);
		}
	}

}
