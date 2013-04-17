/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.stomp.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.web.stomp.StompCommand;
import org.springframework.web.stomp.StompException;
import org.springframework.web.stomp.StompHeaders;
import org.springframework.web.stomp.StompMessage;
import org.springframework.web.stomp.StompSession;
import org.springframework.web.stomp.adapter.StompMessageProcessor;

import reactor.Fn;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.Registration;
import reactor.fn.Tuple;

/**
 * @author Gary Russell
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 */
public class ReactorServerStompMessageProcessor implements StompMessageProcessor {

	private static Log logger = LogFactory.getLog(ReactorServerStompMessageProcessor.class);


	private final Reactor reactor;

	private Map<String, List<Registration<?>>> subscriptionsBySession = new ConcurrentHashMap<String, List<Registration<?>>>();


	public ReactorServerStompMessageProcessor(Reactor reactor) {
		this.reactor = reactor;
	}

	public void processMessage(StompSession session, StompMessage message) throws IOException {

		StompCommand command = message.getCommand();
		Assert.notNull(command, "STOMP command not found");

		if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
			connect(session, message);
		}
		else if (StompCommand.SUBSCRIBE.equals(command)) {
			subscribe(session, message);
		}
		else if (StompCommand.UNSUBSCRIBE.equals(command)) {
			unsubscribe(session, message);
		}
		else if (StompCommand.SEND.equals(command)) {
			send(session, message);
		}
		else if (StompCommand.DISCONNECT.equals(command)) {
			disconnect(session);
		}
		else {
			throw new IllegalStateException("Unexpected command: " + command);
		}
	}

	protected void connect(StompSession session, StompMessage connectMessage) throws IOException {

		StompHeaders headers = new StompHeaders();
		Set<String> acceptVersions = connectMessage.getHeaders().getAcceptVersion();
		if (acceptVersions.contains("1.2")) {
			headers.setVersion("1.2");
		}
		else if (acceptVersions.contains("1.1")) {
			headers.setVersion("1.1");
		}
		else if (acceptVersions.isEmpty()) {
			// 1.0
		}
		else {
			throw new StompException("Unsupported version '" + acceptVersions + "'");
		}
		headers.setHeartbeat(0,0); // TODO
		headers.setId(session.getId());

		// TODO: security

		this.reactor.notify(StompCommand.CONNECT, Fn.event(session.getId()));

		session.sendMessage(new StompMessage(StompCommand.CONNECTED, headers));
	}

	protected void subscribe(final StompSession session, StompMessage message) {

		final String subscription = message.getHeaders().getId();
		String replyToKey = StompCommand.SUBSCRIBE + ":" + session.getId() + ":" + subscription;

		if (logger.isTraceEnabled()) {
			logger.trace("Adding subscription with replyToKey=" + replyToKey);
		}

		Registration<?> registration = this.reactor.on(Fn.$(replyToKey), new Consumer<Event<StompMessage>>() {
			@Override
			public void accept(Event<StompMessage> event) {
				event.getData().getHeaders().setSubscription(subscription);
				try {
					session.sendMessage(event.getData());
				}
				catch (IOException e) {
					// TODO: stomp error, close session, websocket close status
					ReactorServerStompMessageProcessor.this.removeSubscriptions(session.getId());
					e.printStackTrace();
				}
			}
		});

		addSubscription(session.getId(), registration);

		this.reactor.notify(StompCommand.SUBSCRIBE, Fn.event(Tuple.of(session.getId(), message), replyToKey));
	}

	private void addSubscription(String sessionId, Registration<?> registration) {
		List<Registration<?>> list = this.subscriptionsBySession.get(sessionId);
		if (list == null) {
			list = new ArrayList<Registration<?>>();
			this.subscriptionsBySession.put(sessionId, list);
		}
		list.add(registration);
	}

	protected void unsubscribe(StompSession session, StompMessage message) {
		this.reactor.notify(StompCommand.UNSUBSCRIBE, Fn.event(Tuple.of(session.getId(), message)));
	}

	protected void send(StompSession session, StompMessage message) {
		this.reactor.notify(StompCommand.SEND, Fn.event(Tuple.of(session.getId(), message)));
	}

	protected void disconnect(StompSession session) {
		String sessionId = session.getId();
		removeSubscriptions(sessionId);
		this.reactor.notify(StompCommand.DISCONNECT, Fn.event(sessionId));
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
