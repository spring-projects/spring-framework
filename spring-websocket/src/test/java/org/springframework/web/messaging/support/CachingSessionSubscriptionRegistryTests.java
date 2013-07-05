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
import org.springframework.web.messaging.SessionSubscriptionRegistry;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link CachingSessionSubscriptionRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class CachingSessionSubscriptionRegistryTests {

	private CachingSessionSubscriptionRegistry registry;


	@Before
	public void setup() {
		SessionSubscriptionRegistry delegate = new DefaultSessionSubscriptionRegistry();
		this.registry = new CachingSessionSubscriptionRegistry(delegate);
	}

	@Test
	public void getRegistrationsByDestination() {

		SessionSubscriptionRegistration reg1 = this.registry.getOrCreateRegistration("sess1");
		reg1.addSubscription("/foo", "sub1");

		SessionSubscriptionRegistration reg2 = this.registry.getOrCreateRegistration("sess2");
		reg2.addSubscription("/foo", "sub1");

		Set<SessionSubscriptionRegistration> actual = this.registry.getRegistrationsByDestination("/foo");
		assertEquals(2, actual.size());
		assertTrue(actual.contains(reg1));
		assertTrue(actual.contains(reg2));

		reg1.removeSubscription("sub1");

		actual = this.registry.getRegistrationsByDestination("/foo");
		assertEquals("Invalid set of registrations " + actual, 1, actual.size());
		assertTrue(actual.contains(reg2));

		reg2.removeSubscription("sub1");

		actual = this.registry.getRegistrationsByDestination("/foo");
		assertNull("Unexpected registrations " + actual, actual);
	}

}
