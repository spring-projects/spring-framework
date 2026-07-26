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
import jakarta.jms.CompletionListener;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueSender;
import jakarta.jms.Topic;
import jakarta.jms.TopicPublisher;
import org.jspecify.annotations.Nullable;

/**
 * JMS MessageProducer decorator that adapts calls to a shared MessageProducer
 * instance underneath, managing QoS settings locally within the decorator.
 *
 * @author Juergen Hoeller
 * @since 2.5.3
 */
@Mixin
class CachedMessageProducer extends CachedMessageProducerForwarder implements QueueSender, TopicPublisher {


	private @Nullable Boolean originalDisableMessageID;

	private @Nullable Boolean originalDisableMessageTimestamp;

	private @Nullable Long originalDeliveryDelay;

	private int deliveryMode;

	private int priority;

	private long timeToLive;


	public CachedMessageProducer(MessageProducer target) throws JMSException {
		super(target);
		this.deliveryMode = target.getDeliveryMode();
		this.priority = target.getPriority();
		this.timeToLive = target.getTimeToLive();
	}


	@Override
	public void setDisableMessageID(boolean disableMessageID) throws JMSException {
		if (this.originalDisableMessageID == null) {
			this.originalDisableMessageID = super.getDisableMessageID();
		}
		super.setDisableMessageID(disableMessageID);
	}

	@Override
	public void setDisableMessageTimestamp(boolean disableMessageTimestamp) throws JMSException {
		if (this.originalDisableMessageTimestamp == null) {
			this.originalDisableMessageTimestamp = super.getDisableMessageTimestamp();
		}
		super.setDisableMessageTimestamp(disableMessageTimestamp);
	}

	@Override
	public void setDeliveryDelay(long deliveryDelay) throws JMSException {
		if (this.originalDeliveryDelay == null) {
			this.originalDeliveryDelay = super.getDeliveryDelay();
		}
		super.setDeliveryDelay(deliveryDelay);
	}

	@Override
	public void setDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	@Override
	public int getDeliveryMode() {
		return this.deliveryMode;
	}

	@Override
	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

	@Override
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	@Override
	public long getTimeToLive() {
		return this.timeToLive;
	}

	@Override
	public Queue getQueue() throws JMSException {
		return (Queue) super.getDestination();
	}

	@Override
	public Topic getTopic() throws JMSException {
		return (Topic) super.getDestination();
	}

	@Override
	public void send(Message message) throws JMSException {
		super.send(message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void send(Destination destination, Message message) throws JMSException {
		super.send(destination, message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void send(Message message, CompletionListener completionListener) throws JMSException {
		super.send(message, this.deliveryMode, this.priority, this.timeToLive, completionListener);
	}

	@Override
	public void send(Destination destination, Message message, CompletionListener completionListener) throws JMSException {
		super.send(destination, message, this.deliveryMode, this.priority, this.timeToLive, completionListener);
	}

	@Override
	public void send(Queue queue, Message message) throws JMSException {
		super.send(queue, message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void publish(Message message) throws JMSException {
		super.send(message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void publish(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		super.send(message, deliveryMode, priority, timeToLive);
	}

	@Override
	public void publish(Topic topic, Message message) throws JMSException {
		super.send(topic, message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void publish(Topic topic, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		super.send(topic, message, deliveryMode, priority, timeToLive);
	}

	@Override
	public void close() throws JMSException {
		// It's a cached MessageProducer... reset properties only.
		if (this.originalDisableMessageID != null) {
			super.setDisableMessageID(this.originalDisableMessageID);
			this.originalDisableMessageID = null;
		}
		if (this.originalDisableMessageTimestamp != null) {
			super.setDisableMessageTimestamp(this.originalDisableMessageTimestamp);
			this.originalDisableMessageTimestamp = null;
		}
		if (this.originalDeliveryDelay != null) {
			super.setDeliveryDelay(this.originalDeliveryDelay);
			this.originalDeliveryDelay = null;
		}
	}

	@Override
	public String toString() {
		return "Cached JMS MessageProducer: " + target;
	}

}
