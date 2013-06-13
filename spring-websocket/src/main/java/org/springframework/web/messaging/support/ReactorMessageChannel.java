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

package org.springframework.web.messaging.support;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.registry.Registration;
import reactor.fn.selector.ObjectSelector;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ReactorMessageChannel implements SubscribableChannel {

	private static Log logger = LogFactory.getLog(ReactorMessageChannel.class);

	private final Reactor reactor;

	private final Object key = new Object();

	private String name = toString(); // TODO


	private final Map<MessageHandler, Registration<?>> registrations =
			new HashMap<MessageHandler, Registration<?>>();


	public ReactorMessageChannel(Reactor reactor) {
		this.reactor = reactor;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean send(Message<?> message) {
		return send(message, -1);
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		if (logger.isTraceEnabled()) {
			logger.trace("Channel " + getName() + ", sending message id=" + message.getHeaders().getId());
		}
		this.reactor.notify(this.key, Event.wrap(message));
		return true;
	}

	@Override
	public boolean subscribe(final MessageHandler handler) {

		if (this.registrations.containsKey(handler)) {
			logger.warn("Channel " + getName() + ", handler already subscribed " + handler);
			return false;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Channel " + getName() + ", subscribing handler " + handler);
		}

		Registration<Consumer<Event<Message<?>>>> registration = this.reactor.on(
				ObjectSelector.objectSelector(key), new MessageHandlerConsumer(handler));

		this.registrations.put(handler, registration);

		return true;
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {

		if (logger.isTraceEnabled()) {
			logger.trace("Channel " + getName() + ", removing subscription for handler " + handler);
		}

		Registration<?> registration = this.registrations.get(handler);
		if (registration == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Channel " + getName() + ", no subscription for handler " + handler);
			}
			return false;
		}

		registration.cancel();
		return true;
	}


	private static final class MessageHandlerConsumer implements Consumer<Event<Message<?>>> {

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
				// TODO
				logger.error("Failed to process message " + message, t);
			}
		}
	}

}
