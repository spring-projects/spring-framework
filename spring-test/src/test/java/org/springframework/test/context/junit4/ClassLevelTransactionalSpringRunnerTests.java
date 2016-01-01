/*
 * Copyright 2002-2015 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;
import static org.springframework.test.transaction.TransactionTestUtils.*;

/**
 * JUnit 4 based integration test which verifies support of Spring's
 * {@link Transactional &#64;Transactional}, {@link TestExecutionListeners
 * &#64;TestExecutionListeners}, and {@link ContextConfiguration
 * &#64;ContextConfiguration} annotations in conjunction with the
 * {@link SpringJUnit4ClassRunner} and the following
 * {@link TestExecutionListener TestExecutionListeners}:
 *
 * <ul>
 * <li>{@link DependencyInjectionTestExecutionListener}</li>
 * <li>{@link DirtiesContextTestExecutionListener}</li>
 * <li>{@link TransactionalTestExecutionListener}</li>
 * </ul>
 *
 * <p>This class specifically tests usage of {@code @Transactional} defined
 * at the <strong>class level</strong>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see MethodLevelTransactionalSpringRunnerTests
 */
@Transactional
public class ClassLevelTransactionalSpringRunnerTests extends AbstractTransactionalSpringRunnerTests {

	protected static JdbcTemplate jdbcTemplate;


	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@AfterClass
	public static void verifyFinalTestData() {
		assertEquals("Verifying the final number of rows in the person table after all tests.", 4,
			countRowsInPersonTable(jdbcTemplate));
	}

	@Before
	public void verifyInitialTestData() {
		clearPersonTable(jdbcTemplate);
		assertEquals("Adding bob", 1, addPerson(jdbcTemplate, BOB));
		assertEquals("Verifying the initial number of rows in the person table.", 1,
			countRowsInPersonTable(jdbcTemplate));
	}

	@Test
	public void modifyTestDataWithinTransaction() {
		assertInTransaction(true);
		assertEquals("Deleting bob", 1, deletePerson(jdbcTemplate, BOB));
		assertEquals("Adding jane", 1, addPerson(jdbcTemplate, JANE));
		assertEquals("Adding sue", 1, addPerson(jdbcTemplate, SUE));
		assertEquals("Verifying the number of rows in the person table within a transaction.", 2,
			countRowsInPersonTable(jdbcTemplate));
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void modifyTestDataWithoutTransaction() {
		assertInTransaction(false);
		assertEquals("Adding luke", 1, addPerson(jdbcTemplate, LUKE));
		assertEquals("Adding leia", 1, addPerson(jdbcTemplate, LEIA));
		assertEquals("Adding yoda", 1, addPerson(jdbcTemplate, YODA));
		assertEquals("Verifying the number of rows in the person table without a transaction.", 4,
			countRowsInPersonTable(jdbcTemplate));
	}

}
