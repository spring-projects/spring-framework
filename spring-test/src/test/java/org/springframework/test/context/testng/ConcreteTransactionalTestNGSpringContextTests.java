/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.annotation.Resource;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.tests.sample.beans.Pet;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Combined integration test for {@link AbstractTestNGSpringContextTests} and
 * {@link AbstractTransactionalTestNGSpringContextTests}.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@ContextConfiguration
public class ConcreteTransactionalTestNGSpringContextTests extends AbstractTransactionalTestNGSpringContextTests
		implements BeanNameAware, InitializingBean {

	private static final String JANE = "jane";
	private static final String SUE = "sue";
	private static final String YODA = "yoda";

	private static final int NUM_TESTS = 8;
	private static final int NUM_TX_TESTS = 1;

	private static int numSetUpCalls = 0;
	private static int numSetUpCallsInTransaction = 0;
	private static int numTearDownCalls = 0;
	private static int numTearDownCallsInTransaction = 0;


	private Employee employee;

	@Autowired
	private Pet pet;

	@Autowired(required = false)
	private Long nonrequiredLong;

	@Resource
	private String foo;

	private String bar;

	private String beanName;

	private boolean beanInitialized = false;


	@Autowired
	private void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Resource
	private void setBar(String bar) {
		this.bar = bar;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void afterPropertiesSet() {
		this.beanInitialized = true;
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
		assertEquals(numSetUpCalls, NUM_TESTS, "number of calls to setUp().");
		assertEquals(numSetUpCallsInTransaction, NUM_TX_TESTS, "number of calls to setUp() within a transaction.");
		assertEquals(numTearDownCalls, NUM_TESTS, "number of calls to tearDown().");
		assertEquals(numTearDownCallsInTransaction, NUM_TX_TESTS, "number of calls to tearDown() within a transaction.");
	}

	@BeforeMethod
	void setUp() {
		numSetUpCalls++;
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			numSetUpCallsInTransaction++;
		}
		assertNumRowsInPersonTable((TransactionSynchronizationManager.isActualTransactionActive() ? 2 : 1), "before a test method");
	}

	@AfterMethod
	void tearDown() {
		numTearDownCalls++;
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			numTearDownCallsInTransaction++;
		}
		assertNumRowsInPersonTable((TransactionSynchronizationManager.isActualTransactionActive() ? 4 : 1), "after a test method");
	}

	@BeforeTransaction
	void beforeTransaction() {
		assertNumRowsInPersonTable(1, "before a transactional test method");
		assertAddPerson(YODA);
	}

	@AfterTransaction
	void afterTransaction() {
		assertEquals(deletePerson(YODA), 1, "Deleting yoda");
		assertNumRowsInPersonTable(1, "after a transactional test method");
	}


	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyBeanNameSet() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertTrue(this.beanName.startsWith(getClass().getName()), "The bean name of this test instance " +
				"should have been set to the fully qualified class name due to BeanNameAware semantics.");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyApplicationContextSet() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertNotNull(super.applicationContext,
				"The application context should have been set due to ApplicationContextAware semantics.");
		Employee employeeBean = (Employee) super.applicationContext.getBean("employee");
		assertEquals(employeeBean.getName(), "John Smith", "employee's name.");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyBeanInitialized() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertTrue(beanInitialized,
				"This test instance should have been initialized due to InitializingBean semantics.");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyAnnotationAutowiredFields() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertNull(nonrequiredLong, "The nonrequiredLong field should NOT have been autowired.");
		assertNotNull(pet, "The pet field should have been autowired.");
		assertEquals(pet.getName(), "Fido", "pet's name.");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyAnnotationAutowiredMethods() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertNotNull(employee, "The setEmployee() method should have been autowired.");
		assertEquals(employee.getName(), "John Smith", "employee's name.");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyResourceAnnotationInjectedFields() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertEquals(foo, "Foo", "The foo field should have been injected via @Resource.");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyResourceAnnotationInjectedMethods() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertEquals(bar, "Bar", "The setBar() method should have been injected via @Resource.");
	}

	@Test
	void modifyTestDataWithinTransaction() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
		assertAddPerson(JANE);
		assertAddPerson(SUE);
		assertNumRowsInPersonTable(4, "in modifyTestDataWithinTransaction()");
	}


	private int createPerson(String name) {
		return jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	private int deletePerson(String name) {
		return jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	private void assertNumRowsInPersonTable(int expectedNumRows, String testState) {
		assertEquals(countRowsInTable("person"), expectedNumRows,
				"the number of rows in the person table (" + testState + ").");
	}

	private void assertAddPerson(String name) {
		assertEquals(createPerson(name), 1, "Adding '" + name + "'");
	}

}
