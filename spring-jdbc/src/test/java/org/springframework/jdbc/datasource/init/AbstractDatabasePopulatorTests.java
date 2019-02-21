/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Test;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Abstract base class for integration tests for {@link ResourceDatabasePopulator}
 * and {@link DatabasePopulatorUtils}.
 *
 * @author Dave Syer
 * @author Sam Brannen
 * @author Oliver Gierke
 */
public abstract class AbstractDatabasePopulatorTests extends AbstractDatabaseInitializationTests {

	protected final ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();


	@Test
	public void scriptWithSingleLineCommentsAndFailedDrop() throws Exception {
		databasePopulator.addScript(resource("db-schema-failed-drop-comments.sql"));
		databasePopulator.addScript(resource("db-test-data.sql"));
		databasePopulator.setIgnoreFailedDrops(true);
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertTestDatabaseCreated();
	}

	@Test
	public void scriptWithStandardEscapedLiteral() throws Exception {
		databasePopulator.addScript(defaultSchema());
		databasePopulator.addScript(resource("db-test-data-escaped-literal.sql"));
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertTestDatabaseCreated("'Keith'");
	}

	@Test
	public void scriptWithMySqlEscapedLiteral() throws Exception {
		databasePopulator.addScript(defaultSchema());
		databasePopulator.addScript(resource("db-test-data-mysql-escaped-literal.sql"));
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertTestDatabaseCreated("\\$Keith\\$");
	}

	@Test
	public void scriptWithMultipleStatements() throws Exception {
		databasePopulator.addScript(defaultSchema());
		databasePopulator.addScript(resource("db-test-data-multiple.sql"));
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Keith'", Integer.class),
			equalTo(1));
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Dave'", Integer.class),
			equalTo(1));
	}

	@Test
	public void scriptWithMultipleStatementsAndLongSeparator() throws Exception {
		databasePopulator.addScript(defaultSchema());
		databasePopulator.addScript(resource("db-test-data-endings.sql"));
		databasePopulator.setSeparator("@@");
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Keith'", Integer.class),
			equalTo(1));
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Dave'", Integer.class),
			equalTo(1));
	}

	@Test
	public void scriptWithMultipleStatementsAndWhitespaceSeparator() throws Exception {
		databasePopulator.addScript(defaultSchema());
		databasePopulator.addScript(resource("db-test-data-whitespace.sql"));
		databasePopulator.setSeparator("/\n");
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Keith'", Integer.class),
			equalTo(1));
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Dave'", Integer.class),
			equalTo(1));
	}

	@Test
	public void scriptWithMultipleStatementsAndNewlineSeparator() throws Exception {
		databasePopulator.addScript(defaultSchema());
		databasePopulator.addScript(resource("db-test-data-newline.sql"));
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Keith'", Integer.class),
			equalTo(1));
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Dave'", Integer.class),
			equalTo(1));
	}

	@Test
	public void scriptWithMultipleStatementsAndMultipleNewlineSeparator() throws Exception {
		databasePopulator.addScript(defaultSchema());
		databasePopulator.addScript(resource("db-test-data-multi-newline.sql"));
		databasePopulator.setSeparator("\n\n");
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Keith'", Integer.class),
			equalTo(1));
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Dave'", Integer.class),
			equalTo(1));
	}

	@Test
	public void scriptWithEolBetweenTokens() throws Exception {
		databasePopulator.addScript(usersSchema());
		databasePopulator.addScript(resource("users-data.sql"));
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertUsersDatabaseCreated("Brannen");
	}

	@Test
	public void scriptWithCommentsWithinStatements() throws Exception {
		databasePopulator.addScript(usersSchema());
		databasePopulator.addScript(resource("users-data-with-comments.sql"));
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertUsersDatabaseCreated("Brannen", "Hoeller");
	}

	@Test
	public void scriptWithoutStatementSeparator() throws Exception {
		databasePopulator.setSeparator(ScriptUtils.EOF_STATEMENT_SEPARATOR);
		databasePopulator.addScript(resource("drop-users-schema.sql"));
		databasePopulator.addScript(resource("users-schema-without-separator.sql"));
		databasePopulator.addScript(resource("users-data-without-separator.sql"));
		DatabasePopulatorUtils.execute(databasePopulator, db);

		assertUsersDatabaseCreated("Brannen");
	}

	@Test
	public void constructorWithMultipleScriptResources() throws Exception {
		final ResourceDatabasePopulator populator = new ResourceDatabasePopulator(usersSchema(),
			resource("users-data-with-comments.sql"));
		DatabasePopulatorUtils.execute(populator, db);
		assertUsersDatabaseCreated("Brannen", "Hoeller");
	}

	@Test
	public void scriptWithSelectStatements() throws Exception {
		databasePopulator.addScript(defaultSchema());
		databasePopulator.addScript(resource("db-test-data-select.sql"));
		DatabasePopulatorUtils.execute(databasePopulator, db);
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Keith'", Integer.class),
			equalTo(1));
		assertThat(jdbcTemplate.queryForObject("select COUNT(NAME) from T_TEST where NAME='Dave'", Integer.class),
			equalTo(1));
	}

	/**
	 * See SPR-9457
	 */
	@Test
	public void usesBoundConnectionIfAvailable() throws SQLException {
		TransactionSynchronizationManager.initSynchronization();
		Connection connection = DataSourceUtils.getConnection(db);
		DatabasePopulator populator = mock(DatabasePopulator.class);
		DatabasePopulatorUtils.execute(populator, db);
		verify(populator).populate(connection);
	}

	/**
	 * See SPR-9781
	 */
	@Test(timeout = 1000)
	public void executesHugeScriptInReasonableTime() throws SQLException {
		databasePopulator.addScript(defaultSchema());
		databasePopulator.addScript(resource("db-test-data-huge.sql"));
		DatabasePopulatorUtils.execute(databasePopulator, db);
	}

	private void assertTestDatabaseCreated() {
		assertTestDatabaseCreated("Keith");
	}

	private void assertTestDatabaseCreated(String name) {
		assertEquals(name, jdbcTemplate.queryForObject("select NAME from T_TEST", String.class));
	}

}
