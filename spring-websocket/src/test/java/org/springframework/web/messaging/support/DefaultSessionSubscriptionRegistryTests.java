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

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.messaging.SessionSubscriptionRegistration;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link DefaultSessionSubscriptionRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultSessionSubscriptionRegistryTests {

	private DefaultSessionSubscriptionRegistry registry;


	@Before
	public void setup() {
		this.registry = new DefaultSessionSubscriptionRegistry();
	}

	@Test
	public void getRegistration() {
		String sessionId = "sess1";
		assertNull(this.registry.getRegistration(sessionId));

		this.registry.getOrCreateRegistration(sessionId);
		assertNotNull(this.registry.getRegistration(sessionId));
		assertEquals(sessionId, this.registry.getRegistration(sessionId).getSessionId());
	}

	@Test
	public void getOrCreateRegistration() {
		String sessionId = "sess1";
		assertNull(this.registry.getRegistration(sessionId));

		SessionSubscriptionRegistration registration = this.registry.getOrCreateRegistration(sessionId);
		assertSame(registration, this.registry.getOrCreateRegistration(sessionId));
	}

	@Test
	public void removeRegistration() {
		String sessionId = "sess1";
		this.registry.getOrCreateRegistration(sessionId);
		assertNotNull(this.registry.getRegistration(sessionId));
		assertEquals(sessionId, this.registry.getRegistration(sessionId).getSessionId());

		this.registry.removeRegistration(sessionId);
		assertNull(this.registry.getRegistration(sessionId));
	}

	@Test
	public void getSessionSubscriptions() {
		String sessionId = "sess1";
		SessionSubscriptionRegistration registration = this.registry.getOrCreateRegistration(sessionId);
		registration.addSubscription("/foo", "sub1");
		registration.addSubscription("/foo", "sub2");

		Set<String> subscriptions = this.registry.getSessionSubscriptions(sessionId, "/foo");
		assertEquals("Wrong number of subscriptions " + subscriptions, 2, subscriptions.size());
		assertTrue(subscriptions.contains("sub1"));
		assertTrue(subscriptions.contains("sub2"));
	}

}
