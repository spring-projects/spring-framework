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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Adilson Antunes
 */
class IntegerToEnumConverterFactoryTests {


	enum Colors {
		RED,
		BLUE,
		GREEN
	}

	@Test
	void convertIntegerToEnum() {
		final IntegerToEnumConverterFactory enumConverterFactory = new IntegerToEnumConverterFactory();
		assertThat(enumConverterFactory.getConverter(Colors.class).convert(0)).isEqualTo(Colors.RED);
		assertThat(enumConverterFactory.getConverter(Colors.class).convert(1)).isEqualTo(Colors.BLUE);
		assertThat(enumConverterFactory.getConverter(Colors.class).convert(2)).isEqualTo(Colors.GREEN);
	}

	@Test
	void throwsArrayIndexOutOfBoundsExceptionIfInvalidEnumInteger() {
		final IntegerToEnumConverterFactory enumConverterFactory = new IntegerToEnumConverterFactory();
		assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
				.isThrownBy(() -> enumConverterFactory.getConverter(Colors.class).convert(999));
	}


}
