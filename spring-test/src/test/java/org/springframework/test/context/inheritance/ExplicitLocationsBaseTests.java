/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.inheritance;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit based integration test for verifying support for the
 * {@link ContextConfiguration#inheritLocations() inheritLocations} flag of
 * {@link ContextConfiguration @ContextConfiguration} indirectly proposed in <a
 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-3896"
 * target="_blank">SPR-3896</a>.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@SpringJUnitConfig(locations = "DefaultLocationsBaseTests-context.xml")
class ExplicitLocationsBaseTests {

	@Autowired
	Employee employee;


	@Test
	void verifyEmployeeSetFromBaseContextConfig() {
		assertThat(this.employee).as("The employee should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("John Smith");
	}

}
