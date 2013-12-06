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

package org.springframework.messaging.simp.broker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.DefaultSubscriptionRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link org.springframework.messaging.simp.broker.DefaultSubscriptionRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultSubscriptionRegistryTests {

	private DefaultSubscriptionRegistry registry;


	@Before
	public void setup() {
		this.registry = new DefaultSubscriptionRegistry();
	}


	@Test
	public void registerSubscriptionInvalidInput() {

		String sessId = "sess01";
		String subsId = "subs01";
		String dest = "/foo";

		this.registry.registerSubscription(subscribeMessage(null, subsId, dest));
		assertEquals(0, this.registry.findSubscriptions(message(dest)).size());

		this.registry.registerSubscription(subscribeMessage(sessId, null, dest));
		assertEquals(0, this.registry.findSubscriptions(message(dest)).size());

		this.registry.registerSubscription(subscribeMessage(sessId, subsId, null));
		assertEquals(0, this.registry.findSubscriptions(message(dest)).size());
	}

	@Test
	public void registerSubscription() {

		String sessId = "sess01";
		String subsId = "subs01";
		String dest = "/foo";

		this.registry.registerSubscription(subscribeMessage(sessId, subsId, dest));
		MultiValueMap<String, String> actual = this.registry.findSubscriptions(message(dest));

		assertEquals("Expected one element " + actual, 1, actual.size());
		assertEquals(Arrays.asList(subsId), actual.get(sessId));
	}

	@Test
	public void registerSubscriptionOneSession() {

		String sessId = "sess01";
		List<String> subscriptionIds = Arrays.asList("subs01", "subs02", "subs03");
		String dest = "/foo";

		for (String subId : subscriptionIds) {
			this.registry.registerSubscription(subscribeMessage(sessId, subId, dest));
		}

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(message(dest));

		assertEquals("Expected one element " + actual, 1, actual.size());
		assertEquals(subscriptionIds, sort(actual.get(sessId)));
	}

	@Test
	public void registerSubscriptionMultipleSessions() {

		List<String> sessIds = Arrays.asList("sess01", "sess02", "sess03");
		List<String> subscriptionIds = Arrays.asList("subs01", "subs02", "subs03");
		String dest = "/foo";

		for (String sessId : sessIds) {
			for (String subsId : subscriptionIds) {
				this.registry.registerSubscription(subscribeMessage(sessId, subsId, dest));
			}
		}

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(message(dest));

		assertEquals("Expected three elements " + actual, 3, actual.size());
		assertEquals(subscriptionIds, sort(actual.get(sessIds.get(0))));
		assertEquals(subscriptionIds, sort(actual.get(sessIds.get(1))));
		assertEquals(subscriptionIds, sort(actual.get(sessIds.get(2))));
	}

	@Test
	public void registerSubscriptionWithDestinationPattern() {

		String sessId = "sess01";
		String subsId = "subs01";
		String destPattern = "/topic/PRICE.STOCK.*.IBM";
		String dest = "/topic/PRICE.STOCK.NASDAQ.IBM";

		this.registry.registerSubscription(subscribeMessage(sessId, subsId, destPattern));
		MultiValueMap<String, String> actual = this.registry.findSubscriptions(message(dest));

		assertEquals("Expected one element " + actual, 1, actual.size());
		assertEquals(Arrays.asList(subsId), actual.get(sessId));
	}

	@Test
	public void registerSubscriptionWithDestinationPatternRegex() {

		String sessId = "sess01";
		String subsId = "subs01";
		String destPattern = "/topic/PRICE.STOCK.*.{ticker:(IBM|MSFT)}";

		this.registry.registerSubscription(subscribeMessage(sessId, subsId, destPattern));
		Message<?> message = message("/topic/PRICE.STOCK.NASDAQ.IBM");
		MultiValueMap<String, String> actual = this.registry.findSubscriptions(message);

		assertEquals("Expected one element " + actual, 1, actual.size());
		assertEquals(Arrays.asList(subsId), actual.get(sessId));

		message = message("/topic/PRICE.STOCK.NASDAQ.MSFT");
		actual = this.registry.findSubscriptions(message);

		assertEquals("Expected one element " + actual, 1, actual.size());
		assertEquals(Arrays.asList(subsId), actual.get(sessId));

		message = message("/topic/PRICE.STOCK.NASDAQ.VMW");
		actual = this.registry.findSubscriptions(message);

		assertEquals("Expected no elements " + actual, 0, actual.size());
	}

	@Test
	public void unregisterSubscription() {

		List<String> sessIds = Arrays.asList("sess01", "sess02", "sess03");
		List<String> subscriptionIds = Arrays.asList("subs01", "subs02", "subs03");
		String dest = "/foo";

		for (String sessId : sessIds) {
			for (String subsId : subscriptionIds) {
				this.registry.registerSubscription(subscribeMessage(sessId, subsId, dest));
			}
		}

		this.registry.unregisterSubscription(unsubscribeMessage(sessIds.get(0), subscriptionIds.get(0)));
		this.registry.unregisterSubscription(unsubscribeMessage(sessIds.get(0), subscriptionIds.get(1)));
		this.registry.unregisterSubscription(unsubscribeMessage(sessIds.get(0), subscriptionIds.get(2)));

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(message(dest));

		assertEquals("Expected three elements " + actual, 2, actual.size());
		assertEquals(subscriptionIds, sort(actual.get(sessIds.get(1))));
		assertEquals(subscriptionIds, sort(actual.get(sessIds.get(2))));
	}

	@Test
	public void unregisterAllSubscriptions() {

		List<String> sessIds = Arrays.asList("sess01", "sess02", "sess03");
		List<String> subscriptionIds = Arrays.asList("subs01", "subs02", "subs03");
		String dest = "/foo";

		for (String sessId : sessIds) {
			for (String subsId : subscriptionIds) {
				this.registry.registerSubscription(subscribeMessage(sessId, subsId, dest));
			}
		}

		this.registry.unregisterAllSubscriptions(sessIds.get(0));
		this.registry.unregisterAllSubscriptions(sessIds.get(1));

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(message(dest));

		assertEquals("Expected three elements " + actual, 1, actual.size());
		assertEquals(subscriptionIds, sort(actual.get(sessIds.get(2))));
	}

	@Test
	public void unregisterAllSubscriptionsNoMatch() {
		this.registry.unregisterAllSubscriptions("bogus");
		// no exceptions
	}

	@Test
	public void findSubscriptionsNoMatches() {
		MultiValueMap<String, String> actual = this.registry.findSubscriptions(message("/foo"));
		assertEquals("Expected no elements " + actual, 0, actual.size());
	}


	private Message<?> subscribeMessage(String sessionId, String subscriptionId, String destination) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
		headers.setSessionId(sessionId);
		headers.setSubscriptionId(subscriptionId);
		if (destination != null) {
			headers.setDestination(destination);
		}
		return MessageBuilder.withPayload("").copyHeaders(headers.toMap()).build();
	}

	private Message<?> unsubscribeMessage(String sessionId, String subscriptionId) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
		headers.setSessionId(sessionId);
		headers.setSubscriptionId(subscriptionId);
		return MessageBuilder.withPayload("").copyHeaders(headers.toMap()).build();
	}

	private Message<?> message(String destination) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		headers.setDestination(destination);
		return MessageBuilder.withPayload("").copyHeaders(headers.toMap()).build();
	}

	private List<String> sort(List<String> list) {
		Collections.sort(list);
		return list;
	}

}
