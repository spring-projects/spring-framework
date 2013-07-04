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

package org.springframework.web.messaging.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.web.messaging.SessionSubscriptionRegistration;


/**
 * A default implementation of SessionSubscriptionRegistration. Uses a map to keep track
 * of subscriptions by destination. This implementation assumes that only one thread will
 * access and update subscriptions at a time.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultSessionSubscriptionRegistration implements SessionSubscriptionRegistration {

	private final String sessionId;

	// destination -> subscriptionIds
	private final Map<String, Set<String>> subscriptions = new HashMap<String, Set<String>>(4);


	public DefaultSessionSubscriptionRegistration(String sessionId) {
		Assert.notNull(sessionId, "sessionId is required");
		this.sessionId = sessionId;
	}


	public String getSessionId() {
		return this.sessionId;
	}

	@Override
	public Set<String> getDestinations() {
		return this.subscriptions.keySet();
	}

	@Override
	public void addSubscription(String destination, String subscriptionId) {
		Assert.hasText(destination, "destination must not be empty");
		Assert.hasText(subscriptionId, "subscriptionId must not be empty");
		Set<String> subs = this.subscriptions.get(destination);
		if (subs == null) {
			subs = new HashSet<String>(4);
			this.subscriptions.put(destination, subs);
		}
		subs.add(subscriptionId);
	}

	@Override
	public String removeSubscription(String subscriptionId) {
		Assert.hasText(subscriptionId, "subscriptionId must not be empty");
		for (String destination : this.subscriptions.keySet()) {
			Set<String> subscriptionIds = this.subscriptions.get(destination);
			if (subscriptionIds.remove(subscriptionId)) {
				if (subscriptionIds.isEmpty()) {
					this.subscriptions.remove(destination);
				}
				return destination;
			}
		}
		return null;
	}

	@Override
	public Set<String> getSubscriptionsByDestination(String destination) {
		Assert.hasText(destination, "destination must not be empty");
		return this.subscriptions.get(destination);
	}


	@Override
	public String toString() {
		return "DefaultSessionSubscriptionRegistration [sessionId=" + this.sessionId
				+ ", subscriptions=" + this.subscriptions + "]";
	}

}
