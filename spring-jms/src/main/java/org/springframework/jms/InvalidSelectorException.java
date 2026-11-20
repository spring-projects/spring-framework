/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS InvalidSelectorException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.InvalidSelectorException
 */
@SuppressWarnings("serial")
public class InvalidSelectorException extends JmsException {

	public InvalidSelectorException(jakarta.jms.InvalidSelectorException cause) {
		super(cause);
	}

}
