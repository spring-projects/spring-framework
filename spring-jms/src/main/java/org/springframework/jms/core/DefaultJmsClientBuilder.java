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

package org.springframework.jms.core;

import java.util.ArrayList;
import java.util.List;

import jakarta.jms.ConnectionFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.CompositeMessagePostProcessor;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link JmsClient.Builder}.
 * @author Brian Clozel
 * @since 7.0
 * @see JmsClient#builder(ConnectionFactory)
 * @see JmsClient#builder(JmsOperations)
 */
class DefaultJmsClientBuilder implements JmsClient.Builder {

	private final DefaultJmsClient jmsClient;

	private @Nullable List<MessageConverter> messageConverters;

	private @Nullable List<MessagePostProcessor> messagePostProcessors;


	DefaultJmsClientBuilder(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.jmsClient = new DefaultJmsClient(connectionFactory);
	}

	DefaultJmsClientBuilder(JmsOperations jmsTemplate) {
		Assert.notNull(jmsTemplate, "JmsOperations must not be null");
		this.jmsClient = new DefaultJmsClient(jmsTemplate);
	}

	@Override
	public JmsClient.Builder messageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		if (this.messageConverters == null) {
			this.messageConverters = new ArrayList<>();
		}
		this.messageConverters.add(messageConverter);
		return this;
	}

	@Override
	public JmsClient.Builder messagePostProcessor(MessagePostProcessor messagePostProcessor) {
		Assert.notNull(messagePostProcessor, "MessagePostProcessor must not be null");
		if (this.messagePostProcessors == null) {
			this.messagePostProcessors = new ArrayList<>();
		}
		this.messagePostProcessors.add(messagePostProcessor);
		return this;
	}

	@Override
	public JmsClient build() {
		if (this.messageConverters != null) {
			this.jmsClient.setMessageConverter(new CompositeMessageConverter(this.messageConverters));
		}
		if (this.messagePostProcessors != null) {
			this.jmsClient.setMessagePostProcessor(new CompositeMessagePostProcessor(this.messagePostProcessors));
		}
		return this.jmsClient;
	}

}
