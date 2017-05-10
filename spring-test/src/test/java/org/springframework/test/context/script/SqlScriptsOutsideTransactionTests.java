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

import static org.junit.Assert.*;

/**
 * Unit test to verify {@link SqlScripts} outside of transaction.
 *
 * @author Tadaya Tsuyukubo
 */
@ContextConfiguration("/org/springframework/test/context/script/SqlScripts-SingleDataSource-context.xml")
@TestExecutionListeners(SqlScriptsTestExecutionListener.class)
public class SqlScriptsOutsideTransactionTests extends AbstractTransactionalJUnit4SpringContextTests {

	@Test
	@SqlScripts(value = "db-add-bar.sql", withinTransaction = false)
	public void testTransaction() {
		assertEquals("inside of test transaction. expected foo and bar", 2, getPersonCount());
	}

	@AfterTransaction
	public void verifyChangeOutsideOfTransaction() {
		// change has to be persisted.
		assertEquals("outside of test transaction. expected foo and bar", 2, getPersonCount());
	}

	private int getPersonCount() {
		return SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "person");
	}

}
