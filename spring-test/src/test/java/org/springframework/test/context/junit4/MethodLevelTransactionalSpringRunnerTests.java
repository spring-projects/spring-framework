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
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * JUnit 4 based integration test which verifies support of Spring's
 * {@link Transactional &#64;Transactional}, {@link TestExecutionListeners
 * &#64;TestExecutionListeners}, and {@link ContextConfiguration
 * &#64;ContextConfiguration} annotations in conjunction with the
 * {@link SpringJUnit4ClassRunner} and the following
 * {@link TestExecutionListener TestExecutionListeners}:
 * </p>
 * <ul>
 * <li>{@link DependencyInjectionTestExecutionListener}</li>
 * <li>{@link DirtiesContextTestExecutionListener}</li>
 * <li>{@link TransactionalTestExecutionListener}</li>
 * </ul>
 * <p>
 * This class specifically tests usage of <code>&#064;Transactional</code>
 * defined at the <strong>method level</strong>. In contrast to
 * {@link ClassLevelTransactionalSpringRunnerTests}, this class omits usage of
 * <code>&#064;NotTransactional</code>.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 * @see ClassLevelTransactionalSpringRunnerTests
 */
@SuppressWarnings("deprecation")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
	TransactionalTestExecutionListener.class })
public class MethodLevelTransactionalSpringRunnerTests extends AbstractTransactionalSpringRunnerTests {

	protected static SimpleJdbcTemplate simpleJdbcTemplate;


	@AfterClass
	public static void verifyFinalTestData() {
		assertEquals("Verifying the final number of rows in the person table after all tests.", 4,
			countRowsInPersonTable(simpleJdbcTemplate));
	}

	@Before
	public void verifyInitialTestData() {
		clearPersonTable(simpleJdbcTemplate);
		assertEquals("Adding bob", 1, addPerson(simpleJdbcTemplate, BOB));
		assertEquals("Verifying the initial number of rows in the person table.", 1,
			countRowsInPersonTable(simpleJdbcTemplate));
	}

	@Test
	@Transactional("transactionManager2")
	public void modifyTestDataWithinTransaction() {
		assertInTransaction(true);
		assertEquals("Deleting bob", 1, deletePerson(simpleJdbcTemplate, BOB));
		assertEquals("Adding jane", 1, addPerson(simpleJdbcTemplate, JANE));
		assertEquals("Adding sue", 1, addPerson(simpleJdbcTemplate, SUE));
		assertEquals("Verifying the number of rows in the person table within a transaction.", 2,
			countRowsInPersonTable(simpleJdbcTemplate));
	}

	@Test
	public void modifyTestDataWithoutTransaction() {
		assertInTransaction(false);
		assertEquals("Adding luke", 1, addPerson(simpleJdbcTemplate, LUKE));
		assertEquals("Adding leia", 1, addPerson(simpleJdbcTemplate, LEIA));
		assertEquals("Adding yoda", 1, addPerson(simpleJdbcTemplate, YODA));
		assertEquals("Verifying the number of rows in the person table without a transaction.", 4,
			countRowsInPersonTable(simpleJdbcTemplate));
	}


	public static class DatabaseSetup {

		@Resource
		public void setDataSource2(DataSource dataSource) {
			simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
			createPersonTable(simpleJdbcTemplate);
		}
	}

}
