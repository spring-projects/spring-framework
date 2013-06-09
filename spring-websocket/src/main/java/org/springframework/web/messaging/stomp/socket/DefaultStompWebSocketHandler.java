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
import org.springframework.http.MediaType;
import org.springframework.messaging.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.converter.CompositeMessageConverter;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.event.EventBus;
import org.springframework.web.messaging.event.EventConsumer;
import org.springframework.web.messaging.event.EventRegistration;
import org.springframework.web.messaging.service.AbstractMessageService;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompException;
import org.springframework.web.messaging.stomp.StompHeaders;
import org.springframework.web.messaging.stomp.StompMessage;
import org.springframework.web.messaging.stomp.StompSession;
import org.springframework.web.messaging.stomp.support.StompHeaderMapper;

/**
 * @author Gary Russell
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultStompWebSocketHandler extends AbstractStompWebSocketHandler {

	private static Log logger = LogFactory.getLog(DefaultStompWebSocketHandler.class);


	private final EventBus eventBus;

	private MessageConverter payloadConverter = new CompositeMessageConverter(null);

	private final StompHeaderMapper headerMapper = new StompHeaderMapper();

	private Map<String, List<EventRegistration>> registrationsBySession =
			new ConcurrentHashMap<String, List<EventRegistration>>();


	public DefaultStompWebSocketHandler(EventBus eventBus) {
		this.eventBus = eventBus;
	}


	public void setMessageConverters(List<MessageConverter> converters) {
		this.payloadConverter = new CompositeMessageConverter(converters);
	}

	public void handleStompMessage(final StompSession session, StompMessage stompMessage) {

		if (logger.isTraceEnabled()) {
			logger.trace("Processing: " + stompMessage);
		}

		try {
			MessageType messageType = MessageType.OTHER;
			String replyKey = null;

			StompCommand command = stompMessage.getCommand();
			if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
				session.registerConnectionClosedTask(new ConnectionClosedTask(session));
				messageType = MessageType.CONNECT;
				replyKey = handleConnect(session, stompMessage);
			}
			else if (StompCommand.SEND.equals(command)) {
				messageType = MessageType.MESSAGE;
				handleSend(session, stompMessage);
			}
			else if (StompCommand.SUBSCRIBE.equals(command)) {
				messageType = MessageType.SUBSCRIBE;
				replyKey = handleSubscribe(session, stompMessage);
			}
			else if (StompCommand.UNSUBSCRIBE.equals(command)) {
				messageType = MessageType.UNSUBSCRIBE;
				handleUnsubscribe(session, stompMessage);
			}
			else if (StompCommand.DISCONNECT.equals(command)) {
				messageType = MessageType.DISCONNECT;
				handleDisconnect(session, stompMessage);
			}
			else {
				sendErrorMessage(session, "Invalid STOMP command " + command);
				return;
			}

			Map<String, Object> messageHeaders = this.headerMapper.toMessageHeaders(stompMessage.getHeaders());
			messageHeaders.put("messageType", messageType);
			if (replyKey != null) {
				messageHeaders.put(MessageHeaders.REPLY_CHANNEL, replyKey);
			}
			messageHeaders.put("stompCommand", command);
			messageHeaders.put("sessionId", session.getId());

			Message<byte[]> genericMessage = new GenericMessage<byte[]>(stompMessage.getPayload(), messageHeaders);

			if (logger.isTraceEnabled()) {
				logger.trace("Sending notification: " + genericMessage);
			}
			this.eventBus.send(AbstractMessageService.MESSAGE_KEY, genericMessage);
		}
		catch (Throwable t) {
			handleError(session, t);
		}
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

	protected String handleConnect(final StompSession session, StompMessage stompMessage) throws IOException {

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

		String replyKey = "relay-message" + session.getId();

		EventRegistration registration = this.eventBus.registerConsumer(replyKey,
				new EventConsumer<StompMessage>() {
					@Override
					public void accept(StompMessage message) {
						try {
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
		addRegistration(session, registration);

		return replyKey;
	}

	protected String handleSubscribe(final StompSession session, StompMessage message) {

		final String subscriptionId = message.getHeaders().getId();
		String replyKey = getSubscriptionReplyKey(session, subscriptionId);

		// TODO: extract and remember "ack" mode
		// http://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE_ack_Header

		if (logger.isTraceEnabled()) {
			logger.trace("Adding subscription, key=" + replyKey);
		}

		EventRegistration registration = this.eventBus.registerConsumer(replyKey, new EventConsumer<Message<?>>() {
			@Override
			public void accept(Message<?> replyMessage) {

				StompHeaders headers = new StompHeaders();
				headers.setSubscription(subscriptionId);

				headerMapper.fromMessageHeaders(replyMessage.getHeaders(), headers);

				byte[] payload;
				try {
					MediaType contentType = headers.getContentType();
					payload = payloadConverter.convertToPayload(replyMessage.getPayload(), contentType);
				}
				catch (Exception e) {
					logger.error("Failed to send " + replyMessage, e);
					return;
				}

				try {
					StompMessage stompMessage = new StompMessage(StompCommand.MESSAGE, headers, payload);
					session.sendMessage(stompMessage);
				}
				catch (Throwable t) {
					handleError(session, t);
				}
			}
		});
		addRegistration(session, registration);

		return replyKey;

		// TODO: need a way to communicate back if subscription was successfully created or
		// not in which case an ERROR should be sent back and close the connection
		// http://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE
	}

	private String getSubscriptionReplyKey(StompSession session, String subscriptionId) {
		return StompCommand.SUBSCRIBE + ":" + session.getId() + ":" + subscriptionId;
	}

	private void addRegistration(StompSession session, EventRegistration registration) {
		String sessionId = session.getId();
		List<EventRegistration> list = this.registrationsBySession.get(sessionId);
		if (list == null) {
			list = new ArrayList<EventRegistration>();
			this.registrationsBySession.put(sessionId, list);
		}
		list.add(registration);
	}

	protected void handleUnsubscribe(StompSession session, StompMessage message) {
		cancelRegistration(session, message.getHeaders().getId());
	}

	private void cancelRegistration(StompSession session, String subscriptionId) {
		String key = getSubscriptionReplyKey(session, subscriptionId);
		List<EventRegistration> list = this.registrationsBySession.get(session.getId());
		for (EventRegistration registration : list) {
			if (registration.getRegistrationKey().equals(key)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cancelling subscription, key=" + key);
				}
				list.remove(registration);
				registration.cancel();
			}
		}
	}

	protected void handleSend(StompSession session, StompMessage stompMessage) {
	}

	protected void handleDisconnect(StompSession session, StompMessage stompMessage) {
		removeSubscriptions(session);
	}

	private boolean removeSubscriptions(StompSession session) {
		String sessionId = session.getId();
		List<EventRegistration> registrations = this.registrationsBySession.remove(sessionId);
		if (CollectionUtils.isEmpty(registrations)) {
			return false;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Cancelling " + registrations.size() + " subscriptions for session=" + sessionId);
		}
		for (EventRegistration registration : registrations) {
			registration.cancel();
		}
		return true;
	}


	private final class ConnectionClosedTask implements Runnable {

		private final StompSession session;

		private ConnectionClosedTask(StompSession session) {
			this.session = session;
		}

		@Override
		public void run() {
			removeSubscriptions(session);
			if (logger.isTraceEnabled()) {
				logger.trace("Sending notification for closed connection: " + session.getId());
			}
			eventBus.send(AbstractMessageService.CLIENT_CONNECTION_CLOSED_KEY, session.getId());
		}
	}

}
