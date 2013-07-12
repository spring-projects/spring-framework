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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

/**
 * A {@link SubscribableChannel} that sends messages to each of its subscribers.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class PublishSubscribeChannel implements SubscribableChannel {

	private final Executor executor;

	private final Set<MessageHandler> handlers = new CopyOnWriteArraySet<MessageHandler>();


	/**
	 * Create a new {@link PublishSubscribeChannel} instance where messages will be sent
	 * in the callers thread.
	 */
	public PublishSubscribeChannel() {
		this(null);
	}

	/**
	 * Create a new {@link PublishSubscribeChannel} instance where messages will be sent
	 * via the specified executor.
	 * @param executor the executor used to send the message or {@code null} to execute in
	 *        the callers thread.
	 */
	public PublishSubscribeChannel(Executor executor) {
		this.executor = executor;
	}

	@Override
	public boolean send(Message<?> message) {
		return send(message, INDEFINITE_TIMEOUT);
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		Assert.notNull(message, "Message must not be null");
		Assert.notNull(message.getPayload(), "Message payload must not be null");
		for (final MessageHandler handler : this.handlers) {
			dispatchToHandler(message, handler);
		}
		return true;
	}

	private void dispatchToHandler(final Message<?> message, final MessageHandler handler) {
		if (this.executor == null) {
			handler.handleMessage(message);
		}
		else {
			this.executor.execute(new Runnable() {
				@Override
				public void run() {
					handler.handleMessage(message);
				}
			});
		}
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		return this.handlers.add(handler);
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		return this.handlers.remove(handler);
	}

}
