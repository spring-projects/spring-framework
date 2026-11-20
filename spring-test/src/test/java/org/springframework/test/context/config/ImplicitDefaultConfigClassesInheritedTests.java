/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Inherited tests for detection of implicit configuration classes.
 *
 * @author Sam Brannen
 * @since 7.0.2
 * @see DefaultConfigClassesInheritedTests
 */
class ImplicitDefaultConfigClassesInheritedTests extends ImplicitDefaultConfigClassesBaseTests {

	@Autowired
	String greeting2;


	@Test
	void greeting2() {
		// This class must NOT be annotated with @SpringJUnitConfig or @ContextConfiguration.
		assertThat(AnnotatedElementUtils.hasAnnotation(getClass(), ContextConfiguration.class)).isFalse();

		assertThat(greeting2).isEqualTo("TEST 2");
	}

	@Test
	void greetings(@Autowired List<String> greetings) {
		assertThat(greetings).containsExactly("TEST 1", "TEST 2");
	}


	@Configuration
	static class DefaultConfig2 {

		@Bean
		String greeting2() {
			return "TEST 2";
		}

		@Bean
		Integer puzzle2() {
			return 222;
		}
	}

}
