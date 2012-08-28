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

package org.springframework.test.context.testng;

import static org.springframework.test.transaction.TransactionTestUtils.assertInTransaction;
import static org.springframework.test.transaction.TransactionTestUtils.inTransaction;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import javax.sql.DataSource;

import org.springframework.beans.Employee;
import org.springframework.beans.Pet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration tests that verify support for
 * {@link import org.springframework.context.annotation.Configuration @Configuration}
 * classes with TestNG-based tests.
 * 
 * <p>Configuration will be loaded from
 * {@link AnnotationConfigTransactionalTestNGSpringContextTests.ContextConfiguration}.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
@SuppressWarnings("deprecation")
@ContextConfiguration
public class AnnotationConfigTransactionalTestNGSpringContextTests extends
		AbstractTransactionalTestNGSpringContextTests {

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
		return simpleJdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	private int deletePerson(String name) {
		return simpleJdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	private void assertNumRowsInPersonTable(int expectedNumRows, String testState) {
		assertEquals(countRowsInTable("person"), expectedNumRows, "the number of rows in the person table ("
				+ testState + ").");
	}

	private void assertAddPerson(final String name) {
		assertEquals(createPerson(name), 1, "Adding '" + name + "'");
	}

	@BeforeClass
	public void beforeClass() {
		numSetUpCalls = 0;
		numSetUpCallsInTransaction = 0;
		numTearDownCalls = 0;
		numTearDownCallsInTransaction = 0;
	}

	@AfterClass
	public void afterClass() {
		assertEquals(numSetUpCalls, NUM_TESTS, "number of calls to setUp().");
		assertEquals(numSetUpCallsInTransaction, NUM_TX_TESTS, "number of calls to setUp() within a transaction.");
		assertEquals(numTearDownCalls, NUM_TESTS, "number of calls to tearDown().");
		assertEquals(numTearDownCallsInTransaction, NUM_TX_TESTS, "number of calls to tearDown() within a transaction.");
	}

	@Test
	@NotTransactional
	public void autowiringFromConfigClass() {
		assertNotNull(employee, "The employee should have been autowired.");
		assertEquals(employee.getName(), "John Smith");

		assertNotNull(pet, "The pet should have been autowired.");
		assertEquals(pet.getName(), "Fido");
	}

	@BeforeTransaction
	public void beforeTransaction() {
		assertNumRowsInPersonTable(1, "before a transactional test method");
		assertAddPerson(YODA);
	}

	@BeforeMethod
	public void setUp() throws Exception {
		numSetUpCalls++;
		if (inTransaction()) {
			numSetUpCallsInTransaction++;
		}
		assertNumRowsInPersonTable((inTransaction() ? 2 : 1), "before a test method");
	}

	@Test
	public void modifyTestDataWithinTransaction() {
		assertInTransaction(true);
		assertAddPerson(JANE);
		assertAddPerson(SUE);
		assertNumRowsInPersonTable(4, "in modifyTestDataWithinTransaction()");
	}

	@AfterMethod
	public void tearDown() throws Exception {
		numTearDownCalls++;
		if (inTransaction()) {
			numTearDownCallsInTransaction++;
		}
		assertNumRowsInPersonTable((inTransaction() ? 4 : 1), "after a test method");
	}

	@AfterTransaction
	public void afterTransaction() {
		assertEquals(deletePerson(YODA), 1, "Deleting yoda");
		assertNumRowsInPersonTable(1, "after a transactional test method");
	}


	@Configuration
	static class ContextConfiguration {

		@Bean
		public Employee employee() {
			Employee employee = new Employee();
			employee.setName("John Smith");
			employee.setAge(42);
			employee.setCompany("Acme Widgets, Inc.");
			return employee;
		}

		@Bean
		public Pet pet() {
			return new Pet("Fido");
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()//
			.addScript("classpath:/org/springframework/test/context/testng/schema.sql")//
			.addScript("classpath:/org/springframework/test/context/testng/data.sql")//
			.build();
		}

	}

}
