/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener;

import org.jspecify.annotations.Nullable;

import org.springframework.context.SmartLifecycle;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * Internal abstraction used by the framework representing a message
 * listener container. Not meant to be implemented externally with
 * support for both JMS and JCA style containers.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface MessageListenerContainer extends SmartLifecycle {

	/**
	 * Set up the message listener to use. Throws an {@link IllegalArgumentException}
	 * if that message listener type is not supported.
	 */
	void setupMessageListener(Object messageListener);

	/**
	 * Return the {@link MessageConverter} that can be used to
	 * convert {@link jakarta.jms.Message}, if any.
	 */
	@Nullable MessageConverter getMessageConverter();

	/**
	 * Return the {@link DestinationResolver} to use to resolve
	 * destinations by names.
	 */
	@Nullable DestinationResolver getDestinationResolver();

	/**
	 * Return whether the Publish/Subscribe domain ({@link jakarta.jms.Topic Topics}) is used.
	 * Otherwise, the Point-to-Point domain ({@link jakarta.jms.Queue Queues}) is used.
	 */
	boolean isPubSubDomain();

	/**
	 * Return whether the reply destination uses Publish/Subscribe domain
	 * ({@link jakarta.jms.Topic Topics}). Otherwise, the Point-to-Point domain
	 * ({@link jakarta.jms.Queue Queues}) is used.
	 * <p>By default, the value is identical to {@link #isPubSubDomain()}.
	 */
	boolean isReplyPubSubDomain();

	/**
	 * Return the {@link QosSettings} to use when sending a reply,
	 * or {@code null} if the broker's defaults should be used.
	 * @since 5.0
	 */
	@Nullable QosSettings getReplyQosSettings();

}
