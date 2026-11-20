/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.config;

import org.springframework.jms.listener.SimpleMessageListenerContainer;

/**
 * A {@link JmsListenerContainerFactory} implementation to build a
 * standard {@link SimpleMessageListenerContainer}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class SimpleJmsListenerContainerFactory
		extends AbstractJmsListenerContainerFactory<SimpleMessageListenerContainer> {

	@Override
	protected SimpleMessageListenerContainer createContainerInstance() {
		return new SimpleMessageListenerContainer();
	}

}
