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

package org.springframework.test.context.junit4;

import jakarta.annotation.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

/**
 * Combined integration test for {@link AbstractJUnit4SpringContextTests} and
 * {@link AbstractTransactionalJUnit4SpringContextTests}.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@ContextConfiguration
public class ConcreteTransactionalJUnit4SpringContextTests extends AbstractTransactionalJUnit4SpringContextTests
		implements BeanNameAware, InitializingBean {

	private static final String JANE = "jane";
	private static final String SUE = "sue";
	private static final String YODA = "yoda";

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


	@Before
	public void setUp() {
		long expected = (isActualTransactionActive() ? 2 : 1);
		assertThat(countRowsInPersonTable()).as("Verifying the number of rows in the person table before a test method.").isEqualTo(expected);
	}

	@After
	public void tearDown() {
		long expected = (isActualTransactionActive() ? 4 : 1);
		assertThat(countRowsInPersonTable()).as("Verifying the number of rows in the person table after a test method.").isEqualTo(expected);
	}

	@BeforeTransaction
	public void beforeTransaction() {
		assertThat(countRowsInPersonTable()).as("Verifying the number of rows in the person table before a transactional test method.").isEqualTo(1);
		assertThat(addPerson(YODA)).as("Adding yoda").isEqualTo(1);
	}

	@AfterTransaction
	public void afterTransaction() {
		assertThat(deletePerson(YODA)).as("Deleting yoda").isEqualTo(1);
		assertThat(countRowsInPersonTable()).as("Verifying the number of rows in the person table after a transactional test method.").isEqualTo(1);
	}


	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void verifyBeanNameSet() {
		assertThatTransaction().isNotActive();
		assertThat(this.beanName.startsWith(getClass().getName())).as("The bean name of this test instance should have been set to the fully qualified class name " +
				"due to BeanNameAware semantics.").isTrue();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void verifyApplicationContext() {
		assertThatTransaction().isNotActive();
		assertThat(super.applicationContext).as("The application context should have been set due to ApplicationContextAware semantics.").isNotNull();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void verifyBeanInitialized() {
		assertThatTransaction().isNotActive();
		assertThat(this.beanInitialized).as("This test bean should have been initialized due to InitializingBean semantics.").isTrue();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void verifyAnnotationAutowiredFields() {
		assertThatTransaction().isNotActive();
		assertThat(this.nonrequiredLong).as("The nonrequiredLong property should NOT have been autowired.").isNull();
		assertThat(this.pet).as("The pet field should have been autowired.").isNotNull();
		assertThat(this.pet.getName()).isEqualTo("Fido");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void verifyAnnotationAutowiredMethods() {
		assertThatTransaction().isNotActive();
		assertThat(this.employee).as("The employee setter method should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("John Smith");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void verifyResourceAnnotationWiredFields() {
		assertThatTransaction().isNotActive();
		assertThat(this.foo).as("The foo field should have been wired via @Resource.").isEqualTo("Foo");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void verifyResourceAnnotationWiredMethods() {
		assertThatTransaction().isNotActive();
		assertThat(this.bar).as("The bar method should have been wired via @Resource.").isEqualTo("Bar");
	}

	@Test
	public void modifyTestDataWithinTransaction() {
		assertThatTransaction().isActive();
		assertThat(addPerson(JANE)).as("Adding jane").isEqualTo(1);
		assertThat(addPerson(SUE)).as("Adding sue").isEqualTo(1);
		assertThat(countRowsInPersonTable()).as("Verifying the number of rows in the person table in modifyTestDataWithinTransaction().").isEqualTo(4);
	}


	private int addPerson(String name) {
		return super.jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	private int deletePerson(String name) {
		return super.jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	private int countRowsInPersonTable() {
		return countRowsInTable("person");
	}

}
