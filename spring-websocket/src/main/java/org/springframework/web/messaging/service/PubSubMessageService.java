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

package org.springframework.web.messaging.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.web.messaging.PubSubHeaders;
import org.springframework.web.messaging.converter.CompositeMessageConverter;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.event.EventBus;
import org.springframework.web.messaging.event.EventConsumer;
import org.springframework.web.messaging.event.EventRegistration;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class PubSubMessageService extends AbstractMessageService {

	private MessageConverter payloadConverter;

	private Map<String, List<EventRegistration>> subscriptionsBySession =
			new ConcurrentHashMap<String, List<EventRegistration>>();


	public PubSubMessageService(EventBus reactor) {
		super(reactor);
		this.payloadConverter = new CompositeMessageConverter(null);
	}


	public void setMessageConverters(List<MessageConverter> converters) {
		this.payloadConverter = new CompositeMessageConverter(converters);
	}

	@Override
	protected void processMessage(Message<?> message) {

		if (logger.isDebugEnabled()) {
			logger.debug("Message received: " + message);
		}

		try {
			// Convert to byte[] payload before the fan-out
			PubSubHeaders inHeaders = new PubSubHeaders(message.getHeaders(), true);
			byte[] payload = payloadConverter.convertToPayload(message.getPayload(), inHeaders.getContentType());
			message = new GenericMessage<byte[]>(payload, message.getHeaders());

			getEventBus().send(getPublishKey(inHeaders.getDestination()), message);
		}
		catch (Exception ex) {
			logger.error("Failed to publish " + message, ex);
		}
	}

	private String getPublishKey(String destination) {
		return "destination:" + destination;
	}

	@Override
	protected void processSubscribe(Message<?> message) {
		if (logger.isDebugEnabled()) {
			logger.debug("Subscribe " + message);
		}
		PubSubHeaders headers = new PubSubHeaders(message.getHeaders(), true);
		final String subscriptionId = headers.getSubscriptionId();
		EventRegistration registration = getEventBus().registerConsumer(getPublishKey(headers.getDestination()),
				new EventConsumer<Message<?>>() {
					@Override
					public void accept(Message<?> message) {
						PubSubHeaders inHeaders = new PubSubHeaders(message.getHeaders(), true);
						PubSubHeaders outHeaders = new PubSubHeaders();
						outHeaders.setDestinations(inHeaders.getDestinations());
						outHeaders.setContentType(inHeaders.getContentType());
						outHeaders.setSubscriptionId(subscriptionId);
						Object payload = message.getPayload();
						message = new GenericMessage<Object>(payload, outHeaders.getMessageHeaders());
						getEventBus().send(AbstractMessageService.SERVER_TO_CLIENT_MESSAGE_KEY, message);
					}
				});

		addSubscription((String) message.getHeaders().get("sessionId"), registration);
	}

	private void addSubscription(String sessionId, EventRegistration registration) {
		List<EventRegistration> list = this.subscriptionsBySession.get(sessionId);
		if (list == null) {
			list = new ArrayList<EventRegistration>();
			this.subscriptionsBySession.put(sessionId, list);
		}
		list.add(registration);
	}

	@Override
	public void processDisconnect(Message<?> message) {
		String sessionId = (String) message.getHeaders().get("sessionId");
		removeSubscriptions(sessionId);
	}

	@Override
	protected void processClientConnectionClosed(String sessionId) {
		removeSubscriptions(sessionId);
	}

	private void removeSubscriptions(String sessionId) {
		List<EventRegistration> registrations = this.subscriptionsBySession.remove(sessionId);
		if (logger.isTraceEnabled()) {
			logger.trace("Cancelling " + registrations.size() + " subscriptions for session=" + sessionId);
		}
		for (EventRegistration registration : registrations) {
			registration.cancel();
		}
	}

}
