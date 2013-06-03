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

import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompHeaders;
import org.springframework.web.messaging.stomp.StompMessage;

import reactor.Fn;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.registry.Registration;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleStompService extends AbstractStompService {

	private Map<String, List<Registration<?>>> subscriptionsBySession =
			new ConcurrentHashMap<String, List<Registration<?>>>();


	public SimpleStompService(Reactor reactor) {
		super(reactor);
	}


	@Override
	protected void processSubscribe(StompMessage message, final Object replyTo) {
		if (logger.isDebugEnabled()) {
			logger.debug("Subscribe " + message);
		}
		Registration<?> registration = getReactor().on(
				Fn.$("destination:" + message.getHeaders().getDestination()),
				new Consumer<Event<StompMessage>>() {
					@Override
					public void accept(Event<StompMessage> sendEvent) {
						StompMessage inMessage = sendEvent.getData();
						StompHeaders headers = new StompHeaders();
						headers.setDestination(inMessage.getHeaders().getDestination());
						StompMessage outMessage = new StompMessage(StompCommand.MESSAGE, headers, inMessage.getPayload());
						getReactor().notify(replyTo, Event.wrap(outMessage));
					}
		});
		addSubscription(message.getSessionId(), registration);
	}

	private void addSubscription(String sessionId, Registration<?> registration) {
		List<Registration<?>> list = this.subscriptionsBySession.get(sessionId);
		if (list == null) {
			list = new ArrayList<Registration<?>>();
			this.subscriptionsBySession.put(sessionId, list);
		}
		list.add(registration);
	}

	@Override
	protected void processSend(StompMessage message) {
		logger.debug("Message received: " + message);
		String destination = message.getHeaders().getDestination();
		getReactor().notify("destination:" + destination, Event.wrap(message));
	}

	@Override
	protected void processDisconnect(StompMessage message) {
		removeSubscriptions(message.getSessionId());
	}

	@Override
	protected void processConnectionClosed(String sessionId) {
		removeSubscriptions(sessionId);
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

}
