/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.transaction;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * JUnit based integration test which verifies
 * {@link BeforeTransaction @BeforeTransaction} and
 * {@link AfterTransaction @AfterTransaction} behavior.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
class BeforeAndAfterTransactionAnnotationTests extends AbstractTransactionalSpringTests {

	private static int numBeforeTransactionCalls = 0;
	private static int numAfterTransactionCalls = 0;

	JdbcTemplate jdbcTemplate;

	boolean inTransaction = false;


	@Autowired
	void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}


	@BeforeAll
	void beforeClass() {
		BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls = 0;
		BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls = 0;
	}

	@AfterAll
	void afterClass() {
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo(3);
		assertThat(BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls).as("Verifying the total number of calls to beforeTransaction().").isEqualTo(2);
		assertThat(BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls).as("Verifying the total number of calls to afterTransaction().").isEqualTo(2);
	}

	@BeforeTransaction
	void beforeTransaction() {
		assertThatTransaction().isNotActive();
		this.inTransaction = true;
		BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls++;
		clearPersonTable(jdbcTemplate);
		assertThat(addPerson(jdbcTemplate, YODA)).as("Adding yoda").isEqualTo(1);
	}

	@AfterTransaction
	void afterTransaction() {
		assertThatTransaction().isNotActive();
		this.inTransaction = false;
		BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls++;
		assertThat(deletePerson(jdbcTemplate, YODA)).as("Deleting yoda").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table after a transactional test method.").isEqualTo(0);
	}

	@BeforeEach
	void before(TestInfo testInfo) {
		assertShouldBeInTransaction(testInfo);
		long expected = (this.inTransaction ? 1 : 0);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table before a test method.").isEqualTo(expected);
	}

	@AfterEach
	void after(TestInfo testInfo) {
		assertShouldBeInTransaction(testInfo);
	}

	@Test
	void transactionalMethod1() {
		assertThatTransaction().isActive();
		assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within transactionalMethod1().").isEqualTo(2);
	}

	@Test
	void transactionalMethod2() {
		assertThatTransaction().isActive();
		assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within transactionalMethod2().").isEqualTo(3);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void nonTransactionalMethod() {
		assertThatTransaction().isNotActive();
		assertThat(addPerson(jdbcTemplate, LUKE)).as("Adding luke").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, LEIA)).as("Adding leia").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, YODA)).as("Adding yoda").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table without a transaction.").isEqualTo(3);
	}


	private static void assertShouldBeInTransaction(TestInfo testInfo) {
		if (!testInfo.getTestMethod().get().getName().equals("nonTransactionalMethod")) {
			assertThatTransaction().isActive();
		}
		else {
			assertThatTransaction().isNotActive();
		}
	}

}
