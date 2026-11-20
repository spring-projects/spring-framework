/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.nested;

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
 * Integration tests for detection of default context configuration within
 * nested test class hierarchies without the use of {@link ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 7.0.2
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/31456">gh-31456</a>
 */
@ExtendWith(SpringExtension.class)
class DefaultContextConfigurationDetectionWithNestedTests {

	@Autowired
	String greeting;


	@Test
	void test(@Autowired String localGreeting) {
		// This class must NOT be annotated with @SpringJUnitConfig or @ContextConfiguration.
		assertThat(AnnotatedElementUtils.hasAnnotation(getClass(), ContextConfiguration.class)).isFalse();

		assertThat(greeting).isEqualTo("TEST");
		assertThat(localGreeting).isEqualTo("TEST");
	}


	/** for 7.1:
	@Nested
	class NestedTests {

		@Test
		void test(@Autowired String localGreeting) {
			assertThat(greeting).isEqualTo("TEST");
			assertThat(localGreeting).isEqualTo("TEST");
		}
	}
	*/


	@Configuration
	static class DefaultConfig {

		@Bean
		String greeting() {
			return "TEST";
		}
	}

}
