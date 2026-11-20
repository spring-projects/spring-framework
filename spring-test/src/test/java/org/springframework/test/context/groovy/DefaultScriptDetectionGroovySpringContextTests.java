/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.groovy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test class that verifies proper detection of a default
 * Groovy script (as opposed to a default XML config file).
 *
 * @author Sam Brannen
 * @since 4.1
 * @see DefaultScriptDetectionGroovySpringContextTestsContext
 */
@SpringJUnitConfig
// Config loaded from DefaultScriptDetectionGroovySpringContextTestsContext.groovy
class DefaultScriptDetectionGroovySpringContextTests {

	@Autowired
	Employee employee;

	@Autowired
	Pet pet;

	@Autowired
	String foo;


	@Test
	void verifyAnnotationAutowiredFields() {
		assertThat(this.employee).as("The employee field should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("Dilbert");

		assertThat(this.pet).as("The pet field should have been autowired.").isNotNull();
		assertThat(this.pet.getName()).isEqualTo("Dogbert");

		assertThat(this.foo).as("The foo field should have been autowired.").isEqualTo("Foo");
	}

}
