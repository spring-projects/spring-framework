/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging;

/**
 * The base exception for any failures related to messaging.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MessagingException extends RuntimeException {

	private volatile Message<?> failedMessage;


	public MessagingException(Message<?> message) {
		super();
		this.failedMessage = message;
	}

	public MessagingException(String description) {
		super(description);
		this.failedMessage = null;
	}

	public MessagingException(String description, Throwable cause) {
		super(description, cause);
		this.failedMessage = null;
	}

	public MessagingException(Message<?> message, String description) {
		super(description);
		this.failedMessage = message;
	}

	public MessagingException(Message<?> message, Throwable cause) {
		super(cause);
		this.failedMessage = message;
	}

	public MessagingException(Message<?> message, String description, Throwable cause) {
		super(description, cause);
		this.failedMessage = message;
	}

	public Message<?> getFailedMessage() {
		return this.failedMessage;
	}

	public void setFailedMessage(Message<?> message) {
		this.failedMessage = message;
	}

}
