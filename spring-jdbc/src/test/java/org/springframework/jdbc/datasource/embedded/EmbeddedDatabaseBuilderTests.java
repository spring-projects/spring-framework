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

package org.springframework.jdbc.datasource.embedded;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.CannotReadScriptException;

/**
 * @author Keith Donald
 */
public class EmbeddedDatabaseBuilderTests {

	@Test
	public void testBuildDefaultScripts() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase db = builder.addDefaultScripts().build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuild() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()));
		EmbeddedDatabase db = builder.addScript("db-schema.sql").addScript("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuildWithComments() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()));
		EmbeddedDatabase db = builder.addScript("db-schema-comments.sql").addScript("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuildH2() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()));
		EmbeddedDatabase db = builder.setType(EmbeddedDatabaseType.H2).addScript("db-schema.sql").addScript("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuildDerby() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()));
		EmbeddedDatabase db = builder.setType(EmbeddedDatabaseType.DERBY).addScript("db-schema-derby.sql").addScript("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuildNoSuchScript() {
		try {
			new EmbeddedDatabaseBuilder().addScript("bogus.sql").build();
			fail("Should have failed");
		}
		catch (DataAccessResourceFailureException ex) {
			assertTrue(ex.getCause() instanceof CannotReadScriptException);
		}
	}

	private void assertDatabaseCreatedAndShutdown(EmbeddedDatabase db) {
		JdbcTemplate template = new JdbcTemplate(db);
		assertEquals("Keith", template.queryForObject("select NAME from T_TEST", String.class));
		db.shutdown();
	}

}
