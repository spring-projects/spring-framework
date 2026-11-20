/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.support;

import jakarta.jms.Message;
import org.jspecify.annotations.Nullable;

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
	 * @see jakarta.jms.DeliveryMode#PERSISTENT
	 * @see jakarta.jms.DeliveryMode#NON_PERSISTENT
	 * @see jakarta.jms.Message#DEFAULT_DELIVERY_MODE
	 * @see jakarta.jms.MessageProducer#send(jakarta.jms.Message, int, int, long)
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
	 * @see jakarta.jms.Message#DEFAULT_PRIORITY
	 * @see jakarta.jms.MessageProducer#send(jakarta.jms.Message, int, int, long)
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
	 * @see jakarta.jms.Message#DEFAULT_TIME_TO_LIVE
	 * @see jakarta.jms.MessageProducer#send(jakarta.jms.Message, int, int, long)
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
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof QosSettings that &&
				this.deliveryMode == that.deliveryMode &&
				this.priority == that.priority &&
				this.timeToLive == that.timeToLive));
	}

	@Override
	public int hashCode() {
		return this.deliveryMode * 31 + this.priority;
	}

	@Override
	public String toString() {
		return "QosSettings{" + "deliveryMode=" + this.deliveryMode +
				", priority=" + this.priority + ", timeToLive=" + this.timeToLive + '}';
	}
}
