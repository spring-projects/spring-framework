/*
 * Copyright 2002-present the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript;

/**
 * Integration tests for {@link ScriptUtils}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 * @see ScriptUtilsTests
 */
@ParameterizedClass
@EnumSource(EmbeddedDatabaseType.class)
class ScriptUtilsIntegrationTests extends AbstractDatabaseInitializationTests {

	@Parameter
	EmbeddedDatabaseType databaseType;


	@Override
	protected EmbeddedDatabaseType getEmbeddedDatabaseType() {
		return this.databaseType;
	}

	@BeforeEach
	void setUpSchema() throws SQLException {
		executeSqlScript(db.getConnection(), encodedResource(usersSchema()), false, true, "--", null, "/*", "*/");
	}

	@Test
	void executeSqlScriptContainingMultiLineComments() throws SQLException {
		executeSqlScript(db.getConnection(), resource("test-data-with-multi-line-comments.sql"));
		assertUsersDatabaseCreated("Hoeller", "Brannen");
	}

	/**
	 * @since 4.2
	 */
	@Test
	void executeSqlScriptContainingSingleQuotesNestedInsideDoubleQuotes() throws SQLException {
		executeSqlScript(db.getConnection(), resource("users-data-with-single-quotes-nested-in-double-quotes.sql"));
		assertUsersDatabaseCreated("Hoeller", "Brannen");
	}

	@Test
	@SuppressWarnings("unchecked")
	void statementWithMultipleResultSets() throws SQLException {
		// Derby does not support multiple statements/ResultSets within a single Statement.
		assumeThat(this.databaseType).isNotSameAs(EmbeddedDatabaseType.DERBY);

		EncodedResource resource = encodedResource(resource("users-data.sql"));
		executeSqlScript(db.getConnection(), resource, false, true, "--", null, "/*", "*/");

		assertUsersInDatabase(user("Sam", "Brannen"));

		resource = encodedResource(inlineResource("""
				SELECT last_name FROM users WHERE id = 0;
				UPDATE users SET first_name = 'Jane' WHERE id = 0;
				UPDATE users SET last_name = 'Smith' WHERE id = 0;
				SELECT last_name FROM users WHERE id = 0;
				GO
				"""));

		String separator = "GO\n";
		executeSqlScript(db.getConnection(), resource, false, true, "--", separator, "/*", "*/");

		assertUsersInDatabase(user("Jane", "Smith"));
	}

	private void assertUsersInDatabase(User... expectedUsers) {
		List<User> users = jdbcTemplate.query("SELECT * FROM users WHERE id = 0",
				new DataClassRowMapper<>(User.class));
		assertThat(users).containsExactly(expectedUsers);
	}


	private static EncodedResource encodedResource(Resource resource) {
		return new EncodedResource(resource);
	}

	private static Resource inlineResource(String sql) {
		byte[] bytes = sql.getBytes(StandardCharsets.UTF_8);
		return new ByteArrayResource(bytes, "inline SQL");
	}

	private static User user(String firstName, String lastName) {
		return new User(0, firstName, lastName);
	}

	record User(int id, String firstName, String lastName) {
	}

}
