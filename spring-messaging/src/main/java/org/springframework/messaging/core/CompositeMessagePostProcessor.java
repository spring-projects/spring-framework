/*
 * Copyright 2002-present the original author or authors.
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
