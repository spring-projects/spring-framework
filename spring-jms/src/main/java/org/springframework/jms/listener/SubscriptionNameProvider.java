/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener;

/**
 * Interface to be implemented by message listener objects that suggest a specific
 * name for a durable subscription that they might be registered with. Otherwise,
 * the listener class name will be used as a default subscription name.
 *
 * <p>Applies to {@link jakarta.jms.MessageListener} objects as well as to
 * {@link SessionAwareMessageListener} objects and plain listener methods
 * (as supported by {@link org.springframework.jms.listener.adapter.MessageListenerAdapter}).
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 */
public interface SubscriptionNameProvider {

	/**
	 * Determine the subscription name for this message listener object.
	 */
	String getSubscriptionName();

}
