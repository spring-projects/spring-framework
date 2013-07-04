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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.messaging.SessionSubscriptionRegistration;
import org.springframework.web.messaging.SessionSubscriptionRegistry;


/**
 * A default implementation of SessionSubscriptionRegistry.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultSessionSubscriptionRegistry implements SessionSubscriptionRegistry {

	// sessionId -> SessionSubscriptionRegistration
	private final Map<String, SessionSubscriptionRegistration> registrations =
			new ConcurrentHashMap<String, SessionSubscriptionRegistration>();


	@Override
	public SessionSubscriptionRegistration getRegistration(String sessionId) {
		return this.registrations.get(sessionId);
	}

	@Override
	public SessionSubscriptionRegistration getOrCreateRegistration(String sessionId) {
		SessionSubscriptionRegistration registration = this.registrations.get(sessionId);
		if (registration == null) {
			registration = new DefaultSessionSubscriptionRegistration(sessionId);
			this.registrations.put(sessionId, registration);
		}
		return registration;
	}

	@Override
	public SessionSubscriptionRegistration removeRegistration(String sessionId) {
		return this.registrations.remove(sessionId);
	}

	@Override
	public Set<String> getSessionSubscriptions(String sessionId, String destination) {
		SessionSubscriptionRegistration registration = this.registrations.get(sessionId);
		return (registration != null) ? registration.getSubscriptionsByDestination(destination) : null;
	}

}
