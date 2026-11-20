/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.notification;

import org.springframework.jmx.JmxException;

/**
 * Thrown when a JMX {@link javax.management.Notification} is unable to be sent.
 *
 * <p>The root cause of just why a particular notification could not be sent
 * will <i>typically</i> be available via the {@link #getCause()} property.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see NotificationPublisher
 */
@SuppressWarnings("serial")
public class UnableToSendNotificationException extends JmxException {

	/**
	 * Create a new instance of the {@link UnableToSendNotificationException}
	 * class with the specified error message.
	 * @param msg the detail message
	 */
	public UnableToSendNotificationException(String msg) {
		super(msg);
	}

	/**
	 * Create a new instance of the {@link UnableToSendNotificationException}
	 * with the specified error message and root cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public UnableToSendNotificationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
