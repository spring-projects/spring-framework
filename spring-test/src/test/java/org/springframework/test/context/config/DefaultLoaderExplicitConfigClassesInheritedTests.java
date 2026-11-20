/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
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
@SpringJUnitConfig(DefaultLoaderDefaultConfigClassesInheritedTests.Config.class)
class DefaultLoaderExplicitConfigClassesInheritedTests extends DefaultLoaderExplicitConfigClassesBaseTests {

	@Autowired
	Pet pet;


	@Test
	void verifyPetSetFromExtendedContextConfig() {
		assertThat(this.pet).as("The pet should have been autowired.").isNotNull();
		assertThat(this.pet.getName()).isEqualTo("Fido");
	}

}
