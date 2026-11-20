/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener.adapter;

import org.springframework.jms.JmsException;

/**
 * Exception to be thrown when the reply of a message failed to be sent.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@SuppressWarnings("serial")
public class ReplyFailureException extends JmsException {

	public ReplyFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
