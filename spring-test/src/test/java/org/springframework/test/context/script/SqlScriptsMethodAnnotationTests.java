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

package org.springframework.test.context.script;

import org.junit.Test;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import static org.junit.Assert.assertEquals;

/**
 * Unit test to verify method level {@link SqlScripts} annotation.
 *
 * @author Tadaya Tsuyukubo
 */
@ContextConfiguration("/org/springframework/test/context/script/SqlScripts-SingleDataSource-context.xml")
@TestExecutionListeners(SqlScriptsTestExecutionListener.class)
public class SqlScriptsMethodAnnotationTests extends AbstractTransactionalJUnit4SpringContextTests {

	@Test
	@SqlScripts("db-add-bar.sql")
	public void testSingleSqlFile() {
		assertEquals("expected foo and bar", 2, getPersonCount());
	}

	@Test
	@SqlScripts({"db-add-bar.sql", "db-add-baz.sql"})
	public void testMultipleSqlFiles() {
		assertEquals("expected foo, bar, and baz", 3, getPersonCount());
	}

	@Test
	@SqlScripts("/org/springframework/test/context/script/db-add-bar.sql")
	public void testFullSqlPath() {
		assertEquals("expected foo and bar", 2, getPersonCount());
	}

	@AfterTransaction
	public void verifyScriptRanInTransaction() {
		assertEquals("transaction must be rolled back. expected foo", 1, getPersonCount());
	}

	private int getPersonCount() {
		return SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "person");
	}
}
