/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.lookup;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.util.Assert;

/**
 * An implementation of {@link ConnectionFactoryLookup} that
 * simply wraps a single given {@link ConnectionFactory}
 * returned for any connection factory name.
 *
 * @author Mark Paluch
 * @since 5.3
 */
public class SingleConnectionFactoryLookup implements ConnectionFactoryLookup {

	private final ConnectionFactory connectionFactory;


	/**
	 * Create a new instance of the {@link SingleConnectionFactoryLookup} class.
	 * @param connectionFactory the single {@link ConnectionFactory} to wrap
	 */
	public SingleConnectionFactoryLookup(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.connectionFactory = connectionFactory;
	}


	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName)
			throws ConnectionFactoryLookupFailureException {

		return this.connectionFactory;
	}

}
