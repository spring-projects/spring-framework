/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.DelegatingSmartContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for configuration classes in
 * the Spring TestContext Framework in conjunction with the
 * {@link DelegatingSmartContextLoader}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
@ContextConfiguration(classes = DefaultLoaderBeanOverridingDefaultConfigClassesInheritedTests.Config.class)
class DefaultLoaderBeanOverridingExplicitConfigClassesInheritedTests extends
		DefaultLoaderExplicitConfigClassesBaseTests {

	@Test
	@Override
	void verifyEmployeeSetFromBaseContextConfig() {
		assertThat(this.employee).as("The employee should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).as("The employee bean should have been overridden.").isEqualTo("Yoda");
	}

}
