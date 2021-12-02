/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.convert.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * Unit tests for {@link IntegerToEnumConverterFactory}.
 *
 * @author Adilson Antunes
 * @author Sam Brannen
 */
class IntegerToEnumConverterFactoryTests {

	private final Converter<Integer, Color> converter = new IntegerToEnumConverterFactory().getConverter(Color.class);


	@ParameterizedTest
	@CsvSource(textBlock = """
		0, RED
		1, BLUE
		2, GREEN
	""")
	void convertsIntegerToEnum(int index, Color color) {
		assertThat(converter.convert(index)).isEqualTo(color);
	}

	@Test
	void throwsArrayIndexOutOfBoundsExceptionIfInvalidEnumInteger() {
		assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
				.isThrownBy(() -> converter.convert(999));
	}


	enum Color {
		RED,
		BLUE,
		GREEN
	}

}
