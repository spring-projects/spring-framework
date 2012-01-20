/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.jms;

import javax.jms.JMSException;

import org.springframework.core.NestedRuntimeException;

/**
 * Base class for exception thrown by the framework whenever it
 * encounters a problem related to JMS.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 */
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
	 * expected to be a proper subclass of {@link javax.jms.JMSException},
	 * but can also be a JNDI NamingException or the like.
	 */
	public JmsException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Constructor that takes a plain root cause, intended for
	 * subclasses mirroring corresponding <code>javax.jms</code> exceptions.
	 * @param cause the cause of the exception. This argument is generally
	 * expected to be a proper subclass of {@link javax.jms.JMSException}.
	 */
	public JmsException(Throwable cause) {
		super(cause != null ? cause.getMessage() : null, cause);
	}


	/**
	 * Convenience method to get the vendor specific error code if
	 * the root cause was an instance of JMSException.
	 * @return a string specifying the vendor-specific error code if the
	 * root cause is an instance of JMSException, or <code>null</code>
	 */
	public String getErrorCode() {
		Throwable cause = getCause();
		if (cause instanceof JMSException) {
			return ((JMSException) cause).getErrorCode();
		}
		return null;
	}

	/**
	 * Return the detail message, including the message from the linked exception
	 * if there is one.
	 * @see javax.jms.JMSException#getLinkedException()
	 */
	public String getMessage() {
		String message = super.getMessage();
		Throwable cause = getCause();
		if (cause instanceof JMSException) {
			Exception linkedEx = ((JMSException) cause).getLinkedException();
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
