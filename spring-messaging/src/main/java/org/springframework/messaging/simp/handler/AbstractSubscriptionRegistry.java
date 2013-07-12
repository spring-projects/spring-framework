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

package org.springframework.messaging.simp.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.MultiValueMap;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSubscriptionRegistry implements SubscriptionRegistry {

	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public void addSubscription(Message<?> message) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		if (!SimpMessageType.SUBSCRIBE.equals(headers.getMessageType())) {
			logger.error("Expected SUBSCRIBE message: " + message);
			return;
		}
		String sessionId = headers.getSessionId();
		if (sessionId == null) {
			logger.error("Ignoring subscription. No sessionId in message: " + message);
			return;
		}
		String subscriptionId = headers.getSubscriptionId();
		if (subscriptionId == null) {
			logger.error("Ignoring subscription. No subscriptionId in message: " + message);
			return;
		}
		String destination = headers.getDestination();
		if (destination == null) {
			logger.error("Ignoring destination. No destination in message: " + message);
			return;
		}
		addSubscriptionInternal(sessionId, subscriptionId, destination, message);
	}

	protected abstract void addSubscriptionInternal(String sessionId, String subscriptionId,
			String destination, Message<?> message);

	@Override
	public void removeSubscription(Message<?> message) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		if (!SimpMessageType.UNSUBSCRIBE.equals(headers.getMessageType())) {
			logger.error("Expected UNSUBSCRIBE message: " + message);
			return;
		}
		String sessionId = headers.getSessionId();
		if (sessionId == null) {
			logger.error("Ignoring subscription. No sessionId in message: " + message);
			return;
		}
		String subscriptionId = headers.getSubscriptionId();
		if (subscriptionId == null) {
			logger.error("Ignoring subscription. No subscriptionId in message: " + message);
			return;
		}
		removeSubscriptionInternal(sessionId, subscriptionId, message);
	}

	protected abstract void removeSubscriptionInternal(String sessionId, String subscriptionId, Message<?> message);

	@Override
	public void removeSessionSubscriptions(String sessionId) {
	}

	@Override
	public MultiValueMap<String, String> findSubscriptions(Message<?> message) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		if (!SimpMessageType.MESSAGE.equals(headers.getMessageType())) {
			logger.error("Unexpected message type: " + message);
			return null;
		}
		String destination = headers.getDestination();
		if (destination == null) {
			logger.error("Ignoring destination. No destination in message: " + message);
			return null;
		}
		return findSubscriptionsInternal(destination, message);
	}

	protected abstract MultiValueMap<String, String> findSubscriptionsInternal(
			String destination, Message<?> message);

}
