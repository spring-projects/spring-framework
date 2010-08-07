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

import java.sql.Connection;
import javax.sql.DataSource;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 * @author Dave Syer
 */
public class DatabasePopulatorTests {

	@Test
	public void testBuildWithCommentsAndFailedDrop() throws Exception {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase db = builder.build();
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());
		databasePopulator.addScript(resourceLoader.getResource("db-schema-failed-drop-comments.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data.sql"));
		databasePopulator.setIgnoreFailedDrops(true);
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		}
		finally {
			connection.close();
		}
		assertDatabaseCreated(db);
		db.shutdown();
	}

	private void assertDatabaseCreated(DataSource db) {
		JdbcTemplate template = new JdbcTemplate(db);
		assertEquals("Keith", template.queryForObject("select NAME from T_TEST", String.class));
	}

}
