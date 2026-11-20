/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.inheritance;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.test.context.ContextConfiguration;

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
@ContextConfiguration
class DefaultLocationsInheritedTests extends DefaultLocationsBaseTests {

	@Autowired
	Pet pet;


	@Test
	void verifyPetSetFromExtendedContextConfig() {
		assertThat(this.pet).as("The pet should have been autowired.").isNotNull();
		assertThat(this.pet.getName()).isEqualTo("Fido");
	}

}
