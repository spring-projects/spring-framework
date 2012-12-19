/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.mail;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.ObjectUtils;

/**
 * Exception thrown when a mail sending error is encountered.
 * Can register failed messages with their exceptions.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class MailSendException extends MailException {

	private transient final Map<Object, Exception> failedMessages;

	private Exception[] messageExceptions;


	/**
	 * Constructor for MailSendException.
	 * @param msg the detail message
	 */
	public MailSendException(String msg) {
		this(msg, null);
	}

	/**
	 * Constructor for MailSendException.
	 * @param msg the detail message
	 * @param cause the root cause from the mail API in use
	 */
	public MailSendException(String msg, Throwable cause) {
		super(msg, cause);
		this.failedMessages = new LinkedHashMap<Object, Exception>();
	}

	/**
	 * Constructor for registration of failed messages, with the
	 * messages that failed as keys, and the thrown exceptions as values.
	 * <p>The messages should be the same that were originally passed
	 * to the invoked send method.
	 * @param msg the detail message
	 * @param cause the root cause from the mail API in use
	 * @param failedMessages Map of failed messages as keys and thrown
	 * exceptions as values
	 */
	public MailSendException(String msg, Throwable cause, Map<Object, Exception> failedMessages) {
		super(msg, cause);
		this.failedMessages = new LinkedHashMap<Object, Exception>(failedMessages);
		this.messageExceptions = failedMessages.values().toArray(new Exception[failedMessages.size()]);
	}

	/**
	 * Constructor for registration of failed messages, with the
	 * messages that failed as keys, and the thrown exceptions as values.
	 * <p>The messages should be the same that were originally passed
	 * to the invoked send method.
	 * @param failedMessages Map of failed messages as keys and thrown
	 * exceptions as values
	 */
	public MailSendException(Map<Object, Exception> failedMessages) {
		this(null, null, failedMessages);
	}


	/**
	 * Return a Map with the failed messages as keys, and the thrown exceptions
	 * as values.
	 * <p>Note that a general mail server connection failure will not result
	 * in failed messages being returned here: A message will only be
	 * contained here if actually sending it was attempted but failed.
	 * <p>The messages will be the same that were originally passed to the
	 * invoked send method, that is, SimpleMailMessages in case of using
	 * the generic MailSender interface.
	 * <p>In case of sending MimeMessage instances via JavaMailSender,
	 * the messages will be of type MimeMessage.
	 * <p><b>NOTE:</b> This Map will not be available after serialization.
	 * Use {@link #getMessageExceptions()} in such a scenario, which will
	 * be available after serialization as well.
	 * @return the Map of failed messages as keys and thrown exceptions as values
	 * @see SimpleMailMessage
	 * @see javax.mail.internet.MimeMessage
	 */
	public final Map<Object, Exception> getFailedMessages() {
		return this.failedMessages;
	}

	/**
	 * Return an array with thrown message exceptions.
	 * <p>Note that a general mail server connection failure will not result
	 * in failed messages being returned here: A message will only be
	 * contained here if actually sending it was attempted but failed.
	 * @return the array of thrown message exceptions,
	 * or an empty array if no failed messages
	 */
	public final Exception[] getMessageExceptions() {
		return (this.messageExceptions != null ? this.messageExceptions : new Exception[0]);
	}


	@Override
	public String getMessage() {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			return super.getMessage();
		}
		else {
			StringBuilder sb = new StringBuilder();
			String baseMessage = super.getMessage();
			if (baseMessage != null) {
				sb.append(baseMessage).append(". ");
			}
			sb.append("Failed messages: ");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				sb.append(subEx.toString());
				if (i < this.messageExceptions.length - 1) {
					sb.append("; ");
				}
			}
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			return super.toString();
		}
		else {
			StringBuilder sb = new StringBuilder(super.toString());
			sb.append("; message exceptions (").append(this.messageExceptions.length).append(") are:");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				sb.append('\n').append("Failed message ").append(i + 1).append(": ");
				sb.append(subEx);
			}
			return sb.toString();
		}
	}

	@Override
	public void printStackTrace(PrintStream ps) {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			super.printStackTrace(ps);
		}
		else {
			ps.println(super.toString() + "; message exception details (" +
					this.messageExceptions.length + ") are:");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				ps.println("Failed message " + (i + 1) + ":");
				subEx.printStackTrace(ps);
			}
		}
	}

	@Override
	public void printStackTrace(PrintWriter pw) {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			super.printStackTrace(pw);
		}
		else {
			pw.println(super.toString() + "; message exception details (" +
					this.messageExceptions.length + ") are:");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				pw.println("Failed message " + (i + 1) + ":");
				subEx.printStackTrace(pw);
			}
		}
	}

}
