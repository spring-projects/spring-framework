/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.test.context.config.PojoAndStringConfig;
import org.springframework.test.context.config.meta.ConfigClassesAndProfilesWithCustomDefaultsMetaConfig.ProductionConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for meta-annotation attribute override support, overriding
 * default attribute values defined in {@link ConfigClassesAndProfilesWithCustomDefaultsMetaConfig}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(SpringExtension.class)
@ConfigClassesAndProfilesWithCustomDefaultsMetaConfig(
		classes = { PojoAndStringConfig.class, ProductionConfig.class }, profiles = "prod")
class ConfigClassesAndProfilesWithCustomDefaultsMetaConfigWithOverridesTests {

	@Autowired
	String foo;

	@Autowired
	Pet pet;

	@Autowired
	Employee employee;


	@Test
	void verifyEmployee() {
		assertThat(this.employee).as("The employee should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("John Smith");
	}

	@Test
	void verifyPet() {
		assertThat(this.pet).as("The pet should have been autowired.").isNotNull();
		assertThat(this.pet.getName()).isEqualTo("Fido");
	}

	@Test
	void verifyFoo() {
		assertThat(this.foo).isEqualTo("Production Foo");
	}

}
