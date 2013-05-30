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
package org.springframework.web.messaging.stomp.socket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompException;
import org.springframework.web.messaging.stomp.StompHeaders;
import org.springframework.web.messaging.stomp.StompMessage;
import org.springframework.web.messaging.stomp.StompSession;

import reactor.Fn;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.Registration;

/**
 * @author Gary Russell
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultStompWebSocketHandler extends AbstractStompWebSocketHandler {

	private static Log logger = LogFactory.getLog(DefaultStompWebSocketHandler.class);


	private final Reactor reactor;

	private Map<String, List<Registration<?>>> registrationsBySession =
			new ConcurrentHashMap<String, List<Registration<?>>>();


	public DefaultStompWebSocketHandler(Reactor reactor) {
		this.reactor = reactor;
	}

	public void handleStompMessage(StompSession session, StompMessage message) {
		try {
			StompCommand command = message.getCommand();
			if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
				registerConnectionClosedCallback(session);
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
				disconnect(session, message);
			}
			else if (StompCommand.ACK.equals(command) || StompCommand.NACK.equals(command)) {
				this.reactor.notify(command, Fn.event(message));
			}
			else if (StompCommand.BEGIN.equals(command) || StompCommand.COMMIT.equals(command) || StompCommand.ABORT.equals(command)) {
				this.reactor.notify(command, Fn.event(message));
			}
			else {
				sendErrorMessage(session, "Invalid STOMP command " + command);
			}
		}
		catch (Throwable t) {
			handleError(session, t);
		}
	}

	private void registerConnectionClosedCallback(final StompSession session) {
		session.registerConnectionClosedCallback(new Runnable() {
			@Override
			public void run() {
				removeSubscriptions(session);
				reactor.notify("CONNECTION_CLOSED", Fn.event(session.getId()));
			}
		});
	}

	private void handleError(final StompSession session, Throwable t) {
		logger.error("Terminating STOMP session due to failure to send message: ", t);
		sendErrorMessage(session, t.getMessage());
		if (removeSubscriptions(session)) {
			// TODO: send error event including exception info
		}
	}

	private void sendErrorMessage(StompSession session, String errorText) {
		StompHeaders headers = new StompHeaders();
		headers.setMessage(errorText);
		StompMessage errorMessage = new StompMessage(StompCommand.ERROR, headers);
		try {
			session.sendMessage(errorMessage);
		}
		catch (Throwable t) {
			// ignore
		}
	}

	protected void connect(final StompSession session, StompMessage stompMessage) throws IOException {

		StompHeaders headers = new StompHeaders();
		Set<String> acceptVersions = stompMessage.getHeaders().getAcceptVersion();
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

		session.sendMessage(new StompMessage(StompCommand.CONNECTED, headers));

		String replyToKey = "relay-message" + session.getId();

		Registration<?> registration = this.reactor.on(Fn.$(replyToKey), new Consumer<Event<StompMessage>>() {
			@Override
			public void accept(Event<StompMessage> event) {
				try {
					StompMessage message = event.getData();
					if (StompCommand.CONNECTED.equals(message.getCommand())) {
						// TODO: skip for now (we already sent CONNECTED)
						return;
					}
					if (logger.isTraceEnabled()) {
						logger.trace("Relaying back to client: " + message);
					}
					session.sendMessage(message);
				}
				catch (Throwable t) {
					handleError(session, t);
				}
			}
		});

		addRegistration(session.getId(), registration);

		this.reactor.notify(StompCommand.CONNECT, Fn.event(stompMessage, replyToKey));
	}

	protected void subscribe(final StompSession session, StompMessage message) {

		final String subscriptionId = message.getHeaders().getId();
		String replyToKey = getSubscriptionReplyKey(session, subscriptionId);

		// TODO: extract and remember "ack" mode
		// http://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE_ack_Header

		if (logger.isTraceEnabled()) {
			logger.trace("Adding subscription, key=" + replyToKey);
		}

		Registration<?> registration = this.reactor.on(Fn.$(replyToKey), new Consumer<Event<StompMessage>>() {
			@Override
			public void accept(Event<StompMessage> event) {
				event.getData().getHeaders().setSubscription(subscriptionId);
				try {
					session.sendMessage(event.getData());
				}
				catch (Throwable t) {
					handleError(session, t);
				}
			}
		});

		addRegistration(session.getId(), registration);

		this.reactor.notify(StompCommand.SUBSCRIBE, Fn.event(message, replyToKey));

		// TODO: need a way to communicate back if subscription was successfully created or
		// not in which case an ERROR should be sent back and close the connection
		// http://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE
	}

	private String getSubscriptionReplyKey(StompSession session, String subscriptionId) {
		return StompCommand.SUBSCRIBE + ":" + session.getId() + ":" + subscriptionId;
	}

	private void addRegistration(String sessionId, Registration<?> registration) {
		List<Registration<?>> list = this.registrationsBySession.get(sessionId);
		if (list == null) {
			list = new ArrayList<Registration<?>>();
			this.registrationsBySession.put(sessionId, list);
		}
		list.add(registration);
	}

	protected void unsubscribe(StompSession session, StompMessage message) {
		cancelRegistration(session, message.getHeaders().getId());
		this.reactor.notify(StompCommand.UNSUBSCRIBE, Fn.event(message));
	}

	private void cancelRegistration(StompSession session, String subscriptionId) {
		String key = getSubscriptionReplyKey(session, subscriptionId);
		List<Registration<?>> list = this.registrationsBySession.get(session.getId());
		for (Registration<?> registration : list) {
			if (registration.getSelector().matches(key)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cancelling subscription, key=" + key);
				}
				list.remove(registration);
				registration.cancel();
			}
		}
	}

	protected void send(StompSession session, StompMessage stompMessage) {
		this.reactor.notify(StompCommand.SEND, Fn.event(stompMessage));
	}

	protected void disconnect(StompSession session, StompMessage stompMessage) {
		removeSubscriptions(session);
		this.reactor.notify(StompCommand.DISCONNECT, Fn.event(stompMessage));
	}

	private boolean removeSubscriptions(StompSession session) {
		String sessionId = session.getId();
		List<Registration<?>> registrations = this.registrationsBySession.remove(sessionId);
		if (CollectionUtils.isEmpty(registrations)) {
			return false;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Cancelling " + registrations.size() + " subscriptions for session=" + sessionId);
		}
		for (Registration<?> registration : registrations) {
			registration.cancel();
		}
		return true;
	}

}
