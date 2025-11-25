/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
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

	@Nested
	class NestedTests {

		@Test
		void test(@Autowired String localGreeting) {
			assertThat(greeting).isEqualTo("TEST");
			assertThat(localGreeting).isEqualTo("TEST");
		}
	}

	@Configuration
	static class DefaultConfig {

		@Bean
		String greeting() {
			return "TEST";
		}
	}

}
