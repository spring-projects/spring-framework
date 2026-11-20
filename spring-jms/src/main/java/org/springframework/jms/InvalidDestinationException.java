/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS InvalidDestinationException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.InvalidDestinationException
 */
@SuppressWarnings("serial")
public class InvalidDestinationException extends JmsException {

	public InvalidDestinationException(jakarta.jms.InvalidDestinationException cause) {
		super(cause);
	}

}
