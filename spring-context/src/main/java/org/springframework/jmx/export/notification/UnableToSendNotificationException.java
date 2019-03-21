/*
 * Copyright 2002-2012 the original author or authors.
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
