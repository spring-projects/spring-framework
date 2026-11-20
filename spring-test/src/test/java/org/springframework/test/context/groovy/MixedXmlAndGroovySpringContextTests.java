/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.groovy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test class that verifies proper support for mixing XML
 * configuration files and Groovy scripts to load an {@code ApplicationContext}
 * using the TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "contextA.groovy", "contextB.xml" })
class MixedXmlAndGroovySpringContextTests {

	@Autowired
	Employee employee;

	@Autowired
	Pet pet;

	@Autowired
	String foo;

	@Autowired
	String bar;


	@Test
	void verifyAnnotationAutowiredFields() {
		assertThat(this.employee).as("The employee field should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("Dilbert");

		assertThat(this.pet).as("The pet field should have been autowired.").isNotNull();
		assertThat(this.pet.getName()).isEqualTo("Dogbert");

		assertThat(this.foo).as("The foo field should have been autowired.").isEqualTo("Groovy Foo");
		assertThat(this.bar).as("The bar field should have been autowired.").isEqualTo("XML Bar");
	}

}
