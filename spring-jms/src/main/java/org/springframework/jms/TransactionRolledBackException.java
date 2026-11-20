/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS TransactionRolledBackException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.TransactionRolledBackException
 */
@SuppressWarnings("serial")
public class TransactionRolledBackException extends JmsException {

	public TransactionRolledBackException(jakarta.jms.TransactionRolledBackException cause) {
		super(cause);
	}

}
