/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS MessageNotWriteableException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.MessageNotWriteableException
 */
@SuppressWarnings("serial")
public class MessageNotWriteableException extends JmsException {

	public MessageNotWriteableException(jakarta.jms.MessageNotWriteableException cause) {
		super(cause);
	}

}
