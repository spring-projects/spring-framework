/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.groovy;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.tests.sample.beans.Pet;

import static org.junit.Assert.*;

/**
 * Integration tests for loading an {@code ApplicationContext} from a
 * Groovy script with the TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("context.groovy")
public class GroovySpringContextTests implements BeanNameAware, InitializingBean {

	private Employee employee;

	@Autowired
	private Pet pet;

	@Autowired(required = false)
	protected Long nonrequiredLong;

	@Resource
	protected String foo;

	protected String bar;

	@Autowired
	private ApplicationContext applicationContext;

	private String beanName;

	private boolean beanInitialized = false;


	@Autowired
	protected void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Resource
	protected void setBar(String bar) {
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


	@Test
	public void verifyBeanNameSet() {
		assertTrue("The bean name of this test instance should have been set to the fully qualified class name " +
				"due to BeanNameAware semantics.", this.beanName.startsWith(getClass().getName()));
	}

	@Test
	public void verifyBeanInitialized() {
		assertTrue("This test bean should have been initialized due to InitializingBean semantics.",
				this.beanInitialized);
	}

	@Test
	public void verifyAnnotationAutowiredFields() {
		assertNull("The nonrequiredLong property should NOT have been autowired.", this.nonrequiredLong);
		assertNotNull("The application context should have been autowired.", this.applicationContext);
		assertNotNull("The pet field should have been autowired.", this.pet);
		assertEquals("Dogbert", this.pet.getName());
	}

	@Test
	public void verifyAnnotationAutowiredMethods() {
		assertNotNull("The employee setter method should have been autowired.", this.employee);
		assertEquals("Dilbert", this.employee.getName());
	}

	@Test
	public void verifyResourceAnnotationWiredFields() {
		assertEquals("The foo field should have been wired via @Resource.", "Foo", this.foo);
	}

	@Test
	public void verifyResourceAnnotationWiredMethods() {
		assertEquals("The bar method should have been wired via @Resource.", "Bar", this.bar);
	}

}
