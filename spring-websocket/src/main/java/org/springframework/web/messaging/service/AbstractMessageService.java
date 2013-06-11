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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.PubSubHeaders;
import org.springframework.web.messaging.event.EventBus;
import org.springframework.web.messaging.event.EventConsumer;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractMessageService {

	protected final Log logger = LogFactory.getLog(getClass());


	public static final String CLIENT_TO_SERVER_MESSAGE_KEY = "clientToServerMessageKey";

	public static final String CLIENT_CONNECTION_CLOSED_KEY = "clientConnectionClosed";

	public static final String SERVER_TO_CLIENT_MESSAGE_KEY = "serverToClientMessageKey";


	private final EventBus eventBus;

	private final List<String> allowedDestinations = new ArrayList<String>();

	private final List<String> disallowedDestinations = new ArrayList<String>();

	private final PathMatcher pathMatcher = new AntPathMatcher();


	public AbstractMessageService(EventBus reactor) {

		Assert.notNull(reactor, "reactor is required");
		this.eventBus = reactor;

		this.eventBus.registerConsumer(CLIENT_TO_SERVER_MESSAGE_KEY, new EventConsumer<Message<?>>() {

			@Override
			public void accept(Message<?> message) {

				if (!isAllowedDestination(message)) {
					return;
				}

				if (logger.isTraceEnabled()) {
					logger.trace("Processing message id=" + message.getHeaders().getId());
				}

				PubSubHeaders headers = PubSubHeaders.fromMessageHeaders(message.getHeaders());
				MessageType messageType = headers.getMessageType();
				if (messageType == null || messageType.equals(MessageType.OTHER)) {
					processOther(message);
				}
				else if (MessageType.CONNECT.equals(messageType)) {
					processConnect(message);
				}
				else if (MessageType.MESSAGE.equals(messageType)) {
					processMessage(message);
				}
				else if (MessageType.SUBSCRIBE.equals(messageType)) {
					processSubscribe(message);
				}
				else if (MessageType.UNSUBSCRIBE.equals(messageType)) {
					processUnsubscribe(message);
				}
				else if (MessageType.DISCONNECT.equals(messageType)) {
					processDisconnect(message);
				}
			}
		});

		this.eventBus.registerConsumer(CLIENT_CONNECTION_CLOSED_KEY, new EventConsumer<String>() {

			@Override
			public void accept(String sessionId) {
				processClientConnectionClosed(sessionId);
			}
		});
	}


	/**
	 * Ant-style destination patterns that this service is allowed to process.
	 */
	public void setAllowedDestinations(String... patterns) {
		this.allowedDestinations.clear();
		this.allowedDestinations.addAll(Arrays.asList(patterns));
	}

	/**
	 * Ant-style destination patterns that this service should skip.
	 */
	public void setDisallowedDestinations(String... patterns) {
		this.disallowedDestinations.clear();
		this.disallowedDestinations.addAll(Arrays.asList(patterns));
	}

	public EventBus getEventBus() {
		return this.eventBus;
	}

	private boolean isAllowedDestination(Message<?> message) {
		PubSubHeaders headers = PubSubHeaders.fromMessageHeaders(message.getHeaders());
		String destination = headers.getDestination();
		if (destination == null) {
			return true;
		}
		if (!this.disallowedDestinations.isEmpty()) {
			for (String pattern : this.disallowedDestinations) {
				if (this.pathMatcher.match(pattern, destination)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Skip notification message id=" + message.getHeaders().getId());
					}
					return false;
				}
			}
		}
		if (!this.allowedDestinations.isEmpty()) {
			for (String pattern : this.allowedDestinations) {
				if (this.pathMatcher.match(pattern, destination)) {
					return true;
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Skip notification message id=" + message.getHeaders().getId());
			}
			return false;
		}
		return true;
	}

	protected void processConnect(Message<?> message) {
	}

	protected void processMessage(Message<?> message) {
	}

	protected void processSubscribe(Message<?> message) {
	}

	protected void processUnsubscribe(Message<?> message) {
	}

	protected void processDisconnect(Message<?> message) {
	}

	protected void processOther(Message<?> message) {
	}

	protected void processClientConnectionClosed(String sessionId) {
	}

}
