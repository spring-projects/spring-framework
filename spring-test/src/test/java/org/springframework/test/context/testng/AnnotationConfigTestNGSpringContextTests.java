/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.testng;

import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for
 * {@link org.springframework.context.annotation.Configuration @Configuration} classes
 * with TestNG-based tests.
 *
 * <p>Configuration will be loaded from
 * {@link AnnotationConfigTestNGSpringContextTests.Config}.
 *
 * @author Sam Brannen
 * @since 5.1
 */
@ContextConfiguration
class AnnotationConfigTestNGSpringContextTests extends AbstractTestNGSpringContextTests {

	@Autowired
	Employee employee;

	@Autowired
	Pet pet;

	@Test
	void autowiringFromConfigClass() {
		assertThat(employee).as("The employee should have been autowired.").isNotNull();
		assertThat(employee.getName()).isEqualTo("John Smith");

		assertThat(pet).as("The pet should have been autowired.").isNotNull();
		assertThat(pet.getName()).isEqualTo("Fido");
	}


	@Configuration
	static class Config {

		@Bean
		Employee employee() {
			return new Employee("John Smith");
		}

		@Bean
		Pet pet() {
			return new Pet("Fido");
		}

	}

}
