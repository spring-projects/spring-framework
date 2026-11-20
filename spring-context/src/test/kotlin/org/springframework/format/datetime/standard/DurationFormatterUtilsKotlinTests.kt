/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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