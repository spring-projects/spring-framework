/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.testng;

import javax.sql.DataSource;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

/**
 * Integration tests that verify support for
 * {@link org.springframework.context.annotation.Configuration @Configuration} classes
 * with transactional TestNG-based tests.
 *
 * <p>Configuration will be loaded from
 * {@link AnnotationConfigTransactionalTestNGSpringContextTests.ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
@ContextConfiguration
class AnnotationConfigTransactionalTestNGSpringContextTests
		extends AbstractTransactionalTestNGSpringContextTests {

	private static final String JANE = "jane";
	private static final String SUE = "sue";
	private static final String YODA = "yoda";

	private static final int NUM_TESTS = 2;
	private static final int NUM_TX_TESTS = 1;

	private static int numSetUpCalls = 0;
	private static int numSetUpCallsInTransaction = 0;
	private static int numTearDownCalls = 0;
	private static int numTearDownCallsInTransaction = 0;

	@Autowired
	private Employee employee;

	@Autowired
	private Pet pet;


	private int createPerson(String name) {
		return jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	private int deletePerson(String name) {
		return jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	private void assertNumRowsInPersonTable(int expectedNumRows, String testState) {
		assertThat(countRowsInTable("person"))
			.as("the number of rows in the person table (" + testState + ").")
			.isEqualTo(expectedNumRows);
	}

	private void assertAddPerson(String name) {
		assertThat(createPerson(name)).as("Adding '%s'", name).isEqualTo(1);
	}

	@BeforeClass
	void beforeClass() {
		numSetUpCalls = 0;
		numSetUpCallsInTransaction = 0;
		numTearDownCalls = 0;
		numTearDownCallsInTransaction = 0;
	}

	@AfterClass
	void afterClass() {
		assertThat(numSetUpCalls).as("number of calls to setUp().").isEqualTo(NUM_TESTS);
		assertThat(numSetUpCallsInTransaction).as("number of calls to setUp() within a transaction.").isEqualTo(NUM_TX_TESTS);
		assertThat(numTearDownCalls).as("number of calls to tearDown().").isEqualTo(NUM_TESTS);
		assertThat(numTearDownCallsInTransaction).as("number of calls to tearDown() within a transaction.").isEqualTo(NUM_TX_TESTS);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void autowiringFromConfigClass() {
		assertThat(employee).as("The employee should have been autowired.").isNotNull();
		assertThat(employee.getName()).isEqualTo("John Smith");

		assertThat(pet).as("The pet should have been autowired.").isNotNull();
		assertThat(pet.getName()).isEqualTo("Fido");
	}

	@BeforeTransaction
	void beforeTransaction() {
		assertNumRowsInPersonTable(1, "before a transactional test method");
		assertAddPerson(YODA);
	}

	@BeforeMethod
	void setUp() {
		numSetUpCalls++;
		if (isActualTransactionActive()) {
			numSetUpCallsInTransaction++;
		}
		assertNumRowsInPersonTable((isActualTransactionActive() ? 2 : 1), "before a test method");
	}

	@Test
	void modifyTestDataWithinTransaction() {
		assertThatTransaction().isActive();
		assertAddPerson(JANE);
		assertAddPerson(SUE);
		assertNumRowsInPersonTable(4, "in modifyTestDataWithinTransaction()");
	}

	@AfterMethod
	void tearDown() {
		numTearDownCalls++;
		if (isActualTransactionActive()) {
			numTearDownCallsInTransaction++;
		}
		assertNumRowsInPersonTable((isActualTransactionActive() ? 4 : 1), "after a test method");
	}

	@AfterTransaction
	void afterTransaction() {
		assertThat(deletePerson(YODA)).as("Deleting yoda").isEqualTo(1);
		assertNumRowsInPersonTable(1, "after a transactional test method");
	}


	@Configuration
	static class ContextConfiguration {

		@Bean
		Employee employee() {
			Employee employee = new Employee();
			employee.setName("John Smith");
			employee.setAge(42);
			employee.setCompany("Acme Widgets, Inc.");
			return employee;
		}

		@Bean
		Pet pet() {
			return new Pet("Fido");
		}

		@Bean
		PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/org/springframework/test/jdbc/schema.sql")
					.addScript("classpath:/org/springframework/test/jdbc/data.sql")
					.build();
		}

	}

}
