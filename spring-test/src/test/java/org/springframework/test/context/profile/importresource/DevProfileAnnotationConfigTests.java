/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.profile.importresource;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 3.1
 */
@ActiveProfiles("dev")
class DevProfileAnnotationConfigTests extends DefaultProfileAnnotationConfigTests {

	@Test
	@Override
	void employee() {
		assertThat(employee).as("employee bean should be loaded for the 'dev' profile").isNotNull();
		assertThat(employee.getName()).isEqualTo("John Smith");
	}

}
