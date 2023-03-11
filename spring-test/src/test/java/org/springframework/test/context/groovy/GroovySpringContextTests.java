/*
 * Copyright 2002-2023 the original author or authors.
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

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for loading an {@code ApplicationContext} from a
 * Groovy script with the TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("context.groovy")
class GroovySpringContextTests implements BeanNameAware, InitializingBean {

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
	void verifyBeanNameSet() {
		assertThat(this.beanName).as("The bean name of this test instance should have been set to the fully qualified class name " +
				"due to BeanNameAware semantics.").startsWith(getClass().getName());
	}

	@Test
	void verifyBeanInitialized() {
		assertThat(this.beanInitialized).as("This test bean should have been initialized due to InitializingBean semantics.").isTrue();
	}

	@Test
	void verifyAnnotationAutowiredFields() {
		assertThat(this.nonrequiredLong).as("The nonrequiredLong property should NOT have been autowired.").isNull();
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		assertThat(this.pet).as("The pet field should have been autowired.").isNotNull();
		assertThat(this.pet.getName()).isEqualTo("Dogbert");
	}

	@Test
	void verifyAnnotationAutowiredMethods() {
		assertThat(this.employee).as("The employee setter method should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("Dilbert");
	}

	@Test
	void verifyResourceAnnotationWiredFields() {
		assertThat(this.foo).as("The foo field should have been wired via @Resource.").isEqualTo("Foo");
	}

	@Test
	void verifyResourceAnnotationWiredMethods() {
		assertThat(this.bar).as("The bar method should have been wired via @Resource.").isEqualTo("Bar");
	}

}
