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

package org.springframework.test.context.junit38;

import java.util.ArrayList;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.beans.Employee;
import org.springframework.beans.Pet;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

/**
 * Combined integration test for {@link AbstractJUnit38SpringContextTests} and
 * {@link AbstractTransactionalJUnit38SpringContextTests}.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@SuppressWarnings("deprecation")
@RunWith(JUnit38ClassRunner.class)
@ContextConfiguration
public class ConcreteTransactionalJUnit38SpringContextTests extends AbstractTransactionalJUnit38SpringContextTests
		implements BeanNameAware, InitializingBean {

	protected static final String BOB = "bob";
	protected static final String JANE = "jane";
	protected static final String SUE = "sue";
	protected static final String YODA = "yoda";

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

	private boolean inTransaction = false;


	public ConcreteTransactionalJUnit38SpringContextTests() throws Exception {
		this(null);
	}

	public ConcreteTransactionalJUnit38SpringContextTests(final String name) throws Exception {
		super(name);
	}

	protected static int clearPersonTable(final SimpleJdbcTemplate simpleJdbcTemplate) {
		return SimpleJdbcTestUtils.deleteFromTables(simpleJdbcTemplate, "person");
	}

	protected static void createPersonTable(final SimpleJdbcTemplate simpleJdbcTemplate) {
		try {
			simpleJdbcTemplate.update("CREATE TABLE person (name VARCHAR(20) NOT NULL, PRIMARY KEY(name))");
		}
		catch (final BadSqlGrammarException bsge) {
			/* ignore */
		}
	}

	protected static int countRowsInPersonTable(final SimpleJdbcTemplate simpleJdbcTemplate) {
		return SimpleJdbcTestUtils.countRowsInTable(simpleJdbcTemplate, "person");
	}

	protected static int addPerson(final SimpleJdbcTemplate simpleJdbcTemplate, final String name) {
		return simpleJdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	protected static int deletePerson(final SimpleJdbcTemplate simpleJdbcTemplate, final String name) {
		return simpleJdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	@Override
	public final void afterPropertiesSet() throws Exception {
		this.beanInitialized = true;
	}

	@Override
	public final void setBeanName(final String beanName) {
		this.beanName = beanName;
	}

	@Autowired
	protected final void setEmployee(final Employee employee) {
		this.employee = employee;
	}

	@Resource
	protected final void setBar(final String bar) {
		this.bar = bar;
	}

	@NotTransactional
	@Timed(millis = 10000)
	public void testNoOpShouldNotTimeOut() throws Exception {
		/* no-op */
	}

	@NotTransactional
	@ExpectedException(IndexOutOfBoundsException.class)
	public void testExpectedExceptionAnnotation() {
		new ArrayList<Object>().get(1);
	}

	@NotTransactional
	public void testApplicationContextSet() {
		assertNotNull("The application context should have been set due to ApplicationContextAware semantics.",
			super.applicationContext);
	}

	@NotTransactional
	public void testBeanInitialized() {
		assertTrue("This test bean should have been initialized due to InitializingBean semantics.",
			this.beanInitialized);
	}

	@NotTransactional
	public void testBeanNameSet() {
		assertEquals("The bean name of this test instance should have been set to the fully qualified class name "
				+ "due to BeanNameAware semantics.", getClass().getName(), this.beanName);
	}

	@NotTransactional
	public void testAnnotationAutowiredFields() {
		assertNull("The nonrequiredLong property should NOT have been autowired.", this.nonrequiredLong);
		assertNotNull("The pet field should have been autowired.", this.pet);
		assertEquals("Fido", this.pet.getName());
	}

	@NotTransactional
	public void testAnnotationAutowiredMethods() {
		assertNotNull("The employee setter method should have been autowired.", this.employee);
		assertEquals("John Smith", this.employee.getName());
	}

	@NotTransactional
	public void testResourceAnnotationWiredFields() {
		assertEquals("The foo field should have been wired via @Resource.", "Foo", this.foo);
	}

	@NotTransactional
	public void testResourceAnnotationWiredMethods() {
		assertEquals("The bar method should have been wired via @Resource.", "Bar", this.bar);
	}

	@BeforeTransaction
	public void beforeTransaction() {
		this.inTransaction = true;
		assertEquals("Verifying the number of rows in the person table before a transactional test method.", 1,
			countRowsInPersonTable(super.simpleJdbcTemplate));
		assertEquals("Adding yoda", 1, addPerson(super.simpleJdbcTemplate, YODA));
	}

	@Override
	public void setUp() throws Exception {
		assertEquals("Verifying the number of rows in the person table before a test method.", (this.inTransaction ? 2
				: 1), countRowsInPersonTable(super.simpleJdbcTemplate));
	}

	public void testModifyTestDataWithinTransaction() {
		assertEquals("Adding jane", 1, addPerson(super.simpleJdbcTemplate, JANE));
		assertEquals("Adding sue", 1, addPerson(super.simpleJdbcTemplate, SUE));
		assertEquals("Verifying the number of rows in the person table within transactionalMethod2().", 4,
			countRowsInPersonTable(super.simpleJdbcTemplate));
	}

	@Override
	public void tearDown() throws Exception {
		assertEquals("Verifying the number of rows in the person table after a test method.", (this.inTransaction ? 4
				: 1), countRowsInPersonTable(super.simpleJdbcTemplate));
	}

	@AfterTransaction
	public void afterTransaction() {
		assertEquals("Deleting yoda", 1, deletePerson(super.simpleJdbcTemplate, YODA));
		assertEquals("Verifying the number of rows in the person table after a transactional test method.", 1,
			countRowsInPersonTable(super.simpleJdbcTemplate));
	}


	public static class DatabaseSetup {

		@Autowired
		void setDataSource(final DataSource dataSource) {
			final SimpleJdbcTemplate simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
			createPersonTable(simpleJdbcTemplate);
			clearPersonTable(simpleJdbcTemplate);
			addPerson(simpleJdbcTemplate, BOB);
		}
	}

}
