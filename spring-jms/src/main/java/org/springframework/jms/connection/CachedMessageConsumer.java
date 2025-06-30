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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
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
class CachedMessageConsumer implements MessageConsumer, QueueReceiver, TopicSubscriber {

	protected final MessageConsumer target;


	public CachedMessageConsumer(MessageConsumer target) {
		this.target = target;
	}


	@Override
	public String getMessageSelector() throws JMSException {
		return this.target.getMessageSelector();
	}

	@Override
	public @Nullable Queue getQueue() throws JMSException {
		return (this.target instanceof QueueReceiver receiver ? receiver.getQueue() : null);
	}

	@Override
	public @Nullable Topic getTopic() throws JMSException {
		return (this.target instanceof TopicSubscriber subscriber ? subscriber.getTopic() : null);
	}

	@Override
	public boolean getNoLocal() throws JMSException {
		return (this.target instanceof TopicSubscriber subscriber && subscriber.getNoLocal());
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		return this.target.getMessageListener();
	}

	@Override
	public void setMessageListener(MessageListener messageListener) throws JMSException {
		this.target.setMessageListener(messageListener);
	}

	@Override
	public Message receive() throws JMSException {
		return this.target.receive();
	}

	@Override
	public Message receive(long timeout) throws JMSException {
		return this.target.receive(timeout);
	}

	@Override
	public Message receiveNoWait() throws JMSException {
		return this.target.receiveNoWait();
	}

	@Override
	public void close() throws JMSException {
		// It's a cached MessageConsumer...
	}


	@Override
	public String toString() {
		return "Cached JMS MessageConsumer: " + this.target;
	}

}
