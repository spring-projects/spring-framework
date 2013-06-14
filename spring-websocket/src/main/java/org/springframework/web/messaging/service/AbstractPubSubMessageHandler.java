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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.PubSubHeaders;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractPubSubMessageHandler implements MessageHandler<Message<?>> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final MessageChannel publishChannel;

	private final MessageChannel clientChannel;

	private final List<String> allowedDestinations = new ArrayList<String>();

	private final List<String> disallowedDestinations = new ArrayList<String>();

	private final PathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * @param publishChannel a channel for publishing messages from within the
	 *        application
	 *
	 * @param clientChannel a channel for sending messages to connected clients.
	 */
	public AbstractPubSubMessageHandler(SubscribableChannel publishChannel, MessageChannel clientChannel) {

		Assert.notNull(publishChannel, "publishChannel is required");
		Assert.notNull(clientChannel, "clientChannel is required");

		this.publishChannel = publishChannel;
		this.clientChannel = clientChannel;
	}

	public MessageChannel getPublishChannel() {
		return this.publishChannel;
	}

	public MessageChannel getClientChannel() {
		return this.clientChannel;
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

	protected abstract Collection<MessageType> getSupportedMessageTypes();


	protected boolean canHandle(Message<?> message, MessageType messageType) {

		if (!CollectionUtils.isEmpty(getSupportedMessageTypes())) {
			if (!getSupportedMessageTypes().contains(messageType)) {
				return false;
			}
		}

		return isDestinationAllowed(message);
	}

	protected boolean isDestinationAllowed(Message<?> message) {

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

	@Override
	public final void handleMessage(Message<?> message) throws MessagingException {

		PubSubHeaders headers = PubSubHeaders.fromMessageHeaders(message.getHeaders());
		MessageType messageType = headers.getMessageType();

		if (!canHandle(message, messageType)) {
			return;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Handling message id=" + message.getHeaders().getId());
		}

		if (MessageType.MESSAGE.equals(messageType)) {
			handlePublish(message);
		}
		else if (MessageType.SUBSCRIBE.equals(messageType)) {
			handleSubscribe(message);
		}
		else if (MessageType.UNSUBSCRIBE.equals(messageType)) {
			handleUnsubscribe(message);
		}
		else if (MessageType.CONNECT.equals(messageType)) {
			handleConnect(message);
		}
		else if (MessageType.DISCONNECT.equals(messageType)) {
			handleDisconnect(message);
		}
		else {
			handleOther(message);
		}
	}

	protected void handleConnect(Message<?> message) {
	}

	protected void handlePublish(Message<?> message) {
	}

	protected void handleSubscribe(Message<?> message) {
	}

	protected void handleUnsubscribe(Message<?> message) {
	}

	protected void handleDisconnect(Message<?> message) {
	}

	protected void handleOther(Message<?> message) {
	}

}
