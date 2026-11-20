/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS MessageEOFException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.MessageEOFException
 */
@SuppressWarnings("serial")
public class MessageEOFException extends JmsException {

	public MessageEOFException(jakarta.jms.MessageEOFException cause) {
		super(cause);
	}

}
