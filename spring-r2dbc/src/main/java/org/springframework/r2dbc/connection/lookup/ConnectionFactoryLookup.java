/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.lookup;

import io.r2dbc.spi.ConnectionFactory;

/**
 * Strategy interface for looking up {@link ConnectionFactory} by name.
 *
 * @author Mark Paluch
 * @since 5.3
 */
@FunctionalInterface
public interface ConnectionFactoryLookup {

	/**
	 * Retrieve the {@link ConnectionFactory} identified by the given name.
	 * @param connectionFactoryName the name of the {@link ConnectionFactory}
	 * @return the {@link ConnectionFactory} (never {@code null})
	 * @throws ConnectionFactoryLookupFailureException if the lookup failed
	 */
	ConnectionFactory getConnectionFactory(String connectionFactoryName)
			throws ConnectionFactoryLookupFailureException;

}
