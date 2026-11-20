/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.config;

import org.springframework.jms.listener.MessageListenerContainer;

/**
 * Model for a JMS listener endpoint. Can be used against a
 * {@link org.springframework.jms.annotation.JmsListenerConfigurer
 * JmsListenerConfigurer} to register endpoints programmatically.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface JmsListenerEndpoint {

	/**
	 * Return the id of this endpoint.
	 */
	String getId();

	/**
	 * Set up the specified message listener container with the model
	 * defined by this endpoint.
	 * <p>This endpoint must provide the requested missing option(s) of
	 * the specified container to make it usable. Usually, this is about
	 * setting the {@code destination} and the {@code messageListener} to
	 * use but an implementation may override any default setting that
	 * was already set.
	 * @param listenerContainer the listener container to configure
	 */
	void setupListenerContainer(MessageListenerContainer listenerContainer);

}
