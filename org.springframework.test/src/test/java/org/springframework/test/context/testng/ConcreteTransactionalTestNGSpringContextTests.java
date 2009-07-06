/*
 * Copyright 2002-2009 the original author or authors.
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
import javax.sql.DataSource;

import org.springframework.beans.Employee;
import org.springframework.beans.Pet;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Combined unit test for {@link AbstractTestNGSpringContextTests} and
 * {@link AbstractTransactionalTestNGSpringContextTests}.
 * 
 * @author Sam Brannen
 * @since 2.5
 */
@ContextConfiguration
public class ConcreteTransactionalTestNGSpringContextTests extends AbstractTransactionalTestNGSpringContextTests
		implements BeanNameAware, InitializingBean {

	private static final String BOB = "bob";
	private static final String JANE = "jane";
	private static final String SUE = "sue";
	private static final String YODA = "yoda";

	private static int numSetUpCalls = 0;
	private static int numSetUpCallsInTransaction = 0;
	private static int numTearDownCalls = 0;
	private static int numTearDownCallsInTransaction = 0;

	// ------------------------------------------------------------------------|

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


	// ------------------------------------------------------------------------|

	private static int clearPersonTable(SimpleJdbcTemplate simpleJdbcTemplate) {
		return SimpleJdbcTestUtils.deleteFromTables(simpleJdbcTemplate, "person");
	}

	private static void createPersonTable(SimpleJdbcTemplate simpleJdbcTemplate) {
		try {
			simpleJdbcTemplate.update("CREATE TABLE person (name VARCHAR(20) NOT NULL, PRIMARY KEY(name))");
		}
		catch (BadSqlGrammarException bsge) {
			/* ignore */
		}
	}

	private static int countRowsInPersonTable(SimpleJdbcTemplate simpleJdbcTemplate) {
		return SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "person");
	}

	private static int addPerson(SimpleJdbcTemplate simpleJdbcTemplate, String name) {
		return simpleJdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	private static int deletePerson(SimpleJdbcTemplate simpleJdbcTemplate, String name) {
		return simpleJdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	// ------------------------------------------------------------------------|

	public void afterPropertiesSet() throws Exception {
		this.beanInitialized = true;
	}

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

	// ------------------------------------------------------------------------|

	private void assertNumRowsInPersonTable(int expectedNumRows, String testState) {
		assertEquals(countRowsInPersonTable(this.simpleJdbcTemplate), expectedNumRows,
			"Verifying the number of rows in the person table (" + testState + ").");
	}

	private void assertAddPerson(final String name) {
		assertEquals(addPerson(this.simpleJdbcTemplate, name), 1, "Adding '" + name + "'");
	}

	// ------------------------------------------------------------------------|

	@BeforeClass
	public void beforeClass() {
		numSetUpCalls = 0;
		numSetUpCallsInTransaction = 0;
		numTearDownCalls = 0;
		numTearDownCallsInTransaction = 0;
	}

	@AfterClass
	public void afterClass() {
		assertEquals(numSetUpCalls, 8, "Verifying number of calls to setUp().");
		assertEquals(numSetUpCallsInTransaction, 1, "Verifying number of calls to setUp() within a transaction.");
		assertEquals(numTearDownCalls, 8, "Verifying number of calls to tearDown().");
		assertEquals(numTearDownCallsInTransaction, 1, "Verifying number of calls to tearDown() within a transaction.");
	}

	@Test
	@NotTransactional
	public void verifyApplicationContextSet() {
		assertInTransaction(false);
		assertNotNull(super.applicationContext,
			"The application context should have been set due to ApplicationContextAware semantics.");
		Employee employeeBean = (Employee) super.applicationContext.getBean("employee");
		assertEquals(employeeBean.getName(), "John Smith", "Verifying employee's name.");
	}

	@Test
	@NotTransactional
	public void verifyBeanInitialized() {
		assertInTransaction(false);
		assertTrue(this.beanInitialized,
			"This test instance should have been initialized due to InitializingBean semantics.");
	}

	@Test
	@NotTransactional
	public void verifyBeanNameSet() {
		assertInTransaction(false);
		assertEquals(this.beanName, getClass().getName(),
			"The bean name of this test instance should have been set due to BeanNameAware semantics.");
	}

	@Test
	@NotTransactional
	public void verifyAnnotationAutowiredFields() {
		assertInTransaction(false);
		assertNull(this.nonrequiredLong, "The nonrequiredLong field should NOT have been autowired.");
		assertNotNull(this.pet, "The pet field should have been autowired.");
		assertEquals(this.pet.getName(), "Fido", "Verifying pet's name.");
	}

	@Test
	@NotTransactional
	public void verifyAnnotationAutowiredMethods() {
		assertInTransaction(false);
		assertNotNull(this.employee, "The setEmployee() method should have been autowired.");
		assertEquals(this.employee.getName(), "John Smith", "Verifying employee's name.");
	}

	@Test
	@NotTransactional
	public void verifyResourceAnnotationInjectedFields() {
		assertInTransaction(false);
		assertEquals(this.foo, "Foo", "The foo field should have been injected via @Resource.");
	}

	@Test
	@NotTransactional
	public void verifyResourceAnnotationInjectedMethods() {
		assertInTransaction(false);
		assertEquals(this.bar, "Bar", "The setBar() method should have been injected via @Resource.");
	}

	// ------------------------------------------------------------------------|

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
		assertEquals(deletePerson(this.simpleJdbcTemplate, YODA), 1, "Deleting yoda");
		assertNumRowsInPersonTable(1, "after a transactional test method");
	}


	// ------------------------------------------------------------------------|

	public static class DatabaseSetup {

		@Autowired
		void setDataSource(DataSource dataSource) {
			SimpleJdbcTemplate simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
			createPersonTable(simpleJdbcTemplate);
			clearPersonTable(simpleJdbcTemplate);
			addPerson(simpleJdbcTemplate, BOB);
		}
	}

}
