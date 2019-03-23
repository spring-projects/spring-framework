/*
 * Copyright 2002-2014 the original author or authors.
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

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericGroovyApplicationContext;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.tests.sample.beans.Pet;

import static org.junit.Assert.*;

/**
 * Simple integration test to verify the expected functionality of
 * {@link GenericGroovyApplicationContext}, thereby validating the proper
 * syntax and configuration of {@code "context.groovy"} without using the
 * Spring TestContext Framework.
 *
 * <p>In other words, this test class serves merely as a <em>control group</em>
 * to ensure that there is nothing wrong with the Groovy script used by
 * other tests in this package.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class GroovyControlGroupTests {

	@Test
	@SuppressWarnings("resource")
	public void verifyScriptUsingGenericGroovyApplicationContext() {
		ApplicationContext ctx = new GenericGroovyApplicationContext(getClass(), "context.groovy");

		String foo = ctx.getBean("foo", String.class);
		assertEquals("Foo", foo);

		String bar = ctx.getBean("bar", String.class);
		assertEquals("Bar", bar);

		Pet pet = ctx.getBean(Pet.class);
		assertNotNull("pet", pet);
		assertEquals("Dogbert", pet.getName());

		Employee employee = ctx.getBean(Employee.class);
		assertNotNull("employee", employee);
		assertEquals("Dilbert", employee.getName());
		assertEquals("???", employee.getCompany());
	}

}
