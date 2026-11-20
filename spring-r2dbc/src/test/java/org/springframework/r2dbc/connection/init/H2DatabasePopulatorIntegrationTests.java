/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.init;

import java.util.UUID;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;

/**
 * Integration tests for {@link DatabasePopulator} using H2.
 *
 * @author Mark Paluch
 */
class H2DatabasePopulatorIntegrationTests extends AbstractDatabasePopulatorTests {

	UUID databaseName = UUID.randomUUID();

	ConnectionFactory connectionFactory = ConnectionFactories.get(
			"r2dbc:h2:mem:///" + databaseName + "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");


	@Override
	ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

}
