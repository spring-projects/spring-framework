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

package org.springframework.web.messaging.event;

import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.registry.Registration;
import reactor.fn.selector.ObjectSelector;


/**
 *
 */
public class ReactorEventBus implements EventBus {

	private final Reactor reactor;


	public ReactorEventBus(Reactor reactor) {
		this.reactor = reactor;
	}

	@Override
	public void send(String key, Object data) {
		this.reactor.notify(key, Event.wrap(data));
	}

	@Override
	public EventRegistration registerConsumer(final String key, final EventConsumer consumer) {

		ObjectSelector<String> selector = new ObjectSelector<String>(key);

		final Registration<Consumer<Event<Object>>> registration = this.reactor.on(selector,
				new Consumer<Event<Object>>() {
					@Override
					public void accept(Event<Object> event) {
						consumer.accept(event.getData());
					}
				});

		return new EventRegistration() {
			@Override
			public String getRegistrationKey() {
				return key;
			}
			@Override
			public void cancel() {
				registration.cancel();
			}
		};
	}

}
