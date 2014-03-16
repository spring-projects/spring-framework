/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource.init;

import java.sql.SQLException;

import org.junit.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.springframework.jdbc.datasource.init.ScriptUtils.*;

/**
 * Integration tests for {@link ScriptUtils}.
 *
 * @author Sam Brannen
 * @see ScriptUtilsUnitTests
 * @since 4.0.3
 */
public class ScriptUtilsIntegrationTests extends AbstractDatabaseInitializationTests {

	protected EmbeddedDatabaseType getEmbeddedDatabaseType() {
		return EmbeddedDatabaseType.HSQL;
	}

	@Test
	public void executeSqlScriptContainingMuliLineComments() throws SQLException {
		executeSqlScript(db.getConnection(), usersSchema());
		executeSqlScript(db.getConnection(), resource("test-data-with-multi-line-comments.sql"));

		assertUsersDatabaseCreated("Hoeller", "Brannen");
	}

}
