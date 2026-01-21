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


	@Test
	void greeting1() {
		// This class must NOT be annotated with @SpringJUnitConfig or @ContextConfiguration.
		assertThat(AnnotatedElementUtils.hasAnnotation(getClass(), ContextConfiguration.class)).isFalse();

		assertThat(greeting1).isEqualTo("TEST 1");
	}


	@Configuration
	static class DefaultConfig {

		@Bean
		String greeting1() {
			return "TEST 1";
		}
	}

}
