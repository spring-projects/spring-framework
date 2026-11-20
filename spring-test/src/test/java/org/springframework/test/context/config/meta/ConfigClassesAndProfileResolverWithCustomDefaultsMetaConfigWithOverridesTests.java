/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for meta-annotation attribute override support, overriding
 * default attribute values defined in {@link ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@ExtendWith(SpringExtension.class)
@ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig(classes = LocalDevConfig.class, resolver = DevResolver.class)
class ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfigWithOverridesTests {

	@Autowired
	private String foo;


	@Test
	void foo() {
		assertThat(foo).isEqualTo("Local Dev Foo");
	}

}

@Configuration
@Profile("dev")
class LocalDevConfig {

	@Bean
	public String foo() {
		return "Local Dev Foo";
	}
}

class DevResolver implements ActiveProfilesResolver {

	@Override
	public String[] resolve(Class<?> testClass) {
		// Checking that the "test class" name ends with "*Tests" ensures that an actual
		// test class is passed to this method as opposed to a "*Config" class which would
		// imply that we likely have been passed the 'declaringClass' instead of the
		// 'rootDeclaringClass'.
		return testClass.getName().endsWith("Tests") ? new String[] { "dev" } : new String[] {};
	}
}
