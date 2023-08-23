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
		assertThat(conversionService.convert(pattern, Regex::class.java)).isInstanceOfSatisfying(
			Regex::class.java
		) { regex -> assertThat(regex.pattern).isEqualTo(pattern) }
	}

	@Test
	fun regexToString() {
		val pattern = "\\w+"
		assertThat(conversionService.convert(pattern.toRegex(), String::class.java)).isEqualTo(pattern)
	}

}