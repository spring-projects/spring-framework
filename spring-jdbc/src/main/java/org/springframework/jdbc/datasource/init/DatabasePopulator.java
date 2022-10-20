/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Strategy used to populate, initialize, or clean up a database.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 * @see ResourceDatabasePopulator
 * @see DatabasePopulatorUtils
 * @see DataSourceInitializer
 */
@FunctionalInterface
public interface DatabasePopulator {

	/**
	 * Populate, initialize, or clean up the database using the provided JDBC
	 * connection.
	 * <p><strong>Warning</strong>: Concrete implementations should not close
	 * the provided {@link Connection}.
	 * <p>Concrete implementations <em>may</em> throw an {@link SQLException} if
	 * an error is encountered but are <em>strongly encouraged</em> to throw a
	 * specific {@link ScriptException} instead. For example, Spring's
	 * {@link ResourceDatabasePopulator} and {@link DatabasePopulatorUtils} wrap
	 * all {@code SQLExceptions} in {@code ScriptExceptions}.
	 * @param connection the JDBC connection to use; already configured and
	 * ready to use; never {@code null}
	 * @throws SQLException if an unrecoverable data access exception occurs
	 * while interacting with the database
	 * @throws ScriptException in all other error cases
	 * @see DatabasePopulatorUtils#execute
	 */
	void populate(Connection connection) throws SQLException, ScriptException;

}
