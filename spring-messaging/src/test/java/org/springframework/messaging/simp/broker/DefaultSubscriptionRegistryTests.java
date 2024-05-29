/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultSubscriptionRegistry}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class DefaultSubscriptionRegistryTests {

	private final DefaultSubscriptionRegistry registry = new DefaultSubscriptionRegistry();


	@Test
	void registerSubscriptionInvalidInput() {
		String sessId = "sess01";
		String subsId = "subs01";
		String dest = "/foo";

		this.registry.registerSubscription(subscribeMessage(null, subsId, dest));
		MultiValueMap<String, String> actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).isEmpty();

		this.registry.registerSubscription(subscribeMessage(sessId, null, dest));
		actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).isEmpty();

		this.registry.registerSubscription(subscribeMessage(sessId, subsId, null));
		actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).isEmpty();
	}

	@Test
	void registerSubscription() {
		String sessId = "sess01";
		String subsId = "subs01";
		String dest = "/foo";

		this.registry.registerSubscription(subscribeMessage(sessId, subsId, dest));

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).hasSize(1);
		assertThat(actual.get(sessId)).containsExactly(subsId);
	}

	@Test
	void registerSubscriptionOneSession() {
		String sessId = "sess01";
		List<String> subscriptionIds = List.of("subs01", "subs02", "subs03");
		String dest = "/foo";

		for (String subId : subscriptionIds) {
			this.registry.registerSubscription(subscribeMessage(sessId, subId, dest));
		}

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).hasSize(1);
		assertThat(sort(actual.get(sessId))).isEqualTo(subscriptionIds);
	}

	@Test
	void registerSameSubscriptionTwice() {
		String sessId = "sess01";
		String subId = "subs01";
		String dest = "/foo";

		this.registry.registerSubscription(subscribeMessage(sessId, subId, dest));
		this.registry.registerSubscription(subscribeMessage(sessId, subId, dest));

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).hasSize(1);
		assertThat(actual.get(sessId)).containsExactly(subId);

		// Register more after destinationCache populated through findSubscriptions,
		// and make sure it's still only one subscriptionId

		this.registry.registerSubscription(subscribeMessage(sessId, subId, dest));
		this.registry.registerSubscription(subscribeMessage(sessId, subId, dest));

		actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).hasSize(1);
		assertThat(actual.get(sessId)).containsExactly(subId);
	}

	@Test
	void registerSubscriptionMultipleSessions() {
		List<String> sessIds = List.of("sess01", "sess02", "sess03");
		List<String> subscriptionIds = List.of("subs01", "subs02", "subs03");
		String dest = "/foo";

		for (String sessId : sessIds) {
			for (String subsId : subscriptionIds) {
				this.registry.registerSubscription(subscribeMessage(sessId, subsId, dest));
			}
		}

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).hasSize(3);
		assertThat(sort(actual.get(sessIds.get(0)))).isEqualTo(subscriptionIds);
		assertThat(sort(actual.get(sessIds.get(1)))).isEqualTo(subscriptionIds);
		assertThat(sort(actual.get(sessIds.get(2)))).isEqualTo(subscriptionIds);
	}

	@Test
	void registerSubscriptionWithDestinationPattern() {
		String sessId = "sess01";
		String subsId = "subs01";
		String destPattern = "/topic/PRICE.STOCK.*.IBM";
		String dest = "/topic/PRICE.STOCK.NASDAQ.IBM";
		this.registry.registerSubscription(subscribeMessage(sessId, subsId, destPattern));

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).hasSize(1);
		assertThat(actual.get(sessId)).containsExactly(subsId);
	}

	@Test  // SPR-11657
	void registerSubscriptionsWithSimpleAndPatternDestinations() {
		String sess1 = "sess01";
		String sess2 = "sess02";

		String subs1 = "subs01";
		String subs2 = "subs02";
		String subs3 = "subs03";

		String destNasdaqIbm = "/topic/PRICE.STOCK.NASDAQ.IBM";
		Message<?> destNasdaqIbmMessage = createMessage(destNasdaqIbm);

		this.registry.registerSubscription(subscribeMessage(sess1, subs2, destNasdaqIbm));
		this.registry.registerSubscription(subscribeMessage(sess1, subs1, "/topic/PRICE.STOCK.*.IBM"));

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(destNasdaqIbmMessage);
		assertThat(actual).hasSize(1);
		assertThat(actual.get(sess1)).containsExactlyInAnyOrder(subs2, subs1);

		this.registry.registerSubscription(subscribeMessage(sess2, subs1, destNasdaqIbm));
		this.registry.registerSubscription(subscribeMessage(sess2, subs2, "/topic/PRICE.STOCK.NYSE.IBM"));
		this.registry.registerSubscription(subscribeMessage(sess2, subs3, "/topic/PRICE.STOCK.NASDAQ.GOOG"));

		actual = this.registry.findSubscriptions(destNasdaqIbmMessage);
		assertThat(actual).hasSize(2);
		assertThat(actual.get(sess1)).containsExactlyInAnyOrder(subs2, subs1);
		assertThat(actual.get(sess2)).containsExactly(subs1);

		this.registry.unregisterAllSubscriptions(sess1);

		actual = this.registry.findSubscriptions(destNasdaqIbmMessage);
		assertThat(actual).hasSize(1);
		assertThat(actual.get(sess2)).containsExactly(subs1);

		this.registry.registerSubscription(subscribeMessage(sess1, subs1, "/topic/PRICE.STOCK.*.IBM"));
		this.registry.registerSubscription(subscribeMessage(sess1, subs2, destNasdaqIbm));

		actual = this.registry.findSubscriptions(destNasdaqIbmMessage);
		assertThat(actual).hasSize(2);
		assertThat(actual.get(sess1)).containsExactlyInAnyOrder(subs1, subs2);
		assertThat(actual.get(sess2)).containsExactly(subs1);

		this.registry.unregisterSubscription(unsubscribeMessage(sess1, subs2));

		actual = this.registry.findSubscriptions(destNasdaqIbmMessage);
		assertThat(actual).hasSize(2);
		assertThat(actual.get(sess1)).containsExactly(subs1);
		assertThat(actual.get(sess2)).containsExactly(subs1);

		this.registry.unregisterSubscription(unsubscribeMessage(sess1, subs1));

		actual = this.registry.findSubscriptions(destNasdaqIbmMessage);
		assertThat(actual).hasSize(1);
		assertThat(actual.get(sess2)).containsExactly(subs1);

		this.registry.unregisterSubscription(unsubscribeMessage(sess2, subs1));

		actual = this.registry.findSubscriptions(destNasdaqIbmMessage);
		assertThat(actual).isEmpty();
	}

	@Test  // SPR-11755
	void registerAndUnregisterMultipleDestinations() {
		String sess1 = "sess01";
		String sess2 = "sess02";

		String subs1 = "subs01";
		String subs2 = "subs02";
		String subs3 = "subs03";
		String subs4 = "subs04";
		String subs5 = "subs05";

		this.registry.registerSubscription(subscribeMessage(sess1, subs1, "/topic/PRICE.STOCK.NASDAQ.IBM"));
		this.registry.registerSubscription(subscribeMessage(sess1, subs2, "/topic/PRICE.STOCK.NYSE.IBM"));
		this.registry.registerSubscription(subscribeMessage(sess1, subs3, "/topic/PRICE.STOCK.NASDAQ.GOOG"));

		this.registry.findSubscriptions(createMessage("/topic/PRICE.STOCK.NYSE.IBM"));
		this.registry.findSubscriptions(createMessage("/topic/PRICE.STOCK.NASDAQ.GOOG"));
		this.registry.findSubscriptions(createMessage("/topic/PRICE.STOCK.NASDAQ.IBM"));

		this.registry.unregisterSubscription(unsubscribeMessage(sess1, subs1));
		this.registry.unregisterSubscription(unsubscribeMessage(sess1, subs2));
		this.registry.unregisterSubscription(unsubscribeMessage(sess1, subs3));

		this.registry.registerSubscription(subscribeMessage(sess1, subs1, "/topic/PRICE.STOCK.NASDAQ.IBM"));
		this.registry.registerSubscription(subscribeMessage(sess1, subs2, "/topic/PRICE.STOCK.NYSE.IBM"));
		this.registry.registerSubscription(subscribeMessage(sess1, subs3, "/topic/PRICE.STOCK.NASDAQ.GOOG"));
		this.registry.registerSubscription(subscribeMessage(sess1, subs4, "/topic/PRICE.STOCK.NYSE.IBM"));
		this.registry.registerSubscription(subscribeMessage(sess2, subs5, "/topic/PRICE.STOCK.NASDAQ.GOOG"));

		this.registry.unregisterAllSubscriptions(sess1);
		this.registry.unregisterAllSubscriptions(sess2);
	}

	@Test
	void registerSubscriptionWithDestinationPatternRegex() {
		String sessId = "sess01";
		String subsId = "subs01";
		String destPattern = "/topic/PRICE.STOCK.*.{ticker:(IBM|MSFT)}";

		this.registry.registerSubscription(subscribeMessage(sessId, subsId, destPattern));
		Message<?> message = createMessage("/topic/PRICE.STOCK.NASDAQ.IBM");
		MultiValueMap<String, String> actual = this.registry.findSubscriptions(message);
		assertThat(actual).hasSize(1);
		assertThat(actual.get(sessId)).containsExactly(subsId);

		message = createMessage("/topic/PRICE.STOCK.NASDAQ.MSFT");
		actual = this.registry.findSubscriptions(message);
		assertThat(actual).hasSize(1);
		assertThat(actual.get(sessId)).containsExactly(subsId);

		message = createMessage("/topic/PRICE.STOCK.NASDAQ.VMW");
		actual = this.registry.findSubscriptions(message);
		assertThat(actual).isEmpty();
	}

	@Test
	void registerSubscriptionWithSelectorHeaderEnabled() {
		String sessionId1 = "sess01";
		String sessionId2 = "sess02";
		String sessionId3 = "sess03";
		String subscriptionId1 = "subs01";
		String subscriptionId2 = "subs02";
		String subscriptionId3 = "subs02";
		String destination = "/foo";
		String selector1 = "headers.foo == 'bar'";
		String selector2 = "headers.foo == 'enigma'";

		// Explicitly enable selector support
		this.registry.setSelectorHeaderName("selector");

		// Register subscription with matching selector header
		this.registry.registerSubscription(subscribeMessage(sessionId1, subscriptionId1, destination, selector1));
		// Register subscription with non-matching selector header
		this.registry.registerSubscription(subscribeMessage(sessionId2, subscriptionId2, destination, selector2));
		// Register subscription without a selector header
		this.registry.registerSubscription(subscribeMessage(sessionId3, subscriptionId3, destination, null));

		// First, try with message WITH selected 'foo' header present

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setDestination(destination);
		accessor.setNativeHeader("foo", "bar");
		Message<?> message = MessageBuilder.createMessage("", accessor.getMessageHeaders());

		MultiValueMap<String, String> subscriptions = this.registry.findSubscriptions(message);
		assertThat(subscriptions).hasSize(2);

		// Subscription #1 has a 'selector' header that DOES match.
		assertThat(subscriptions.get(sessionId1)).containsExactly(subscriptionId1);
		// Subscription #2 has a 'selector' header that does NOT match.
		assertThat(subscriptions.get(sessionId2)).isNull();
		// Subscription #3 does NOT have a 'selector' header, so it matches anyway.
		assertThat(subscriptions.get(sessionId3)).containsExactly(subscriptionId3);

		// Then try with message WITHOUT selected 'foo' header present

		subscriptions = this.registry.findSubscriptions(createMessage(destination));
		assertThat(subscriptions).hasSize(1);
		// Subscription #3 does NOT have a 'selector' header, so it matches anyway.
		assertThat(subscriptions.get(sessionId3)).containsExactly(subscriptionId3);
	}

	@Test
	void registerSubscriptionWithSelectorHeaderDisabledByDefault() {
		String sessionId1 = "sess01";
		String sessionId2 = "sess02";
		String sessionId3 = "sess03";
		String subscriptionId1 = "subs01";
		String subscriptionId2 = "subs02";
		String subscriptionId3 = "subs02";
		String destination = "/foo";
		String selector1 = "headers.foo == 'bar'";
		String selector2 = "headers.foo == 'enigma'";

		// Register subscription with matching selector header
		this.registry.registerSubscription(subscribeMessage(sessionId1, subscriptionId1, destination, selector1));
		// Register subscription with non-matching selector header
		this.registry.registerSubscription(subscribeMessage(sessionId2, subscriptionId2, destination, selector2));
		// Register subscription without a selector header
		this.registry.registerSubscription(subscribeMessage(sessionId3, subscriptionId3, destination, null));

		// First, try with message WITH selected 'foo' header present

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setDestination(destination);
		accessor.setNativeHeader("foo", "bar");
		Message<?> message = MessageBuilder.createMessage("", accessor.getMessageHeaders());

		MultiValueMap<String, String> subscriptions = this.registry.findSubscriptions(message);

		// 'selector' header is ignored, so all 3 subscriptions should be found
		assertThat(subscriptions).hasSize(3);
		assertThat(subscriptions.get(sessionId1)).containsExactly(subscriptionId1);
		assertThat(subscriptions.get(sessionId2)).containsExactly(subscriptionId2);
		assertThat(subscriptions.get(sessionId3)).containsExactly(subscriptionId3);

		// Then try with message WITHOUT selected 'foo' header present

		subscriptions = this.registry.findSubscriptions(createMessage(destination));

		// 'selector' header is ignored, so all 3 subscriptions should be found
		assertThat(subscriptions).hasSize(3);
		assertThat(subscriptions.get(sessionId1)).containsExactly(subscriptionId1);
		assertThat(subscriptions.get(sessionId2)).containsExactly(subscriptionId2);
		assertThat(subscriptions.get(sessionId3)).containsExactly(subscriptionId3);
	}

	@Test  // SPR-11931
	void registerSubscriptionTwiceAndUnregister() {
		this.registry.registerSubscription(subscribeMessage("sess01", "subs01", "/foo"));
		this.registry.registerSubscription(subscribeMessage("sess01", "subs02", "/foo"));

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(createMessage("/foo"));
		assertThat(actual).hasSize(1);
		assertThat(actual.get("sess01")).containsExactly("subs01", "subs02");

		this.registry.unregisterSubscription(unsubscribeMessage("sess01", "subs01"));

		actual = this.registry.findSubscriptions(createMessage("/foo"));
		assertThat(actual).hasSize(1);
		assertThat(actual.get("sess01")).containsExactly("subs02");

		this.registry.unregisterSubscription(unsubscribeMessage("sess01", "subs02"));

		actual = this.registry.findSubscriptions(createMessage("/foo"));
		assertThat(actual).isEmpty();
	}

	@Test
	void unregisterSubscription() {
		List<String> sessIds = List.of("sess01", "sess02", "sess03");
		List<String> subscriptionIds = List.of("subs01", "subs02", "subs03");
		String dest = "/foo";

		for (String sessId : sessIds) {
			for (String subsId : subscriptionIds) {
				this.registry.registerSubscription(subscribeMessage(sessId, subsId, dest));
			}
		}

		this.registry.unregisterSubscription(unsubscribeMessage(sessIds.get(0), subscriptionIds.get(0)));
		this.registry.unregisterSubscription(unsubscribeMessage(sessIds.get(0), subscriptionIds.get(1)));
		this.registry.unregisterSubscription(unsubscribeMessage(sessIds.get(0), subscriptionIds.get(2)));

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).hasSize(2);
		assertThat(sort(actual.get(sessIds.get(1)))).isEqualTo(subscriptionIds);
		assertThat(sort(actual.get(sessIds.get(2)))).isEqualTo(subscriptionIds);
	}

	@Test
	void unregisterAllSubscriptions() {
		List<String> sessIds = List.of("sess01", "sess02", "sess03");
		List<String> subscriptionIds = List.of("subs01", "subs02", "subs03");
		String dest = "/foo";

		for (String sessId : sessIds) {
			for (String subsId : subscriptionIds) {
				this.registry.registerSubscription(subscribeMessage(sessId, subsId, dest));
			}
		}

		this.registry.unregisterAllSubscriptions(sessIds.get(0));
		this.registry.unregisterAllSubscriptions(sessIds.get(1));

		MultiValueMap<String, String> actual = this.registry.findSubscriptions(createMessage(dest));
		assertThat(actual).hasSize(1);
		assertThat(sort(actual.get(sessIds.get(2)))).isEqualTo(subscriptionIds);
	}

	@Test
	void unregisterAllSubscriptionsNoMatch() {
		this.registry.unregisterAllSubscriptions("bogus");
		// no exceptions
	}

	@Test
	void findSubscriptionsNoMatches() {
		MultiValueMap<String, String> actual = this.registry.findSubscriptions(createMessage("/foo"));
		assertThat(actual).isEmpty();
	}

	@Test  // SPR-12665
	void findSubscriptionsReturnsMapSafeToIterate() {
		this.registry.registerSubscription(subscribeMessage("sess1", "1", "/foo"));
		this.registry.registerSubscription(subscribeMessage("sess2", "1", "/foo"));

		MultiValueMap<String, String> subscriptions = this.registry.findSubscriptions(createMessage("/foo"));
		assertThat(subscriptions).hasSize(2);

		Iterator<Map.Entry<String, List<String>>> iterator = subscriptions.entrySet().iterator();
		iterator.next();

		this.registry.registerSubscription(subscribeMessage("sess3", "1", "/foo"));

		iterator.next();
		// no ConcurrentModificationException
	}

	@Test  // SPR-13185
	void findSubscriptionsReturnsMapSafeToIterateIncludingValues() {
		this.registry.registerSubscription(subscribeMessage("sess1", "1", "/foo"));
		this.registry.registerSubscription(subscribeMessage("sess1", "2", "/foo"));

		MultiValueMap<String, String> allSubscriptions = this.registry.findSubscriptions(createMessage("/foo"));
		assertThat(allSubscriptions).hasSize(1);

		Iterator<String> iteratorValues = allSubscriptions.get("sess1").iterator();
		iteratorValues.next();

		this.registry.unregisterSubscription(unsubscribeMessage("sess1", "2"));

		iteratorValues.next();
		// no ConcurrentModificationException
	}

	@Test // SPR-13555
	void cacheLimitExceeded() {
		this.registry.setCacheLimit(1);
		this.registry.registerSubscription(subscribeMessage("sess1", "1", "/foo"));
		this.registry.registerSubscription(subscribeMessage("sess1", "2", "/bar"));

		assertThat(this.registry.findSubscriptions(createMessage("/foo"))).hasSize(1);
		assertThat(this.registry.findSubscriptions(createMessage("/bar"))).hasSize(1);

		this.registry.registerSubscription(subscribeMessage("sess2", "1", "/foo"));
		this.registry.registerSubscription(subscribeMessage("sess2", "2", "/bar"));

		assertThat(this.registry.findSubscriptions(createMessage("/foo"))).hasSize(2);
		assertThat(this.registry.findSubscriptions(createMessage("/bar"))).hasSize(2);
	}

	private Message<?> createMessage(String destination) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setDestination(destination);
		return MessageBuilder.createMessage("", accessor.getMessageHeaders());
	}

	private Message<?> subscribeMessage(String sessionId, String subscriptionId, String destination) {
		return subscribeMessage(sessionId, subscriptionId, destination, null);
	}

	private Message<?> subscribeMessage(String sessionId, String subscriptionId, String dest, String selector) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
		accessor.setSessionId(sessionId);
		accessor.setSubscriptionId(subscriptionId);
		if (dest != null) {
			accessor.setDestination(dest);
		}
		if (selector != null) {
			accessor.setNativeHeader("selector", selector);
		}
		return MessageBuilder.createMessage("", accessor.getMessageHeaders());
	}

	private Message<?> unsubscribeMessage(String sessionId, String subscriptionId) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
		accessor.setSessionId(sessionId);
		accessor.setSubscriptionId(subscriptionId);
		return MessageBuilder.createMessage("", accessor.getMessageHeaders());
	}

	private List<String> sort(List<String> list) {
		Collections.sort(list);
		return list;
	}

}
