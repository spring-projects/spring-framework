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
package org.springframework.core

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Kotlin tests for [KotlinDetector].
 *
 * @author Sebastien Deleuze
 */
class KotlinDetectorTests {

	@Test
	fun isKotlinType() {
		Assertions.assertThat(KotlinDetector.isKotlinType(KotlinDetectorTests::class.java)).isTrue()
	}

	@Test
	fun isNotKotlinType() {
		Assertions.assertThat(KotlinDetector.isKotlinType(KotlinDetector::class.java)).isFalse()
	}

	@Test
	fun isInlineClass() {
		Assertions.assertThat(KotlinDetector.isInlineClass(ValueClass::class.java)).isTrue()
	}

	@Test
	fun isNotInlineClass() {
		Assertions.assertThat(KotlinDetector.isInlineClass(KotlinDetector::class.java)).isFalse()
		Assertions.assertThat(KotlinDetector.isInlineClass(KotlinDetectorTests::class.java)).isFalse()
	}

	@JvmInline
	value class ValueClass(val value: String)

}
