/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for configuration classes in
 * the Spring TestContext Framework.
 *
 * <p>Configuration will be loaded from {@link DefaultConfigClassesBaseTests.ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 3.1
 * @see DefaultLoaderDefaultConfigClassesBaseTests
 */
@SpringJUnitConfig(loader = AnnotationConfigContextLoader.class)
class DefaultConfigClassesBaseTests {

	@Autowired
	Employee employee;


	@Test
	void verifyEmployeeSetFromBaseContextConfig() {
		assertThat(this.employee).as("The employee field should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("John Smith");
	}


	@Configuration(proxyBeanMethods = false)
	static class ContextConfiguration {

		@Bean
		Employee employee() {
			Employee employee = new Employee();
			employee.setName("John Smith");
			employee.setAge(42);
			employee.setCompany("Acme Widgets, Inc.");
			return employee;
		}
	}

}
