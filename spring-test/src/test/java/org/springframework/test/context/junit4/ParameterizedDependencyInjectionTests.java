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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.tests.sample.beans.Pet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple JUnit 4 based integration test which demonstrates how to use JUnit's
 * {@link Parameterized} Runner in conjunction with
 * {@link ContextConfiguration @ContextConfiguration}, the
 * {@link DependencyInjectionTestExecutionListener}, and a
 * {@link TestContextManager} to provide dependency injection to a
 * <em>parameterized test instance</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.test.context.junit4.rules.ParameterizedSpringRuleTests
 */
@RunWith(Parameterized.class)
@ContextConfiguration
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
public class ParameterizedDependencyInjectionTests {

	private static final AtomicInteger invocationCount = new AtomicInteger();

	private static final TestContextManager testContextManager = new TestContextManager(ParameterizedDependencyInjectionTests.class);

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

	@Before
	public void injectDependencies() throws Exception {
		testContextManager.prepareTestInstance(this);
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
