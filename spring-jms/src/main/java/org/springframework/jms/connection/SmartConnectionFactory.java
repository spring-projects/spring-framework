/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.connection;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;

/**
 * Extension of the {@code jakarta.jms.ConnectionFactory} interface,
 * indicating how to release Connections obtained from it.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 */
public interface SmartConnectionFactory extends ConnectionFactory {

	/**
	 * Should we stop the Connection, obtained from this ConnectionFactory?
	 * @param con the Connection to check
	 * @return whether a stop call is necessary
	 * @see jakarta.jms.Connection#stop()
	 */
	boolean shouldStop(Connection con);

}
