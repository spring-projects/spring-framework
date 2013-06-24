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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.PubSubChannelRegistry;
import org.springframework.web.messaging.converter.CompositeMessageConverter;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.support.PubSubHeaderAccesssor;

import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.registry.Registration;
import reactor.fn.selector.ObjectSelector;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ReactorPubSubMessageHandler extends AbstractPubSubMessageHandler {

	private MessageChannel clientChannel;

	private final Reactor reactor;

	private MessageConverter payloadConverter;

	private Map<String, List<Registration<?>>> subscriptionsBySession = new ConcurrentHashMap<String, List<Registration<?>>>();


	public ReactorPubSubMessageHandler(PubSubChannelRegistry registry, Reactor reactor) {
		Assert.notNull(reactor, "reactor is required");
		this.clientChannel = registry.getClientOutputChannel();
		this.reactor = reactor;
		this.payloadConverter = new CompositeMessageConverter(null);
	}

	public void setMessageConverters(List<MessageConverter> converters) {
		this.payloadConverter = new CompositeMessageConverter(converters);
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

		PubSubHeaderAccesssor headers = PubSubHeaderAccesssor.wrap(message);
		String subscriptionId = headers.getSubscriptionId();
		BroadcastingConsumer consumer = new BroadcastingConsumer(subscriptionId);

		String key = getPublishKey(headers.getDestination());
		Registration<?> registration = this.reactor.on(new ObjectSelector<String>(key), consumer);

		String sessionId = headers.getSessionId();
		List<Registration<?>> list = this.subscriptionsBySession.get(sessionId);
		if (list == null) {
			list = new ArrayList<Registration<?>>();
			this.subscriptionsBySession.put(sessionId, list);
		}
		list.add(registration);
	}

	private String getPublishKey(String destination) {
		return "destination:" + destination;
	}

	@Override
	public void handlePublish(Message<?> message) {

		if (logger.isDebugEnabled()) {
			logger.debug("Message received: " + message);
		}

		try {
			// Convert to byte[] payload before the fan-out
			PubSubHeaderAccesssor headers = PubSubHeaderAccesssor.wrap(message);
			byte[] payload = payloadConverter.convertToPayload(message.getPayload(), headers.getContentType());
			Message<?> m = MessageBuilder.withPayload(payload).copyHeaders(message.getHeaders()).build();

			this.reactor.notify(getPublishKey(headers.getDestination()), Event.wrap(m));
		}
		catch (Exception ex) {
			logger.error("Failed to publish " + message, ex);
		}
	}

	@Override
	public void handleDisconnect(Message<?> message) {
		PubSubHeaderAccesssor headers = PubSubHeaderAccesssor.wrap(message);
		removeSubscriptions(headers.getSessionId());
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


	private final class BroadcastingConsumer implements Consumer<Event<Message<?>>> {

		private final String subscriptionId;


		private BroadcastingConsumer(String subscriptionId) {
			this.subscriptionId = subscriptionId;
		}

		@Override
		public void accept(Event<Message<?>> event) {

			Message<?> sentMessage = event.getData();

			PubSubHeaderAccesssor headers = PubSubHeaderAccesssor.wrap(sentMessage);
			headers.setSubscriptionId(this.subscriptionId);

			Message<?> clientMessage = MessageBuilder.withPayload(
					sentMessage.getPayload()).copyHeaders(headers.toHeaders()).build();

			clientChannel.send(clientMessage);
		}
	}

}
