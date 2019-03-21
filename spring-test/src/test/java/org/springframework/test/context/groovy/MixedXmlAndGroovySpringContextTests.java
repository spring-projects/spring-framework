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
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.tests.sample.beans.Pet;

import static org.junit.Assert.*;

/**
 * Integration test class that verifies proper support for mixing XML
 * configuration files and Groovy scripts to load an {@code ApplicationContext}
 * using the TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "contextA.groovy", "contextB.xml" })
public class MixedXmlAndGroovySpringContextTests {

	@Autowired
	private Employee employee;

	@Autowired
	private Pet pet;

	@Autowired
	protected String foo;

	@Autowired
	protected String bar;


	@Test
	public final void verifyAnnotationAutowiredFields() {
		assertNotNull("The employee field should have been autowired.", this.employee);
		assertEquals("Dilbert", this.employee.getName());

		assertNotNull("The pet field should have been autowired.", this.pet);
		assertEquals("Dogbert", this.pet.getName());

		assertEquals("The foo field should have been autowired.", "Groovy Foo", this.foo);
		assertEquals("The bar field should have been autowired.", "XML Bar", this.bar);
	}

}
