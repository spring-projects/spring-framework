/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.broker;

import org.springframework.messaging.Message;
import org.springframework.util.MultiValueMap;

/**
 * A registry of subscription by session that allows looking up subscriptions.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SubscriptionRegistry {

	/**
	 * Register a subscription represented by the given message.
	 * @param subscribeMessage the subscription request
	 */
	void registerSubscription(Message<?> subscribeMessage);

	/**
	 * Unregister a subscription.
	 * @param unsubscribeMessage the request to unsubscribe
	 */
	void unregisterSubscription(Message<?> unsubscribeMessage);

	/**
	 * Remove all subscriptions associated with the given sessionId.
	 */
	void unregisterAllSubscriptions(String sessionId);

	/**
	 * Find all subscriptions that should receive the given message.
	 * The map returned is safe to iterate and will never be modified.
	 * @param message the message
	 * @return a {@code MultiValueMap} with sessionId-subscriptionId pairs
	 * (possibly empty)
	 */
	MultiValueMap<String, String> findSubscriptions(Message<?> message);

}
