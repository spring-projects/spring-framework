/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS MessageNotReadableException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.MessageNotReadableException
 */
@SuppressWarnings("serial")
public class MessageNotReadableException extends JmsException {

	public MessageNotReadableException(jakarta.jms.MessageNotReadableException cause) {
		super(cause);
	}

}
