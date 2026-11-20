/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;

/**
 * Integration tests for {@link DatabaseClient} against H2.
 *
 * @author Mark Paluch
 */
class H2DatabaseClientIntegrationTests extends AbstractDatabaseClientIntegrationTests {

	private static final String CREATE_TABLE_LEGOSET = """
			CREATE TABLE legoset (
			id          serial CONSTRAINT id PRIMARY KEY,
			version     integer NULL,
			name        varchar(255) NOT NULL,
			manual      integer NULL
			);""";

	@Override
	protected ConnectionFactory createConnectionFactory() {
		return H2ConnectionFactory.inMemory("r2dbc-test");
	}

	@Override
	protected String getCreateTableStatement() {
		return CREATE_TABLE_LEGOSET;
	}

}
