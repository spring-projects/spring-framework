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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import javax.annotation.Resource;

import org.springframework.beans.Employee;
import org.springframework.beans.Pet;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Combined integration test for {@link AbstractTestNGSpringContextTests} and
 * {@link AbstractTransactionalTestNGSpringContextTests}.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@SuppressWarnings("deprecation")
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

	private boolean beanInitialized = false;

	private String beanName = "replace me with [" + getClass().getName() + "]";

	private Employee employee;

	@Autowired
	private Pet pet;

	@Autowired(required = false)
	protected Long nonrequiredLong;

	@Resource()
	protected String foo;

	protected String bar;


	private int createPerson(String name) {
		return simpleJdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	private int deletePerson(String name) {
		return simpleJdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.beanInitialized = true;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Autowired
	protected void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Resource
	protected void setBar(String bar) {
		this.bar = bar;
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
	public void verifyApplicationContextSet() {
		assertInTransaction(false);
		assertNotNull(super.applicationContext,
			"The application context should have been set due to ApplicationContextAware semantics.");
		Employee employeeBean = (Employee) super.applicationContext.getBean("employee");
		assertEquals(employeeBean.getName(), "John Smith", "employee's name.");
	}

	@Test
	@NotTransactional
	public void verifyBeanInitialized() {
		assertInTransaction(false);
		assertTrue(beanInitialized,
			"This test instance should have been initialized due to InitializingBean semantics.");
	}

	@Test
	@NotTransactional
	public void verifyBeanNameSet() {
		assertInTransaction(false);
		assertEquals(beanName, getClass().getName(),
			"The bean name of this test instance should have been set due to BeanNameAware semantics.");
	}

	@Test
	@NotTransactional
	public void verifyAnnotationAutowiredFields() {
		assertInTransaction(false);
		assertNull(nonrequiredLong, "The nonrequiredLong field should NOT have been autowired.");
		assertNotNull(pet, "The pet field should have been autowired.");
		assertEquals(pet.getName(), "Fido", "pet's name.");
	}

	@Test
	@NotTransactional
	public void verifyAnnotationAutowiredMethods() {
		assertInTransaction(false);
		assertNotNull(employee, "The setEmployee() method should have been autowired.");
		assertEquals(employee.getName(), "John Smith", "employee's name.");
	}

	@Test
	@NotTransactional
	public void verifyResourceAnnotationInjectedFields() {
		assertInTransaction(false);
		assertEquals(foo, "Foo", "The foo field should have been injected via @Resource.");
	}

	@Test
	@NotTransactional
	public void verifyResourceAnnotationInjectedMethods() {
		assertInTransaction(false);
		assertEquals(bar, "Bar", "The setBar() method should have been injected via @Resource.");
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

}
