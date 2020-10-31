/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.r2dbc.core;

import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;

/**
 * Integration tests for {@link DatabaseClient} against H2.
 *
 * @author Mark Paluch
 */
public class H2DatabaseClientIntegrationTests
		extends AbstractDatabaseClientIntegrationTests {

	public static String CREATE_TABLE_LEGOSET = "CREATE TABLE legoset (\n" //
			+ "    id          serial CONSTRAINT id PRIMARY KEY,\n" //
			+ "    version     integer NULL,\n" //
			+ "    name        varchar(255) NOT NULL,\n" //
			+ "    manual      integer NULL\n" //
			+ ");";

	@Override
	protected ConnectionFactory createConnectionFactory() {
		return H2ConnectionFactory.inMemory("r2dbc-test");
	}

	@Override
	protected String getCreateTableStatement() {
		return CREATE_TABLE_LEGOSET;
	}

}
