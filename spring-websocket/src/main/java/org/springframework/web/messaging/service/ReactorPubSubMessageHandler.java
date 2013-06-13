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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.PubSubHeaders;
import org.springframework.web.messaging.converter.CompositeMessageConverter;
import org.springframework.web.messaging.converter.MessageConverter;

import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.registry.Registration;
import reactor.fn.selector.ObjectSelector;
import reactor.fn.selector.Selector;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ReactorPubSubMessageHandler extends AbstractPubSubMessageHandler {

	private final Reactor reactor;

	private MessageConverter payloadConverter;

	private Map<String, List<Registration<?>>> subscriptionsBySession = new ConcurrentHashMap<String, List<Registration<?>>>();


	public ReactorPubSubMessageHandler(SubscribableChannel publishChannel, MessageChannel clientChannel,
			Reactor reactor) {

		super(publishChannel, clientChannel);
		this.reactor = reactor;
		this.payloadConverter = new CompositeMessageConverter(null);
	}


	public void setMessageConverters(List<MessageConverter> converters) {
		this.payloadConverter = new CompositeMessageConverter(converters);
	}

	@Override
	public void handlePublish(Message<?> message) {

		if (logger.isDebugEnabled()) {
			logger.debug("Message received: " + message);
		}

		try {
			// Convert to byte[] payload before the fan-out
			PubSubHeaders inHeaders = PubSubHeaders.fromMessageHeaders(message.getHeaders());
			byte[] payload = payloadConverter.convertToPayload(message.getPayload(), inHeaders.getContentType());
			message = new GenericMessage<byte[]>(payload, message.getHeaders());

			this.reactor.notify(getPublishKey(inHeaders.getDestination()), Event.wrap(message));
		}
		catch (Exception ex) {
			logger.error("Failed to publish " + message, ex);
		}
	}

	private String getPublishKey(String destination) {
		return "destination:" + destination;
	}

	@Override
	protected Collection<MessageType> getSupportedMessageTypes() {
		return Arrays.asList(MessageType.MESSAGE, MessageType.SUBSCRIBE, MessageType.UNSUBSCRIBE);
	}


	@Override
	public void handleSubscribe(Message<?> message) {

		if (logger.isDebugEnabled()) {
			logger.debug("Subscribe " + message);
		}

		PubSubHeaders headers = PubSubHeaders.fromMessageHeaders(message.getHeaders());
		final String subscriptionId = headers.getSubscriptionId();

		Selector selector = new ObjectSelector<String>(getPublishKey(headers.getDestination()));
		Registration<?> registration = this.reactor.on(selector,
				new Consumer<Event<Message<?>>>() {
					@Override
					public void accept(Event<Message<?>> event) {
						Message<?> message = event.getData();
						PubSubHeaders inHeaders = PubSubHeaders.fromMessageHeaders(message.getHeaders());
						PubSubHeaders outHeaders = PubSubHeaders.create();
						outHeaders.setDestinations(inHeaders.getDestinations());
						if (inHeaders.getContentType() != null) {
							outHeaders.setContentType(inHeaders.getContentType());
						}
						outHeaders.setSubscriptionId(subscriptionId);
						Object payload = message.getPayload();
						message = new GenericMessage<Object>(payload, outHeaders.toMessageHeaders());
						getClientChannel().send(message);
					}
				});

		addSubscription(headers.getSessionId(), registration);
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
	public void handleDisconnect(Message<?> message) {
		PubSubHeaders headers = PubSubHeaders.fromMessageHeaders(message.getHeaders());
		removeSubscriptions(headers.getSessionId());
	}

/*	@Override
	public void handleClientConnectionClosed(String sessionId) {
		removeSubscriptions(sessionId);
	}
*/
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
