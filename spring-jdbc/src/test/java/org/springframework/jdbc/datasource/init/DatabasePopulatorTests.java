/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import org.easymock.EasyMock;

import org.junit.After;
import org.junit.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Dave Syer
 * @author Sam Brannen
 * @author Oliver Gierke
 */
public class DatabasePopulatorTests {

	private final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
	private final EmbeddedDatabase db = builder.build();
	private final ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
	private final ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());
	private final JdbcTemplate jdbcTemplate = new JdbcTemplate(db);

	private void assertTestDatabaseCreated() {
		assertTestDatabaseCreated("Keith");
	}

	private void assertTestDatabaseCreated(String name) {
		assertEquals(name, jdbcTemplate.queryForObject("select NAME from T_TEST", String.class));
	}

	private void assertUsersDatabaseCreated() {
		assertEquals("Sam", jdbcTemplate.queryForObject("select first_name from users where last_name = 'Brannen'",
				String.class));
	}

	@After
	public void shutDown() {

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
			TransactionSynchronizationManager.unbindResource(db);
		}

		db.shutdown();
	}

	@Test
	public void testBuildWithCommentsAndFailedDrop() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("db-schema-failed-drop-comments.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data.sql"));
		databasePopulator.setIgnoreFailedDrops(true);
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertTestDatabaseCreated();
	}

	@Test
	public void testBuildWithNormalEscapedLiteral() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("db-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data-escaped-literal.sql"));
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertTestDatabaseCreated("'Keith'");
	}

	@Test
	public void testBuildWithMySQLEscapedLiteral() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("db-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data-mysql-escaped-literal.sql"));
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertTestDatabaseCreated("\\$Keith\\$");
	}

	@Test
	public void testBuildWithMultipleStatements() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("db-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data-multiple.sql"));
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Keith'"));
		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Dave'"));
	}

	@Test
	public void testBuildWithMultipleStatementsLongSeparator() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("db-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data-endings.sql"));
		databasePopulator.setSeparator("@@");
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Keith'"));
		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Dave'"));
	}

	@Test
	public void testBuildWithMultipleStatementsWhitespaceSeparator() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("db-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data-whitespace.sql"));
		databasePopulator.setSeparator("/\n");
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Keith'"));
		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Dave'"));
	}

	@Test
	public void testBuildWithMultipleStatementsNewlineSeparator() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("db-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data-newline.sql"));
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Keith'"));
		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Dave'"));
	}

	@Test
	public void testBuildWithMultipleStatementsMultipleNewlineSeparator() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("db-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data-multi-newline.sql"));
		databasePopulator.setSeparator("\n\n");
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Keith'"));
		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Dave'"));
	}

	@Test
	public void scriptWithEolBetweenTokens() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("users-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("users-data.sql"));
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertUsersDatabaseCreated();
	}

	@Test
	public void testBuildWithSelectStatements() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("db-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data-select.sql"));
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Keith'"));
		assertEquals(1, jdbcTemplate.queryForInt("select COUNT(NAME) from T_TEST where NAME='Dave'"));
	}

	/**
	 * @see SPR-9457
	 */
	@Test
	public void usesBoundConnectionIfAvailable() throws SQLException {

		TransactionSynchronizationManager.initSynchronization();
		Connection connection = DataSourceUtils.getConnection(db);

		DatabasePopulator populator = EasyMock.createMock(DatabasePopulator.class);
		populator.populate(connection);
		EasyMock.expectLastCall();
		EasyMock.replay(populator);

		DatabasePopulatorUtils.execute(populator, db);

		EasyMock.verify(populator);
	}

	/**
	 * @see SPR-9781
	 */
	@Test(timeout = 1000)
	public void executesHugeScriptInReasonableTime() throws SQLException {

		databasePopulator.addScript(resourceLoader.getResource("db-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data-huge.sql"));

		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}
	}
}
