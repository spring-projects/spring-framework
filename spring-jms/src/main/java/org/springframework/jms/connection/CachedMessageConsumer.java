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

package org.springframework.jms.connection;

import guru.mocker.annotation.mixin.Mixin;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.QueueReceiver;
import jakarta.jms.Topic;
import jakarta.jms.TopicSubscriber;
import org.jspecify.annotations.Nullable;

/**
 * JMS MessageConsumer decorator that adapts all calls
 * to a shared MessageConsumer instance underneath.
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 */
@Mixin
class CachedMessageConsumer extends CachedMessageConsumerForwarder implements QueueReceiver, TopicSubscriber {

	public CachedMessageConsumer(MessageConsumer target) {
		super(target);
	}

	@Override
	public @Nullable Queue getQueue() throws JMSException {
		return (target instanceof QueueReceiver receiver ? receiver.getQueue() : null);
	}

	@Override
	public @Nullable Topic getTopic() throws JMSException {
		return (target instanceof TopicSubscriber subscriber ? subscriber.getTopic() : null);
	}

	@Override
	public boolean getNoLocal() throws JMSException {
		return (target instanceof TopicSubscriber subscriber && subscriber.getNoLocal());
	}

	@Override
	public void close() throws JMSException {
		// It's a cached MessageConsumer...
	}

	@Override
	public String toString() {
		return "Cached JMS MessageConsumer: " + target;
	}

}
