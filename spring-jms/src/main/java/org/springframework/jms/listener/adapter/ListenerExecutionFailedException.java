/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener.adapter;

import org.springframework.jms.JmsException;

/**
 * Exception to be thrown when the execution of a listener method failed.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see MessageListenerAdapter
 */
@SuppressWarnings("serial")
public class ListenerExecutionFailedException extends JmsException {

	/**
	 * Constructor for ListenerExecutionFailedException.
	 * @param msg the detail message
	 * @param cause the exception thrown by the listener method
	 */
	public ListenerExecutionFailedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
