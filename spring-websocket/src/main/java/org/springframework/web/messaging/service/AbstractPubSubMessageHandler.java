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
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.support.PubSubHeaderAccesssor;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractPubSubMessageHandler<M extends Message> implements MessageHandler<M> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<String> allowedDestinations = new ArrayList<String>();

	private final List<String> disallowedDestinations = new ArrayList<String>();

	private final PathMatcher pathMatcher = new AntPathMatcher();


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


	protected boolean canHandle(M message, MessageType messageType) {

		if (!CollectionUtils.isEmpty(getSupportedMessageTypes())) {
			if (!getSupportedMessageTypes().contains(messageType)) {
				return false;
			}
		}

		return isDestinationAllowed(message);
	}

	protected boolean isDestinationAllowed(M message) {

		PubSubHeaderAccesssor headers = PubSubHeaderAccesssor.wrap(message);
		String destination = headers.getDestination();

		if (destination == null) {
			return true;
		}

		if (!this.disallowedDestinations.isEmpty()) {
			for (String pattern : this.disallowedDestinations) {
				if (this.pathMatcher.match(pattern, destination)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Skip message id=" + message.getHeaders().getId());
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
				logger.trace("Skip message id=" + message.getHeaders().getId());
			}
			return false;
		}

		return true;
	}

	@Override
	public final void handleMessage(M message) throws MessagingException {

		PubSubHeaderAccesssor headers = PubSubHeaderAccesssor.wrap(message);
		MessageType messageType = headers.getMessageType();

		if (!canHandle(message, messageType)) {
			return;
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

	protected void handleConnect(M message) {
	}

	protected void handlePublish(M message) {
	}

	protected void handleSubscribe(M message) {
	}

	protected void handleUnsubscribe(M message) {
	}

	protected void handleDisconnect(M message) {
	}

	protected void handleOther(M message) {
	}

}
