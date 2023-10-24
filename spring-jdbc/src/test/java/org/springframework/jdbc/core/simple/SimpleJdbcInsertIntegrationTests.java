/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.core.simple;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SimpleJdbcInsert} using an embedded H2 database.
 *
 * @author Sam Brannen
 * @since 6.1
 * @see SimpleJdbcInsertTests
 */
class SimpleJdbcInsertIntegrationTests {

	@Nested
	class DefaultSchemaTests extends AbstractSimpleJdbcInsertIntegrationTests {

		@Test
		void retrieveColumnNamesFromMetadata() throws Exception {
			SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
					.withTableName("users")
					.usingGeneratedKeyColumns("id");

			insert.compile();
			// NOTE: column names looked up via metadata in H2/HSQL will be UPPERCASE!
			assertThat(insert.getInsertString()).isEqualTo("INSERT INTO users (FIRST_NAME, LAST_NAME) VALUES(?, ?)");

			insertJaneSmith(insert);
		}

		@Test
		void usingColumns() {
			SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
					.withoutTableColumnMetaDataAccess()
					.withTableName("users")
					.usingColumns("first_name", "last_name")
					.usingGeneratedKeyColumns("id");

			insert.compile();
			assertThat(insert.getInsertString()).isEqualTo("INSERT INTO users (first_name, last_name) VALUES(?, ?)");

			insertJaneSmith(insert);
		}

		@Test  // gh-24013
		void usingColumnsAndQuotedIdentifiers() throws Exception {
			// NOTE: unquoted identifiers in H2/HSQL must be converted to UPPERCASE
			// since that's how they are stored in the DB metadata.
			SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
					.withoutTableColumnMetaDataAccess()
					.withTableName("USERS")
					.usingColumns("FIRST_NAME", "LAST_NAME")
					.usingGeneratedKeyColumns("id")
					.usingQuotedIdentifiers();

			insert.compile();
			assertThat(insert.getInsertString()).isEqualToIgnoringNewLines("""
					INSERT INTO "USERS" ("FIRST_NAME", "LAST_NAME") VALUES(?, ?)
					""");

			insertJaneSmith(insert);
		}

		@Override
		protected String getSchemaScript() {
			return "users-schema.sql";
		}

		@Override
		protected String getUsersTableName() {
			return "users";
		}

	}

	@Nested
	class CustomSchemaTests extends AbstractSimpleJdbcInsertIntegrationTests {

		@Test
		void usingColumnsWithSchemaName() {
			SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
					.withoutTableColumnMetaDataAccess()
					.withSchemaName("my_schema")
					.withTableName("users")
					.usingColumns("first_name", "last_name")
					.usingGeneratedKeyColumns("id");

			insert.compile();
			assertThat(insert.getInsertString()).isEqualTo("INSERT INTO my_schema.users (first_name, last_name) VALUES(?, ?)");

			insertJaneSmith(insert);
		}

		@Test  // gh-24013
		void usingColumnsAndQuotedIdentifiersWithSchemaName() throws Exception {
			// NOTE: unquoted identifiers in H2/HSQL must be converted to UPPERCASE
			// since that's how they are stored in the DB metadata.
			SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
					.withoutTableColumnMetaDataAccess()
					.withSchemaName("MY_SCHEMA")
					.withTableName("USERS")
					.usingColumns("FIRST_NAME", "LAST_NAME")
					.usingGeneratedKeyColumns("id")
					.usingQuotedIdentifiers();

			insert.compile();
			assertThat(insert.getInsertString()).isEqualToIgnoringNewLines("""
					INSERT INTO "MY_SCHEMA"."USERS" ("FIRST_NAME", "LAST_NAME") VALUES(?, ?)
					""");

			insertJaneSmith(insert);
		}

		@Override
		protected String getSchemaScript() {
			return "users-schema-with-custom-schema.sql";
		}

		@Override
		protected String getUsersTableName() {
			return "my_schema.users";
		}

	}

	private abstract static class AbstractSimpleJdbcInsertIntegrationTests {

		protected EmbeddedDatabase embeddedDatabase;

		@BeforeEach
		void createDatabase() {
			this.embeddedDatabase = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(DatabasePopulator.class))
					.setType(EmbeddedDatabaseType.H2)
					.addScript(getSchemaScript())
					.addScript("users-data.sql")
					.build();

			assertNumUsers(1);
		}

		@AfterEach
		void shutdownDatabase() {
			this.embeddedDatabase.shutdown();
		}

		protected void assertNumUsers(long count) {
			JdbcClient jdbcClient = JdbcClient.create(this.embeddedDatabase);
			long numUsers = jdbcClient.sql("select count(*) from " + getUsersTableName()).query(Long.class).single();
			assertThat(numUsers).isEqualTo(count);
		}

		protected void insertJaneSmith(SimpleJdbcInsert insert) {
			Number id = insert.executeAndReturnKey(Map.of("first_name", "Jane", "last_name", "Smith"));
			assertThat(id.intValue()).isEqualTo(2);
			assertNumUsers(2);
		}

		protected abstract String getSchemaScript();

		protected abstract String getUsersTableName();

	}

}
