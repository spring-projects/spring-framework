/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.interfaces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.3
 */
@ExtendWith(SpringExtension.class)
class ActiveProfilesInterfaceTests implements ActiveProfilesTestInterface {

	@Autowired
	Employee employee;


	@Test
	void profileFromTestInterface() {
		assertThat(employee).isNotNull();
		assertThat(employee.getName()).isEqualTo("dev");
	}


	@Configuration
	static class Config {

		@Bean
		@Profile("dev")
		Employee employee1() {
			return new Employee("dev");
		}

		@Bean
		@Profile("prod")
		Employee employee2() {
			return new Employee("prod");
		}
	}

}
