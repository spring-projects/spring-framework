/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.aot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.SpringProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.aot.TestContextAotGenerator.FAIL_ON_ERROR_PROPERTY_NAME;

/**
 * Tests for {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class TestContextAotGeneratorTests {

	@BeforeEach
	@AfterEach
	void resetFlag() {
		SpringProperties.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, null);
	}

	@Test
	void failOnErrorEnabledByDefault() {
		assertThat(createGenerator().failOnError).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "true", "  True\t" })
	void failOnErrorEnabledViaSpringProperty(String value) {
		SpringProperties.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, value);
		assertThat(createGenerator().failOnError).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "false", "  False\t", "x" })
	void failOnErrorDisabledViaSpringProperty(String value) {
		SpringProperties.setProperty(FAIL_ON_ERROR_PROPERTY_NAME, value);
		assertThat(createGenerator().failOnError).isFalse();
	}


	private static TestContextAotGenerator createGenerator() {
		return new TestContextAotGenerator(null);
	}

}
