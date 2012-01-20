/*
 * Copyright 2002-2007 the original author or authors.
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

/**
 * JmsException to be thrown when no other matching subclass found.
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
public class UncategorizedJmsException extends JmsException {

	/**
	 * Constructor that takes a message.
	 * @param msg the detail message
	 */
	public UncategorizedJmsException(String msg) {
		super(msg);
	}

	/**
	 * Constructor that takes a message and a root cause.
	 * @param msg the detail message
	 * @param cause the cause of the exception. This argument is generally
	 * expected to be a proper subclass of {@link javax.jms.JMSException},
	 * but can also be a JNDI NamingException or the like.
	 */
	public UncategorizedJmsException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Constructor that takes a root cause only.
	 * @param cause the cause of the exception. This argument is generally
	 * expected to be a proper subclass of {@link javax.jms.JMSException},
	 * but can also be a JNDI NamingException or the like.
	 */
	public UncategorizedJmsException(Throwable cause) {
		super("Uncategorized exception occured during JMS processing", cause);
	}

}
