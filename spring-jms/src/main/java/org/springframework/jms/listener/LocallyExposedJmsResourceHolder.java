/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener;

import jakarta.jms.Session;

import org.springframework.jms.connection.JmsResourceHolder;

/**
 * {@link JmsResourceHolder} marker subclass that indicates local exposure,
 * i.e. that does not indicate an externally managed transaction.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
class LocallyExposedJmsResourceHolder extends JmsResourceHolder {

	public LocallyExposedJmsResourceHolder(Session session) {
		super(session);
	}

}
