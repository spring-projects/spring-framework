/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for meta-annotation attribute override support, relying on
 * default attribute values defined in {@link ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@ExtendWith(SpringExtension.class)
@ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig
class ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfigTests {

	@Autowired
	private String foo;


	@Test
	void foo() {
		assertThat(foo).isEqualTo("Resolver Foo");
	}

}
