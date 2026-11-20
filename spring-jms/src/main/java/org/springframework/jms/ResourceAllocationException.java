/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS ResourceAllocationException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see jakarta.jms.ResourceAllocationException
 */
@SuppressWarnings("serial")
public class ResourceAllocationException extends JmsException {

	public ResourceAllocationException(jakarta.jms.ResourceAllocationException cause) {
		super(cause);
	}

}
