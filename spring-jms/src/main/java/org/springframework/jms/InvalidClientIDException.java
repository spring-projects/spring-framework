/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS InvalidClientIDException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.InvalidClientIDException
 */
@SuppressWarnings("serial")
public class InvalidClientIDException extends JmsException {

	public InvalidClientIDException(jakarta.jms.InvalidClientIDException cause) {
		super(cause);
	}

}
