/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Strategy used to populate a database during initialization.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 * @see ResourceDatabasePopulator
 * @see DatabasePopulatorUtils
 */
public interface DatabasePopulator {

	/**
	 * Populate the database using the JDBC connection provided.
	 * @param connection the JDBC connection to use to populate the db; already
	 * configured and ready to use
	 * @throws SQLException if an unrecoverable data access exception occurs
	 * during database population
	 * @throws ScriptException in all other error cases
	 * @see DatabasePopulatorUtils#execute
	 */
	void populate(Connection connection) throws SQLException, ScriptException;

}
