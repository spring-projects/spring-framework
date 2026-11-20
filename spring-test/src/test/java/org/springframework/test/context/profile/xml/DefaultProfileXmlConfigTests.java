/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.profile.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 3.1
 */
@SpringJUnitConfig
class DefaultProfileXmlConfigTests {

	@Autowired
	Pet pet;

	@Autowired(required = false)
	Employee employee;


	@Test
	void pet() {
		assertThat(pet).isNotNull();
		assertThat(pet.getName()).isEqualTo("Fido");
	}

	@Test
	void employee() {
		assertThat(employee).as("employee bean should not be created for the default profile").isNull();
	}

}
