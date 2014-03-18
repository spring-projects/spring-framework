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

package org.springframework.jdbc.datasource.embedded;

import org.junit.Test;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.CannotReadScriptException;

import static org.junit.Assert.*;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.*;

/**
 * Integration tests for {@link EmbeddedDatabaseBuilder}.
 *
 * @author Keith Donald
 * @author Sam Brannen
 */
public class EmbeddedDatabaseBuilderTests {

	private final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(
		getClass()));


	@Test
	public void addDefaultScripts() throws Exception {
		doTwice(new Runnable() {

			@Override
			public void run() {
				EmbeddedDatabase db = new EmbeddedDatabaseBuilder()//
				.addDefaultScripts()//
				.build();
				assertDatabaseCreatedAndShutdown(db);
			}
		});
	}

	@Test(expected = CannotReadScriptException.class)
	public void addScriptWithBogusFileName() {
		new EmbeddedDatabaseBuilder().addScript("bogus.sql").build();
	}

	@Test
	public void addScript() throws Exception {
		doTwice(new Runnable() {

			@Override
			public void run() {
				EmbeddedDatabase db = builder//
				.addScript("db-schema.sql")//
				.addScript("db-test-data.sql")//
				.build();
				assertDatabaseCreatedAndShutdown(db);
			}
		});
	}

	@Test
	public void addScripts() throws Exception {
		doTwice(new Runnable() {

			@Override
			public void run() {
				EmbeddedDatabase db = builder//
				.addScripts("db-schema.sql", "db-test-data.sql")//
				.build();
				assertDatabaseCreatedAndShutdown(db);
			}
		});
	}

	@Test
	public void addScriptsWithDefaultCommentPrefix() throws Exception {
		doTwice(new Runnable() {

			@Override
			public void run() {
				EmbeddedDatabase db = builder//
				.addScripts("db-schema-comments.sql", "db-test-data.sql")//
				.build();
				assertDatabaseCreatedAndShutdown(db);
			}
		});
	}

	@Test
	public void addScriptsWithCustomCommentPrefix() throws Exception {
		doTwice(new Runnable() {

			@Override
			public void run() {
				EmbeddedDatabase db = builder//
				.addScripts("db-schema-custom-comments.sql", "db-test-data.sql")//
				.setCommentPrefix("~")//
				.build();
				assertDatabaseCreatedAndShutdown(db);
			}
		});
	}

	@Test
	public void addScriptsWithCustomBlockComments() throws Exception {
		doTwice(new Runnable() {

			@Override
			public void run() {
				EmbeddedDatabase db = builder//
				.addScripts("db-schema-block-comments.sql", "db-test-data.sql")//
				.setBlockCommentStartDelimiter("{*")//
				.setBlockCommentEndDelimiter("*}")//
				.build();
				assertDatabaseCreatedAndShutdown(db);
			}
		});
	}

	@Test
	public void setTypeToH2() throws Exception {
		doTwice(new Runnable() {

			@Override
			public void run() {
				EmbeddedDatabase db = builder//
				.setType(H2)//
				.addScripts("db-schema.sql", "db-test-data.sql")//
				.build();
				assertDatabaseCreatedAndShutdown(db);
			}
		});
	}

	@Test
	public void setTypeToDerbyAndIgnoreFailedDrops() throws Exception {
		doTwice(new Runnable() {

			@Override
			public void run() {
				EmbeddedDatabase db = builder//
				.setType(DERBY)//
				.ignoreFailedDrops(true)//
				.addScripts("db-schema-derby-with-drop.sql", "db-test-data.sql").build();
				assertDatabaseCreatedAndShutdown(db);
			}
		});
	}

	private void doTwice(Runnable test) {
		test.run();
		test.run();
	}

	private void assertDatabaseCreatedAndShutdown(EmbeddedDatabase db) {
		JdbcTemplate template = new JdbcTemplate(db);
		assertEquals("Keith", template.queryForObject("select NAME from T_TEST", String.class));
		db.shutdown();
	}

}
