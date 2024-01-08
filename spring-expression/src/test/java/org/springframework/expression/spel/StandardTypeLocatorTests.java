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

package org.springframework.expression.spel;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.expression.spel.support.StandardTypeLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link StandardTypeLocator}.
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
class StandardTypeLocatorTests {

	private final StandardTypeLocator locator = new StandardTypeLocator();

	@ParameterizedTest(name = "[{index}] {0} --> {1}")
	@CsvSource(delimiterString = "-->", textBlock = """
			Boolean           --> java.lang.Boolean
			Character         --> java.lang.Character
			Number            --> java.lang.Number
			Integer           --> java.lang.Integer
			String            --> java.lang.String

			java.lang.Boolean --> java.lang.Boolean
			java.lang.Integer --> java.lang.Integer
			java.lang.String  --> java.lang.String
			""")
	void defaultImports(String typeName, Class<?> type) {
		assertThat(locator.findType(typeName)).isEqualTo(type);
	}

	@Test
	void importPrefixes() {
		assertThat(locator.getImportPrefixes()).containsExactly("java.lang");
	}

	@Test
	void typeNotFound() {
		assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> locator.findType("URL"))
				.extracting(SpelEvaluationException::getMessageCode)
					.isEqualTo(SpelMessage.TYPE_NOT_FOUND);
	}

	@Test
	void registerImport() {
		locator.registerImport("java.net");
		assertThat(locator.findType("URL")).isEqualTo(URL.class);
	}

}
