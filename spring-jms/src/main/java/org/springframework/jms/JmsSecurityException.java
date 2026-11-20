/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS JMSSecurityException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.JMSSecurityException
 */
@SuppressWarnings("serial")
public class JmsSecurityException extends JmsException {

	public JmsSecurityException(jakarta.jms.JMSSecurityException cause) {
		super(cause);
	}

}
