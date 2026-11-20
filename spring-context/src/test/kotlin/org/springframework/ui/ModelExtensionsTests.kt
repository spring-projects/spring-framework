/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for [Model] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class ModelExtensionsTests {

	@Test
	fun setAttribute() {
		val model:Model = ConcurrentModel()
		model["foo"] = "bing"
		assertThat(model.containsAttribute("foo")).isTrue()
		assertThat(model.asMap()["foo"]).isEqualTo("bing")
	}
}
