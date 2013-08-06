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

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.event.registry.Registration;
import reactor.event.selector.ObjectSelector;
import reactor.event.selector.Selector;
import reactor.function.Consumer;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ReactorSubscribableChannel extends AbstractSubscribableChannel {

	private final Reactor reactor;

	private final Object key = new Object();

	private final Map<MessageHandler, Registration<?>> registrations = new HashMap<MessageHandler, Registration<?>>();


	public ReactorSubscribableChannel(Reactor reactor) {
		this.reactor = reactor;
	}


	@Override
	protected boolean hasSubscription(MessageHandler handler) {
		return this.registrations.containsKey(handler);
	}

	@Override
	public boolean sendInternal(Message<?> message, long timeout) {
		this.reactor.notify(this.key, Event.wrap(message));
		return true;
	}

	@Override
	public boolean subscribeInternal(final MessageHandler handler) {
		Selector selector = ObjectSelector.objectSelector(this.key);
		MessageHandlerConsumer consumer = new MessageHandlerConsumer(handler);
		Registration<Consumer<Event<Message<?>>>> registration = this.reactor.on(selector, consumer);
		this.registrations.put(handler, registration);
		return true;
	}

	@Override
	public boolean unsubscribeInternal(MessageHandler handler) {
		Registration<?> registration = this.registrations.remove(handler);
		if (registration != null) {
			registration.cancel();
			return true;
		}
		return false;
	}


	private final class MessageHandlerConsumer implements Consumer<Event<Message<?>>> {

		private final MessageHandler handler;

		private MessageHandlerConsumer(MessageHandler handler) {
			this.handler = handler;
		}

		@Override
		public void accept(Event<Message<?>> event) {
			Message<?> message = event.getData();
			try {
				this.handler.handleMessage(message);
			}
			catch (Throwable t) {
				logger.error("Failed to process message " + message, t);
			}
		}
	}
}
