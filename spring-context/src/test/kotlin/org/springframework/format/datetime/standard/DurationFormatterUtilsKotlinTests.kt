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

package org.springframework.format.datetime.standard

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.format.annotation.DurationFormat
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

/**
 * Tests for [DurationFormatterUtils] compatibility with Kotlin.
 *
 * @author Simon Baslé
 */
class DurationFormatterUtilsKotlinTests {

	@Test
	fun `Composite style parses Kotlin toString() with seconds resolution`() {
		val kotlinString = "-(1d 2h 34m 57s)"
		val duration = Duration.parse(kotlinString)

		val parseResult = DurationFormatterUtils.parse(kotlinString, DurationFormat.Style.COMPOSITE)
			.toKotlinDuration()

		Assertions.assertThat(parseResult).isEqualTo(duration)
	}

	@Test
	fun `Composite style fails Kotlin toString() with sub-second resolution`() {
		val kotlinString = "-(1d 2h 34m 57.028003002s)"

		Assertions.assertThatException().isThrownBy {
			DurationFormatterUtils.parse(kotlinString, DurationFormat.Style.COMPOSITE) }
			.withMessage("'$kotlinString' is not a valid composite duration")
	}

	@Test
	fun `Detect and parse fails Kotlin toString() with sub-second resolution`() {
		val kotlinString = "-(1d 2h 34m 57.028003002s)"

		Assertions.assertThatException().isThrownBy { DurationFormatterUtils.detectAndParse(kotlinString) }
			.withMessage("'$kotlinString' is not a valid duration, cannot detect any known style")
	}

}