/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS IllegalStateException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.IllegalStateException
 */
@SuppressWarnings("serial")
public class IllegalStateException extends JmsException {

	public IllegalStateException(jakarta.jms.IllegalStateException cause) {
		super(cause);
	}

}
