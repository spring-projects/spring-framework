/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.jdbc;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.AfterTransaction;

import static org.junit.Assert.*;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.*;

/**
 * Transactional integration tests for {@link Sql @Sql} that verify proper
 * support for {@link ExecutionPhase#AFTER_TEST_METHOD}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@DirtiesContext
public class TransactionalAfterTestMethodSqlScriptsTests extends AbstractTransactionalJUnit4SpringContextTests {

	@Rule
	public TestName testName = new TestName();


	@AfterTransaction
	public void afterTransaction() {
		if ("test01".equals(testName.getMethodName())) {
			try {
				assertNumUsers(99);
				fail("Should throw a BadSqlGrammarException after test01, assuming 'drop-schema.sql' was executed");
			}
			catch (BadSqlGrammarException e) {
				/* expected */
			}
		}
	}

	@Test
	@SqlGroup({//
	@Sql({ "schema.sql", "data.sql" }),//
		@Sql(scripts = "drop-schema.sql", executionPhase = AFTER_TEST_METHOD) //
	})
	// test## is required for @FixMethodOrder.
	public void test01() {
		assertNumUsers(1);
	}

	@Test
	@Sql({ "schema.sql", "data.sql", "data-add-dogbert.sql" })
	// test## is required for @FixMethodOrder.
	public void test02() {
		assertNumUsers(2);
	}

	protected void assertNumUsers(int expected) {
		assertEquals("Number of rows in the 'user' table.", expected, countRowsInTable("user"));
	}

}
