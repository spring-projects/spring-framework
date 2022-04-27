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

package org.springframework.test.context.junit4.rules;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test which demonstrates how to use JUnit's {@link Parameterized}
 * runner in conjunction with {@link SpringClassRule} and {@link SpringMethodRule}
 * to provide dependency injection to a <em>parameterized test instance</em>.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see org.springframework.test.context.junit4.ParameterizedDependencyInjectionTests
 */
@RunWith(Parameterized.class)
@ContextConfiguration("/org/springframework/test/context/junit4/ParameterizedDependencyInjectionTests-context.xml")
public class ParameterizedSpringRuleTests {

	private static final AtomicInteger invocationCount = new AtomicInteger();

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Pet pet;

	@Parameter(0)
	public String employeeBeanName;

	@Parameter(1)
	public String employeeName;


	@Parameters(name = "bean [{0}], employee [{1}]")
	public static String[][] employeeData() {
		return new String[][] { { "employee1", "John Smith" }, { "employee2", "Jane Smith" } };
	}

	@BeforeClass
	public static void BeforeClass() {
		invocationCount.set(0);
	}

	@Test
	public final void verifyPetAndEmployee() {
		invocationCount.incrementAndGet();

		// Verifying dependency injection:
		assertThat(this.pet).as("The pet field should have been autowired.").isNotNull();

		// Verifying 'parameterized' support:
		Employee employee = this.applicationContext.getBean(this.employeeBeanName, Employee.class);
		assertThat(employee.getName()).as("Name of the employee configured as bean [" + this.employeeBeanName + "].").isEqualTo(this.employeeName);
	}

	@AfterClass
	public static void verifyNumParameterizedRuns() {
		assertThat(invocationCount.get()).as("Number of times the parameterized test method was executed.").isEqualTo(employeeData().length);
	}

}
