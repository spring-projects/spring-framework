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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * JUnit 4 based integration test which verifies
 * {@link BeforeTransaction @BeforeTransaction} and
 * {@link AfterTransaction @AfterTransaction} behavior.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@SuppressWarnings("deprecation")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({ TransactionalTestExecutionListener.class })
public class BeforeAndAfterTransactionAnnotationTests extends AbstractTransactionalSpringRunnerTests {

	protected static SimpleJdbcTemplate simpleJdbcTemplate;

	protected static int numBeforeTransactionCalls = 0;
	protected static int numAfterTransactionCalls = 0;

	protected boolean inTransaction = false;


	@BeforeClass
	public static void beforeClass() {
		BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls = 0;
		BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls = 0;
	}

	@AfterClass
	public static void afterClass() {
		assertEquals("Verifying the final number of rows in the person table after all tests.", 3,
			countRowsInPersonTable(simpleJdbcTemplate));
		assertEquals("Verifying the total number of calls to beforeTransaction().", 2,
			BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls);
		assertEquals("Verifying the total number of calls to afterTransaction().", 2,
			BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls);
	}

	@BeforeTransaction
	public void beforeTransaction() {
		assertInTransaction(false);
		this.inTransaction = true;
		BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls++;
		clearPersonTable(simpleJdbcTemplate);
		assertEquals("Adding yoda", 1, addPerson(simpleJdbcTemplate, YODA));
	}

	@AfterTransaction
	public void afterTransaction() {
		assertInTransaction(false);
		this.inTransaction = false;
		BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls++;
		assertEquals("Deleting yoda", 1, deletePerson(simpleJdbcTemplate, YODA));
		assertEquals("Verifying the number of rows in the person table after a transactional test method.", 0,
			countRowsInPersonTable(simpleJdbcTemplate));
	}

	@Before
	public void before() {
		assertEquals("Verifying the number of rows in the person table before a test method.", (this.inTransaction ? 1
				: 0), countRowsInPersonTable(simpleJdbcTemplate));
	}

	@Test
	@Transactional
	public void transactionalMethod1() {
		assertInTransaction(true);
		assertEquals("Adding jane", 1, addPerson(simpleJdbcTemplate, JANE));
		assertEquals("Verifying the number of rows in the person table within transactionalMethod1().", 2,
			countRowsInPersonTable(simpleJdbcTemplate));
	}

	@Test
	@Transactional
	public void transactionalMethod2() {
		assertInTransaction(true);
		assertEquals("Adding jane", 1, addPerson(simpleJdbcTemplate, JANE));
		assertEquals("Adding sue", 1, addPerson(simpleJdbcTemplate, SUE));
		assertEquals("Verifying the number of rows in the person table within transactionalMethod2().", 3,
			countRowsInPersonTable(simpleJdbcTemplate));
	}

	@Test
	public void nonTransactionalMethod() {
		assertInTransaction(false);
		assertEquals("Adding luke", 1, addPerson(simpleJdbcTemplate, LUKE));
		assertEquals("Adding leia", 1, addPerson(simpleJdbcTemplate, LEIA));
		assertEquals("Adding yoda", 1, addPerson(simpleJdbcTemplate, YODA));
		assertEquals("Verifying the number of rows in the person table without a transaction.", 3,
			countRowsInPersonTable(simpleJdbcTemplate));
	}


	public static class DatabaseSetup {

		@Resource
		void setDataSource(DataSource dataSource) {
			simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
			createPersonTable(simpleJdbcTemplate);
		}
	}

}
