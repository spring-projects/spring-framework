/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context.junit4.spr9051;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.transaction.TransactionTestUtils.assertInTransaction;
import static org.springframework.test.transaction.TransactionTestUtils.inTransaction;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * This set of tests (i.e., all concrete subclasses) investigates the claims made in
 * <a href="https://jira.springsource.org/browse/SPR-9051" target="_blank">SPR-9051</a>
 * with regard to transactional tests.
 *
 * @author Sam Brannen
 * @since 3.2
 * @see org.springframework.test.context.testng.AnnotationConfigTransactionalTestNGSpringContextTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractTransactionalAnnotatedConfigClassTests {

	protected static final String JANE = "jane";
	protected static final String SUE = "sue";
	protected static final String YODA = "yoda";

	protected DataSource dataSourceFromTxManager;
	protected DataSource dataSourceViaInjection;

	protected JdbcTemplate jdbcTemplate;

	@Autowired
	private Employee employee;


	@Autowired
	public void setTransactionManager(DataSourceTransactionManager transactionManager) {
		this.dataSourceFromTxManager = transactionManager.getDataSource();
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSourceViaInjection = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	private int countRowsInTable(String tableName) {
		return jdbcTemplate.queryForInt("SELECT COUNT(0) FROM " + tableName);
	}

	private int createPerson(String name) {
		return jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	protected int deletePerson(String name) {
		return jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	protected void assertNumRowsInPersonTable(int expectedNumRows, String testState) {
		assertEquals("the number of rows in the person table (" + testState + ").", expectedNumRows,
			countRowsInTable("person"));
	}

	protected void assertAddPerson(final String name) {
		assertEquals("Adding '" + name + "'", 1, createPerson(name));
	}

	@Test
	public void autowiringFromConfigClass() {
		assertNotNull("The employee should have been autowired.", employee);
		assertEquals("John Smith", employee.getName());
	}

	@BeforeTransaction
	public void beforeTransaction() {
		assertNumRowsInPersonTable(0, "before a transactional test method");
		assertAddPerson(YODA);
	}

	@Before
	public void setUp() throws Exception {
		assertNumRowsInPersonTable((inTransaction() ? 1 : 0), "before a test method");
	}

	@Test
	@Transactional
	public void modifyTestDataWithinTransaction() {
		assertInTransaction(true);
		assertAddPerson(JANE);
		assertAddPerson(SUE);
		assertNumRowsInPersonTable(3, "in modifyTestDataWithinTransaction()");
	}

	@After
	public void tearDown() throws Exception {
		assertNumRowsInPersonTable((inTransaction() ? 3 : 0), "after a test method");
	}

	@AfterTransaction
	public void afterTransaction() {
		assertEquals("Deleting yoda", 1, deletePerson(YODA));
		assertNumRowsInPersonTable(0, "after a transactional test method");
	}

}
