/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS TransactionInProgressException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.TransactionInProgressException
 */
@SuppressWarnings("serial")
public class TransactionInProgressException extends JmsException {

	public TransactionInProgressException(jakarta.jms.TransactionInProgressException cause) {
		super(cause);
	}

}
