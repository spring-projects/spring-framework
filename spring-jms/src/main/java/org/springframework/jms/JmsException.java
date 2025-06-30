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

package org.springframework.jms;

import jakarta.jms.JMSException;
import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;

/**
 * Base class for exception thrown by the framework whenever it
 * encounters a problem related to JMS.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public abstract class JmsException extends NestedRuntimeException {

	/**
	 * Constructor that takes a message.
	 * @param msg the detail message
	 */
	public JmsException(String msg) {
		super(msg);
	}

	/**
	 * Constructor that takes a message and a root cause.
	 * @param msg the detail message
	 * @param cause the cause of the exception. This argument is generally
	 * expected to be a proper subclass of {@link jakarta.jms.JMSException},
	 * but can also be a JNDI NamingException or the like.
	 */
	public JmsException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Constructor that takes a plain root cause, intended for
	 * subclasses mirroring corresponding {@code jakarta.jms} exceptions.
	 * @param cause the cause of the exception. This argument is generally
	 * expected to be a proper subclass of {@link jakarta.jms.JMSException}.
	 */
	public JmsException(@Nullable Throwable cause) {
		super(cause != null ? cause.getMessage() : null, cause);
	}


	/**
	 * Convenience method to get the vendor specific error code if
	 * the root cause was an instance of JMSException.
	 * @return a string specifying the vendor-specific error code if the
	 * root cause is an instance of JMSException, or {@code null}
	 */
	public @Nullable String getErrorCode() {
		Throwable cause = getCause();
		if (cause instanceof JMSException jmsException) {
			return jmsException.getErrorCode();
		}
		return null;
	}

	/**
	 * Return the detail message, including the message from the linked exception
	 * if there is one.
	 * @see jakarta.jms.JMSException#getLinkedException()
	 */
	@Override
	public @Nullable String getMessage() {
		String message = super.getMessage();
		Throwable cause = getCause();
		if (cause instanceof JMSException jmsException) {
			Exception linkedEx = jmsException.getLinkedException();
			if (linkedEx != null) {
				String linkedMessage = linkedEx.getMessage();
				String causeMessage = cause.getMessage();
				if (linkedMessage != null && (causeMessage == null || !causeMessage.contains(linkedMessage))) {
					message = message + "; nested exception is " + linkedEx;
				}
			}
		}
		return message;
	}

}
