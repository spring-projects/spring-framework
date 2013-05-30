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

package org.springframework.web.messaging.stomp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompHeaders;
import org.springframework.web.messaging.stomp.StompMessage;

import reactor.Fn;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.Registration;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleStompService {

	private static final Log logger = LogFactory.getLog(SimpleStompService.class);

	private final Reactor reactor;

	private Map<String, List<Registration<?>>> subscriptionsBySession = new ConcurrentHashMap<String, List<Registration<?>>>();


	public SimpleStompService(Reactor reactor) {
		this.reactor = reactor;
		this.reactor.on(Fn.$(StompCommand.SUBSCRIBE), new SubscribeConsumer());
		this.reactor.on(Fn.$(StompCommand.SEND), new SendConsumer());
		this.reactor.on(Fn.$(StompCommand.DISCONNECT), new DisconnectConsumer());
	}

	private void addSubscription(String sessionId, Registration<?> registration) {
		List<Registration<?>> list = this.subscriptionsBySession.get(sessionId);
		if (list == null) {
			list = new ArrayList<Registration<?>>();
			this.subscriptionsBySession.put(sessionId, list);
		}
		list.add(registration);
	}

	private void removeSubscriptions(String sessionId) {
		List<Registration<?>> registrations = this.subscriptionsBySession.remove(sessionId);
		if (logger.isTraceEnabled()) {
			logger.trace("Cancelling " + registrations.size() + " subscriptions for session=" + sessionId);
		}
		for (Registration<?> registration : registrations) {
			registration.cancel();
		}
	}


	private final class SubscribeConsumer implements Consumer<Event<StompMessage>> {

		@Override
		public void accept(Event<StompMessage> event) {

			StompMessage message = event.getData();

			if (logger.isDebugEnabled()) {
				logger.debug("Subscribe " + message);
			}

			Registration<?> registration = SimpleStompService.this.reactor.on(
					Fn.$("destination:" + message.getHeaders().getDestination()),
					new Consumer<Event<StompMessage>>() {
						@Override
						public void accept(Event<StompMessage> event) {
							StompMessage inMessage = event.getData();
							StompHeaders headers = new StompHeaders();
							headers.setDestination(inMessage.getHeaders().getDestination());
							StompMessage outMessage = new StompMessage(StompCommand.MESSAGE, headers, inMessage.getPayload());
							SimpleStompService.this.reactor.notify(event.getReplyTo(), Fn.event(outMessage));
						}
			});

			addSubscription(message.getStompSessionId(), registration);
		}
	}

	private final class SendConsumer implements Consumer<Event<StompMessage>> {

		@Override
		public void accept(Event<StompMessage> event) {
			StompMessage message = event.getData();
			logger.debug("Message received: " + message);

			String destination = message.getHeaders().getDestination();
			SimpleStompService.this.reactor.notify("destination:" + destination, Fn.event(message));
		}
	}

	private final class DisconnectConsumer implements Consumer<Event<String>> {

		@Override
		public void accept(Event<String> event) {
			String sessionId = event.getData();
			SimpleStompService.this.removeSubscriptions(sessionId);
		}
	}

}
