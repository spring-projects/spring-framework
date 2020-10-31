/*
 * Copyright 2002-2019 the original author or authors.
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

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript;

/**
 * Integration tests for {@link ScriptUtils}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 * @see ScriptUtilsUnitTests
 */
public class ScriptUtilsIntegrationTests extends AbstractDatabaseInitializationTests {

	@Override
	protected EmbeddedDatabaseType getEmbeddedDatabaseType() {
		return EmbeddedDatabaseType.HSQL;
	}

	@BeforeEach
	public void setUpSchema() throws SQLException {
		executeSqlScript(db.getConnection(), usersSchema());
	}

	@Test
	public void executeSqlScriptContainingMultiLineComments() throws SQLException {
		executeSqlScript(db.getConnection(), resource("test-data-with-multi-line-comments.sql"));
		assertUsersDatabaseCreated("Hoeller", "Brannen");
	}

	/**
	 * @since 4.2
	 */
	@Test
	public void executeSqlScriptContainingSingleQuotesNestedInsideDoubleQuotes() throws SQLException {
		executeSqlScript(db.getConnection(), resource("users-data-with-single-quotes-nested-in-double-quotes.sql"));
		assertUsersDatabaseCreated("Hoeller", "Brannen");
	}

}
