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
