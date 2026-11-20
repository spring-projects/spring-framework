/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Kotlin tests for [ProblemDetail].
 */
class KotlinProblemDetailTests {

	@Test // gh-31430
	fun propertiesSetter() {
		val problemDetail = ProblemDetail()
		val map = mapOf("foo" to "bar")
		problemDetail.properties = map
		assertThat(problemDetail.properties).isEqualTo(map)
	}

}
