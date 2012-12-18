/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.test.context.junit4;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.transaction.TransactionTestUtils.assertInTransaction;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * JUnit 4 based integration test which verifies proper transactional behavior when the
 * {@link TransactionConfiguration#defaultRollback() defaultRollback} attribute
 * of the {@link TransactionConfiguration} annotation is set to <strong>{@code true}</strong>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see TransactionConfiguration
 */
@SuppressWarnings("deprecation")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true)
public class DefaultRollbackTrueTransactionalSpringRunnerTests extends AbstractTransactionalSpringRunnerTests {

	protected static int originalNumRows;

	protected static SimpleJdbcTemplate simpleJdbcTemplate;


	@AfterClass
	public static void verifyFinalTestData() {
		assertEquals("Verifying the final number of rows in the person table after all tests.", originalNumRows,
			countRowsInPersonTable(simpleJdbcTemplate));
	}

	@Before
	public void verifyInitialTestData() {
		originalNumRows = clearPersonTable(simpleJdbcTemplate);
		assertEquals("Adding bob", 1, addPerson(simpleJdbcTemplate, BOB));
		assertEquals("Verifying the initial number of rows in the person table.", 1,
			countRowsInPersonTable(simpleJdbcTemplate));
	}

	@Test(timeout = 1000)
	@Transactional
	public void modifyTestDataWithinTransaction() {
		assertInTransaction(true);
		assertEquals("Adding jane", 1, addPerson(simpleJdbcTemplate, JANE));
		assertEquals("Adding sue", 1, addPerson(simpleJdbcTemplate, SUE));
		assertEquals("Verifying the number of rows in the person table within a transaction.", 3,
			countRowsInPersonTable(simpleJdbcTemplate));
	}


	public static class DatabaseSetup {

		@Resource
		public void setDataSource(DataSource dataSource) {
			simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
			createPersonTable(simpleJdbcTemplate);
		}
	}

}
