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

package org.springframework.jdbc.datasource.embedded;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.CannotReadScriptException;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.DERBY;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

/**
 * Integration tests for {@link EmbeddedDatabaseBuilder}.
 *
 * @author Keith Donald
 * @author Sam Brannen
 */
class EmbeddedDatabaseBuilderTests {

	private final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()));


	@Test
	void addDefaultScripts() {
		doTwice(() -> {
			EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
					.addDefaultScripts()
					.build();
			assertDatabaseCreatedAndShutdown(db);
		});
	}

	@Test
	void addScriptWithBogusFileName() {
		assertThatExceptionOfType(CannotReadScriptException.class)
				.isThrownBy(new EmbeddedDatabaseBuilder().addScript("bogus.sql")::build);
	}

	@Test
	void addScript() {
		doTwice(() -> {
			EmbeddedDatabase db = builder
					.addScript("db-schema.sql")
					.addScript("db-test-data.sql")
					.build();
			assertDatabaseCreatedAndShutdown(db);
		});
	}

	@Test
	void addScripts() {
		doTwice(() -> {
			EmbeddedDatabase db = builder
					.addScripts("db-schema.sql", "db-test-data.sql")
					.build();
			assertDatabaseCreatedAndShutdown(db);
		});
	}

	@Test
	void addScriptsWithDefaultCommentPrefix() {
		doTwice(() -> {
			EmbeddedDatabase db = builder
					.addScripts("db-schema-comments.sql", "db-test-data.sql")
					.build();
			assertDatabaseCreatedAndShutdown(db);
		});
	}

	@Test
	void addScriptsWithCustomCommentPrefix() {
		doTwice(() -> {
			EmbeddedDatabase db = builder
					.addScripts("db-schema-custom-comments.sql", "db-test-data.sql")
					.setCommentPrefix("~")
					.build();
			assertDatabaseCreatedAndShutdown(db);
		});
	}

	@Test
	void addScriptsWithCustomBlockComments() {
		doTwice(() -> {
			EmbeddedDatabase db = builder
					.addScripts("db-schema-block-comments.sql", "db-test-data.sql")
					.setBlockCommentStartDelimiter("{*")
					.setBlockCommentEndDelimiter("*}")
					.build();
			assertDatabaseCreatedAndShutdown(db);
		});
	}

	@Test
	void setTypeToH2() {
		doTwice(() -> {
			EmbeddedDatabase db = builder
					.setType(H2)
					.addScripts("db-schema.sql", "db-test-data.sql")
					.build();
			assertDatabaseCreatedAndShutdown(db);
		});
	}

	@Test
	void setTypeConfigurerToCustomH2() {
		doTwice(() -> {
			EmbeddedDatabase db = builder
					.setDatabaseConfigurer(EmbeddedDatabaseConfigurers.customizeConfigurer(H2, defaultConfigurer ->
							new EmbeddedDatabaseConfigurerDelegate(defaultConfigurer) {
								@Override
								public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
									super.configureConnectionProperties(properties, databaseName);
								}
							}))
					.addScripts("db-schema.sql", "db-test-data.sql")
					.build();
			assertDatabaseCreatedAndShutdown(db);
		});
	}

	@Test
	void setTypeToDerbyAndIgnoreFailedDrops() {
		doTwice(() -> {
			EmbeddedDatabase db = builder
					.setType(DERBY)
					.ignoreFailedDrops(true)
					.addScripts("db-schema-derby-with-drop.sql", "db-test-data.sql").build();
			assertDatabaseCreatedAndShutdown(db);
		});
	}

	@Test
	void createSameSchemaTwiceWithoutUniqueDbNames() {
		EmbeddedDatabase db1 = builder.addScripts("db-schema-without-dropping.sql").build();
		try {
			assertThatExceptionOfType(ScriptStatementFailedException.class).isThrownBy(() ->
					new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass())).addScripts("db-schema-without-dropping.sql").build());
		}
		finally {
			db1.shutdown();
		}
	}

	@Test
	void createSameSchemaTwiceWithGeneratedUniqueDbNames() {
		EmbeddedDatabase db1 = builder
				.addScripts("db-schema-without-dropping.sql", "db-test-data.sql")
				.generateUniqueName(true)
				.build();

		JdbcTemplate template1 = new JdbcTemplate(db1);
		assertNumRowsInTestTable(template1, 1);
		template1.update("insert into T_TEST (NAME) values ('Sam')");
		assertNumRowsInTestTable(template1, 2);

		EmbeddedDatabase db2 = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()))
				.addScripts("db-schema-without-dropping.sql", "db-test-data.sql")
				.generateUniqueName(true)
				.build();
		assertDatabaseCreated(db2);

		db1.shutdown();
		db2.shutdown();
	}

	private void doTwice(Runnable test) {
		test.run();
		test.run();
	}

	private void assertNumRowsInTestTable(JdbcTemplate template, int count) {
		assertThat(template.queryForObject("select count(*) from T_TEST", Integer.class)).isEqualTo(count);
	}

	private void assertDatabaseCreated(EmbeddedDatabase db) {
		assertNumRowsInTestTable(new JdbcTemplate(db), 1);
	}

	private void assertDatabaseCreatedAndShutdown(EmbeddedDatabase db) {
		assertDatabaseCreated(db);
		db.shutdown();
	}

}
