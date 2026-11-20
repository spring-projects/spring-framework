/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.annotation

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.aot.hint.*

/**
 * Kotlin tests for {@link ControllerMappingReflectiveProcessor}.
 *
 * @author Sebastien Deleuze
 */
class ControllerMappingReflectiveProcessorKotlinTests {

	private val processor = ControllerMappingReflectiveProcessor()

	private val hints = ReflectionHints()

	@Test
	fun registerReflectiveHintsForFunctionWithDefaultArgumentValue() {
		val method = SampleController::class.java.getDeclaredMethod("defaultValue", Boolean::class.javaObjectType)
		processor.registerReflectionHints(hints, method)
		Assertions.assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
			{
				Assertions.assertThat(it.type).isEqualTo(TypeReference.of(SampleController::class.java))
				Assertions.assertThat(it.methods()).extracting<String> { executableHint: ExecutableHint -> executableHint.name }
					.containsExactlyInAnyOrder("defaultValue", "defaultValue\$default")
			}
		)
	}

	class SampleController {

		@GetMapping("/defaultValue")
		fun defaultValue(@RequestParam(required = false) argument: Boolean? = false) = argument
	}

}
