/*
 * Copyright 2002-2010 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 * @author Dave Syer
 * @author Sam Brannen
 */
public class DatabasePopulatorTests {

	private final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
	private final EmbeddedDatabase db = builder.build();
	private final ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
	private final ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());
	private final JdbcTemplate jdbcTemplate = new JdbcTemplate(db);

	private void assertTestDatabaseCreated() {
		assertEquals("Keith", jdbcTemplate.queryForObject("select NAME from T_TEST", String.class));
	}

	private void assertUsersDatabaseCreated(DataSource db) {
		assertEquals("Sam", jdbcTemplate.queryForObject("select first_name from users where last_name = 'Brannen'",
				String.class));
	}

	@After
	public void shutDown() {
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
	public void scriptWithEolBetweenTokens() throws Exception {
		databasePopulator.addScript(resourceLoader.getResource("users-schema.sql"));
		databasePopulator.addScript(resourceLoader.getResource("users-data.sql"));
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}

		assertUsersDatabaseCreated(db);
	}

}
