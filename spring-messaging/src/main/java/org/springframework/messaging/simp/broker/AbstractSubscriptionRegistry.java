/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.broker;

import org.apache.commons.logging.Log;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Abstract base class for implementations of {@link SubscriptionRegistry} that
 * looks up information in messages but delegates to abstract methods for the
 * actual storage and retrieval.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSubscriptionRegistry implements SubscriptionRegistry {

	private static final MultiValueMap<String, String> EMPTY_MAP =
			CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());

	protected final Log logger = SimpLogging.forLogName(getClass());


	@Override
	public final void registerSubscription(Message<?> message) {
		MessageHeaders headers = message.getHeaders();

		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		if (!SimpMessageType.SUBSCRIBE.equals(messageType)) {
			throw new IllegalArgumentException("Expected SUBSCRIBE: " + message);
		}

		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		if (sessionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No sessionId in  " + message);
			}
			return;
		}

		String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(headers);
		if (subscriptionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No subscriptionId in " + message);
			}
			return;
		}

		String destination = SimpMessageHeaderAccessor.getDestination(headers);
		if (destination == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No destination in " + message);
			}
			return;
		}

		addSubscriptionInternal(sessionId, subscriptionId, destination, message);
	}

	@Override
	public final void unregisterSubscription(Message<?> message) {
		MessageHeaders headers = message.getHeaders();

		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		if (!SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			throw new IllegalArgumentException("Expected UNSUBSCRIBE: " + message);
		}

		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		if (sessionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No sessionId in " + message);
			}
			return;
		}

		String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(headers);
		if (subscriptionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No subscriptionId " + message);
			}
			return;
		}

		removeSubscriptionInternal(sessionId, subscriptionId, message);
	}

	@Override
	public final MultiValueMap<String, String> findSubscriptions(Message<?> message) {
		MessageHeaders headers = message.getHeaders();

		SimpMessageType type = SimpMessageHeaderAccessor.getMessageType(headers);
		if (!SimpMessageType.MESSAGE.equals(type)) {
			throw new IllegalArgumentException("Unexpected message type: " + type);
		}

		String destination = SimpMessageHeaderAccessor.getDestination(headers);
		if (destination == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No destination in " + message);
			}
			return EMPTY_MAP;
		}

		return findSubscriptionsInternal(destination, message);
	}


	protected abstract void addSubscriptionInternal(
			String sessionId, String subscriptionId, String destination, Message<?> message);

	protected abstract void removeSubscriptionInternal(
			String sessionId, String subscriptionId, Message<?> message);

	protected abstract MultiValueMap<String, String> findSubscriptionsInternal(
			String destination, Message<?> message);

}
