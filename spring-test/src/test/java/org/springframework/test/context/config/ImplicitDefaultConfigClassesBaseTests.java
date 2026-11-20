/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base tests for detection of implicit configuration classes.
 *
 * @author Sam Brannen
 * @since 7.0.2
 * @see DefaultConfigClassesBaseTests
 */
@ExtendWith(SpringExtension.class)
class ImplicitDefaultConfigClassesBaseTests {

	@Autowired
	String greeting1;

	@Autowired
	Integer puzzle1;


	@Test
	final void greeting1AndPuzzle1() {
		// This class must NOT be annotated with @SpringJUnitConfig or @ContextConfiguration.
		assertThat(AnnotatedElementUtils.hasAnnotation(getClass(), ContextConfiguration.class)).isFalse();

		assertThat(greeting1).isEqualTo("TEST 1");
		assertThat(puzzle1).isEqualTo(111);
	}


	@Configuration
	static class DefaultConfig1A {

		@Bean
		String greeting1() {
			return "TEST 1";
		}
	}

	@Configuration
	static class DefaultConfig1B {

		@Bean
		Integer puzzle1() {
			return 111;
		}
	}

}
