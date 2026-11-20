/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
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
 */
@SpringJUnitConfig(loader = AnnotationConfigContextLoader.class, classes = DefaultConfigClassesBaseTests.ContextConfiguration.class)
class ExplicitConfigClassesBaseTests {

	@Autowired
	Employee employee;


	@Test
	void verifyEmployeeSetFromBaseContextConfig() {
		assertThat(this.employee).as("The employee should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("John Smith");
	}

}
