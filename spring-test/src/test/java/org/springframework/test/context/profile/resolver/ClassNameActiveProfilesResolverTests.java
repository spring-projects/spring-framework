/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.profile.resolver;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michail Nikolaev
 * @author Sam Brannen
 * @since 4.0
 */
@SpringJUnitConfig
@ActiveProfiles(resolver = ClassNameActiveProfilesResolver.class)
class ClassNameActiveProfilesResolverTests {

	@Test
	void test(@Autowired Environment environment) {
		assertThat(environment.getActiveProfiles()).contains(getClass().getSimpleName());
	}

}
