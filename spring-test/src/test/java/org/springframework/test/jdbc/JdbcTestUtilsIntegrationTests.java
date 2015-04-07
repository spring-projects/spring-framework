/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.jdbc;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.junit.Assert.*;

/**
 * Integration tests for {@link JdbcTestUtils}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 * @see JdbcTestUtilsTests
 */
public class JdbcTestUtilsIntegrationTests {

	private final EmbeddedDatabase db = new EmbeddedDatabaseBuilder().build();

	private JdbcTemplate jdbcTemplate = new JdbcTemplate(db);


	@After
	public void shutdown() {
		db.shutdown();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void executeSqlScriptsAndcountRowsInTableWhere() throws Exception {

		for (String script : Arrays.asList("schema.sql", "data.sql")) {
			Resource resource = new ClassPathResource(script, getClass());
			JdbcTestUtils.executeSqlScript(this.jdbcTemplate, new EncodedResource(resource), false);
		}

		assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "person", "name = 'bob'"));
	}

}
