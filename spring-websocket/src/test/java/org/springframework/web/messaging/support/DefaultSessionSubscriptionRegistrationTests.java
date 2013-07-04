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

import static org.junit.Assert.*;


/**
 * Test fixture for {@link DefaultSessionSubscriptionRegistration}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultSessionSubscriptionRegistrationTests {

	private DefaultSessionSubscriptionRegistration registration;


	@Before
	public void setup() {
		this.registration = new DefaultSessionSubscriptionRegistration("sess1");
	}

	@Test
	public void addSubscriptions() {
		this.registration.addSubscription("/foo", "sub1");
		this.registration.addSubscription("/foo", "sub2");
		this.registration.addSubscription("/bar", "sub3");
		this.registration.addSubscription("/bar", "sub4");

		assertSet(this.registration.getSubscriptionsByDestination("/foo"), 2, "sub1", "sub2");
		assertSet(this.registration.getSubscriptionsByDestination("/bar"), 2, "sub3", "sub4");
		assertSet(this.registration.getDestinations(), 2, "/foo", "/bar");
	}

	@Test
	public void removeSubscriptions() {
		this.registration.addSubscription("/foo", "sub1");
		this.registration.addSubscription("/foo", "sub2");
		this.registration.addSubscription("/bar", "sub3");
		this.registration.addSubscription("/bar", "sub4");

		assertEquals("/foo", this.registration.removeSubscription("sub1"));
		assertEquals("/foo", this.registration.removeSubscription("sub2"));

		assertNull(this.registration.getSubscriptionsByDestination("/foo"));
		assertSet(this.registration.getDestinations(), 1, "/bar");

		assertEquals("/bar", this.registration.removeSubscription("sub3"));
		assertEquals("/bar", this.registration.removeSubscription("sub4"));

		assertNull(this.registration.getSubscriptionsByDestination("/bar"));
		assertSet(this.registration.getDestinations(), 0);
	}


	private void assertSet(Set<String> set, int size, String... elements) {
		assertEquals("Wrong number of elements in " + set, size, set.size());
		for (String element : elements) {
			assertTrue("Set does not contain element " + element, set.contains(element));
		}
	}

}
