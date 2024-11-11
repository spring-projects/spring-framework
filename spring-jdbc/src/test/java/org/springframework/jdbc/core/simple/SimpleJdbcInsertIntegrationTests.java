/*
 * Copyright 2002-2024 the original author or authors.
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

import java.sql.Types;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link SimpleJdbcInsert} using an embedded H2 database.
 *
 * @author Sam Brannen
 * @since 6.1
 * @see SimpleJdbcInsertTests
 */
class SimpleJdbcInsertIntegrationTests {

	@Nested
	class DefaultSchemaTests {

		@Nested
		class UnquotedIdentifiersInSchemaTests extends AbstractSimpleJdbcInsertIntegrationTests {

			@Test
			void retrieveColumnNamesFromMetadata() {
				SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
						.withTableName("users")
						.usingGeneratedKeyColumns("id");

				insert.compile();
				assertThat(insert.getInsertTypes()).containsExactly(Types.VARCHAR, Types.VARCHAR);
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
			void usingColumnsAndQuotedIdentifiers() {
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
			protected String getDataScript() {
				return "users-data.sql";
			}

			@Override
			protected String getTableName() {
				return "users";
			}
		}

		@Nested
		class QuotedIdentifiersInSchemaTests extends AbstractSimpleJdbcInsertIntegrationTests {

			@Test
			void retrieveColumnNamesFromMetadata() {
				SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
						.withTableName("Order")
						.usingGeneratedKeyColumns("id");

				insert.compile();

				// Since we are not quoting identifiers, the column names lookup for the "Order"
				// table fails to find anything, and insert types are not populated.
				assertThat(insert.getInsertTypes()).isEmpty();
				// Consequently, any subsequent attempt to execute the INSERT statement should fail.
				assertThatExceptionOfType(BadSqlGrammarException.class)
						.isThrownBy(() -> insert.executeAndReturnKey(Map.of("from", "start", "date", "1999")));
			}

			@Test  // gh-24013
			void usingColumnsAndQuotedIdentifiers() {
				SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
						.withoutTableColumnMetaDataAccess()
						.withTableName("Order")
						.usingColumns("from", "Date")
						.usingGeneratedKeyColumns("id")
						.usingQuotedIdentifiers();

				insert.compile();
				assertThat(insert.getInsertString()).isEqualToIgnoringNewLines("""
						INSERT INTO "Order" ("from", "Date") VALUES(?, ?)
						""");

				insertOrderEntry(insert);
			}

			@Override
			protected ResourceLoader getResourceLoader() {
				return new ClassRelativeResourceLoader(getClass());
			}

			@Override
			protected String getSchemaScript() {
				return "order-schema.sql";
			}

			@Override
			protected String getDataScript() {
				return "order-data.sql";
			}

			@Override
			protected String getTableName() {
				return "\"Order\"";
			}
		}
	}

	@Nested
	class CustomSchemaTests {

		@Nested
		class UnquotedIdentifiersInSchemaTests extends AbstractSimpleJdbcInsertIntegrationTests {

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
			void usingColumnsAndQuotedIdentifiersWithSchemaName() {
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
			protected String getDataScript() {
				return "users-data.sql";
			}

			@Override
			protected String getTableName() {
				return "my_schema.users";
			}
		}

		@Nested
		class QuotedIdentifiersInSchemaTests extends AbstractSimpleJdbcInsertIntegrationTests {

			@Test
			void usingColumnsWithSchemaName() {
				SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
						.withoutTableColumnMetaDataAccess()
						.withSchemaName("My_Schema")
						.withTableName("Order")
						.usingColumns("from", "Date")
						.usingGeneratedKeyColumns("id");

				insert.compile();

				// Since we are not quoting identifiers, the column names lookup for the
				// My_Schema.Order table results in unknown insert types.
				assertThat(insert.getInsertTypes()).containsExactly(SqlTypeValue.TYPE_UNKNOWN, SqlTypeValue.TYPE_UNKNOWN);
				// Consequently, any subsequent attempt to execute the INSERT statement should fail.
				assertThatExceptionOfType(BadSqlGrammarException.class)
						.isThrownBy(() -> insert.executeAndReturnKey(Map.of("from", "start", "date", "1999")));
			}

			@Test  // gh-24013
			void usingColumnsAndQuotedIdentifiersWithSchemaName() {
				SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
						.withoutTableColumnMetaDataAccess()
						.withSchemaName("My_Schema")
						.withTableName("Order")
						.usingColumns("from", "Date")
						.usingGeneratedKeyColumns("id")
						.usingQuotedIdentifiers();

				insert.compile();
				assertThat(insert.getInsertString()).isEqualToIgnoringNewLines("""
						INSERT INTO "My_Schema"."Order" ("from", "Date") VALUES(?, ?)
						""");

				insertOrderEntry(insert);
			}

			@Override
			protected ResourceLoader getResourceLoader() {
				return new ClassRelativeResourceLoader(getClass());
			}

			@Override
			protected String getSchemaScript() {
				return "order-schema-with-custom-schema.sql";
			}

			@Override
			protected String getDataScript() {
				return "order-data.sql";
			}

			@Override
			protected String getTableName() {
				return "\"My_Schema\".\"Order\"";
			}
		}
	}

	private abstract static class AbstractSimpleJdbcInsertIntegrationTests {

		protected EmbeddedDatabase embeddedDatabase;

		@BeforeEach
		void createDatabase() {
			this.embeddedDatabase = new EmbeddedDatabaseBuilder(getResourceLoader())
					.setType(EmbeddedDatabaseType.H2)
					.addScript(getSchemaScript())
					.addScript(getDataScript())
					.build();

			assertNumRows(1);
		}

		@AfterEach
		void shutdownDatabase() {
			this.embeddedDatabase.shutdown();
		}

		protected ResourceLoader getResourceLoader() {
			return new ClassRelativeResourceLoader(DatabasePopulator.class);
		}

		protected void assertNumRows(long count) {
			JdbcClient jdbcClient = JdbcClient.create(this.embeddedDatabase);
			long numRows = jdbcClient.sql("select count(*) from " + getTableName()).query(Long.class).single();
			assertThat(numRows).isEqualTo(count);
		}

		protected void insertJaneSmith(SimpleJdbcInsert insert) {
			Number id = insert.executeAndReturnKey(Map.of("first_name", "Jane", "last_name", "Smith"));
			assertThat(id.intValue()).isEqualTo(2);
			assertNumRows(2);
		}

		protected void insertOrderEntry(SimpleJdbcInsert insert) {
			Number id = insert.executeAndReturnKey(Map.of("from", "start", "date", "1999"));
			assertThat(id.intValue()).isEqualTo(2);
			assertNumRows(2);
		}

		protected abstract String getSchemaScript();

		protected abstract String getDataScript();

		protected abstract String getTableName();

	}

}
