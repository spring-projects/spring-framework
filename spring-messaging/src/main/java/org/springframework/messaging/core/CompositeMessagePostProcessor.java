/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.core;

import java.util.List;

import org.springframework.messaging.Message;

/**
 * Composite {@link MessagePostProcessor} implementation that iterates over
 * a given collection of delegate {@link MessagePostProcessor} instances.
 * @author Brian Clozel
 * @since 7.0
 */
public class CompositeMessagePostProcessor implements MessagePostProcessor {

	private final List<MessagePostProcessor> messagePostProcessors;

	/**
	 * Construct a CompositeMessagePostProcessor from the given delegate MessagePostProcessors.
	 * @param messagePostProcessors the MessagePostProcessors to delegate to
	 */
	public CompositeMessagePostProcessor(List<MessagePostProcessor> messagePostProcessors) {
		this.messagePostProcessors = messagePostProcessors;
	}

	@Override
	public Message<?> postProcessMessage(Message<?> message) {
		for (MessagePostProcessor messagePostProcessor : this.messagePostProcessors) {
			message = messagePostProcessor.postProcessMessage(message);
		}
		return message;
	}
}
