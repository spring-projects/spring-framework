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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

/**
 * A {@link SubscribableChannel} that sends messages to each of its subscribers.
 *
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ExecutorSubscribableChannel extends AbstractSubscribableChannel {

	private final Executor executor;


	/**
	 * Create a new {@link ExecutorSubscribableChannel} instance where messages will be sent
	 * in the callers thread.
	 */
	public ExecutorSubscribableChannel() {
		this(null);
	}

	/**
	 * Create a new {@link ExecutorSubscribableChannel} instance where messages will be sent
	 * via the specified executor.
	 * @param executor the executor used to send the message or {@code null} to execute in
	 *        the callers thread.
	 */
	public ExecutorSubscribableChannel(Executor executor) {
		this.executor = executor;
	}


	public Executor getExecutor() {
		return this.executor;
	}

	@Override
	public boolean sendInternal(final Message<?> message, long timeout) {
		for (final MessageHandler handler : getSubscribers()) {
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
		return true;
	}

}
