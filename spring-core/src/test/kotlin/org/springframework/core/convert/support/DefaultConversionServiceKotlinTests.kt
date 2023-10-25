/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.convert.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for Kotlin support in [DefaultConversionService].
 *
 * @author Stephane Nicoll
 */
class DefaultConversionServiceKotlinTests {

	private val conversionService = DefaultConversionService()

	@Test
	fun stringToRegexEmptyString() {
		assertThat(conversionService.convert("", Regex::class.java)).isNull();
	}

	@Test
	fun stringToRegex() {
		val pattern = "\\w+"
		assertThat(conversionService.convert(pattern, Regex::class.java))
			.isInstanceOfSatisfying(Regex::class.java) { assertThat(it.pattern).isEqualTo(pattern) }
	}

	@Test
	fun regexToString() {
		val pattern = "\\w+"
		assertThat(conversionService.convert(pattern.toRegex(), String::class.java)).isEqualTo(pattern)
	}

}