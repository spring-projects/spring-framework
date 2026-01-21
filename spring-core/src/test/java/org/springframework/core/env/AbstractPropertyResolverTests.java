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

package org.springframework.core.env;

import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.SpringProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.core.env.AbstractPropertyResolver.DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME;
import static org.springframework.core.env.AbstractPropertyResolver.UNDEFINED_ESCAPE_CHARACTER;

/**
 * Unit tests for {@link AbstractPropertyResolver}.
 *
 * @author Sam Brannen
 * @since 6.2.7
 */
class AbstractPropertyResolverTests {

	@BeforeEach
	void resetStateBeforeEachTest() {
		resetState();
	}

	@AfterAll
	static void resetState() {
		AbstractPropertyResolver.defaultEscapeCharacter = UNDEFINED_ESCAPE_CHARACTER;
		setSpringProperty(null);
	}


	@Test
	void getDefaultEscapeCharacterWithSpringPropertySetToCharacterMinValue() {
		setSpringProperty("" + Character.MIN_VALUE);

		assertThatIllegalArgumentException()
				.isThrownBy(AbstractPropertyResolver::getDefaultEscapeCharacter)
				.withMessage("Value for property [%s] must not be Character.MIN_VALUE",
						DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME);

		assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo(UNDEFINED_ESCAPE_CHARACTER);
	}

	@Test
	void getDefaultEscapeCharacterWithSpringPropertySetToXyz() {
		setSpringProperty("XYZ");

		assertThatIllegalArgumentException()
				.isThrownBy(AbstractPropertyResolver::getDefaultEscapeCharacter)
				.withMessage("Value [XYZ] for property [%s] must be a single character or an empty string",
						DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME);

		assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo(UNDEFINED_ESCAPE_CHARACTER);
	}

	@Test
	void getDefaultEscapeCharacterWithSpringPropertySetToEmptyString() {
		setSpringProperty("");
		assertEscapeCharacter(null);
	}

	@Test
	void getDefaultEscapeCharacterWithoutSpringPropertySet() {
		assertEscapeCharacter('\\');
	}

	@Test
	void getDefaultEscapeCharacterWithSpringPropertySetToBackslash() {
		setSpringProperty("\\");
		assertEscapeCharacter('\\');
	}

	@Test
	void getDefaultEscapeCharacterWithSpringPropertySetToTilde() {
		setSpringProperty("~");
		assertEscapeCharacter('~');
	}

	@Test
	void getDefaultEscapeCharacterFromMultipleThreads() {
		setSpringProperty("~");

		IntStream.range(1, 32).parallel().forEach(__ ->
				assertThat(AbstractPropertyResolver.getDefaultEscapeCharacter()).isEqualTo('~'));

		assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo('~');
	}


	private static void setSpringProperty(String value) {
		SpringProperties.setProperty(DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME, value);
	}

	private static void assertEscapeCharacter(@Nullable Character expected) {
		assertThat(AbstractPropertyResolver.getDefaultEscapeCharacter()).isEqualTo(expected);
		assertThat(AbstractPropertyResolver.defaultEscapeCharacter).isEqualTo(expected);
	}

}
