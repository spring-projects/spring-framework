/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jms.support;

import javax.jms.Message;

/**
 * Gather the Quality-of-Service settings that can be used when sending a message.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
public class QosSettings {

	private int deliveryMode;

	private int priority;

	private long timeToLive;


	/**
	 * Create a new instance with the default settings.
	 * @see Message#DEFAULT_DELIVERY_MODE
	 * @see Message#DEFAULT_PRIORITY
	 * @see Message#DEFAULT_TIME_TO_LIVE
	 */
	public QosSettings() {
		this(Message.DEFAULT_DELIVERY_MODE, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
	}

	/**
	 * Create a new instance with the specified settings.
	 */
	public QosSettings(int deliveryMode, int priority, long timeToLive) {
		this.deliveryMode = deliveryMode;
		this.priority = priority;
		this.timeToLive = timeToLive;
	}


	/**
	 * Set the delivery mode to use when sending a message.
	 * Default is the JMS Message default: "PERSISTENT".
	 * @param deliveryMode the delivery mode to use
	 * @see javax.jms.DeliveryMode#PERSISTENT
	 * @see javax.jms.DeliveryMode#NON_PERSISTENT
	 * @see javax.jms.Message#DEFAULT_DELIVERY_MODE
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	/**
	 * Return the delivery mode to use when sending a message.
	 */
	public int getDeliveryMode() {
		return this.deliveryMode;
	}

	/**
	 * Set the priority of a message when sending.
	 * @see javax.jms.Message#DEFAULT_PRIORITY
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Return the priority of a message when sending.
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * Set the time-to-live of the message when sending.
	 * @param timeToLive the message's lifetime (in milliseconds)
	 * @see javax.jms.Message#DEFAULT_TIME_TO_LIVE
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * Return the time-to-live of the message when sending.
	 */
	public long getTimeToLive() {
		return this.timeToLive;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof QosSettings)) {
			return false;
		}

		QosSettings otherSettings = (QosSettings) other;
		return (this.deliveryMode == otherSettings.deliveryMode &&
				this.priority == otherSettings.priority &&
				this.timeToLive == otherSettings.timeToLive);
	}

	@Override
	public int hashCode() {
		return (this.deliveryMode * 31 + this.priority);
	}

	@Override
	public String toString() {
		return "QosSettings{" + "deliveryMode=" + this.deliveryMode +
				", priority=" + this.priority + ", timeToLive=" + this.timeToLive + '}';
	}
}
