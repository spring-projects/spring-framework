/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS MessageFormatException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.MessageFormatException
 */
@SuppressWarnings("serial")
public class MessageFormatException extends JmsException {

	public MessageFormatException(jakarta.jms.MessageFormatException cause) {
		super(cause);
	}

}
