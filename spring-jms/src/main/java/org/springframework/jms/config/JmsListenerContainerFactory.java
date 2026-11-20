/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.config;

import org.springframework.jms.listener.MessageListenerContainer;

/**
 * Factory of {@link MessageListenerContainer} based on a
 * {@link JmsListenerEndpoint} definition.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <C> the container type
 * @see JmsListenerEndpoint
 */
public interface JmsListenerContainerFactory<C extends MessageListenerContainer> {

	/**
	 * Create a {@link MessageListenerContainer} for the given {@link JmsListenerEndpoint}.
	 * @param endpoint the endpoint to configure
	 * @return the created container
	 */
	C createListenerContainer(JmsListenerEndpoint endpoint);

}
