/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.dao.support

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.dao.TypeMismatchDataAccessException
import java.util.Optional

/**
 * Kotlin tests for [DataAccessUtils].
 *
 * @see DataAccessUtilsTests
 */
class DataAccessUtilsKotlinTests {

	@Test  // gh-37438
	fun singleResultWithNullableElements() {
		val col: List<String?> = listOf(null)

		val fromCollection: String? = DataAccessUtils.singleResult(col)
		val fromStream: String? = DataAccessUtils.singleResult(col.stream())
		val fromIterator: String? = DataAccessUtils.singleResult(col.iterator())

		assertThat(fromCollection).isNull()
		assertThat(fromStream).isNull()
		assertThat(fromIterator).isNull()
	}

	@Test  // gh-37438
	fun uniqueResultWithNullableElements() {
		val col: List<String?> = listOf(null)

		val result: String? = DataAccessUtils.uniqueResult(col)

		assertThat(result).isNull()
	}

	@Test  // gh-37438
	fun optionalResultWithNullableElements() {
		val col: List<String?> = listOf(null)

		val fromCollection: Optional<String> = DataAccessUtils.optionalResult(col)
		val fromStream: Optional<String> = DataAccessUtils.optionalResult(col.stream())
		val fromIterator: Optional<String> = DataAccessUtils.optionalResult(col.iterator())

		assertThat(fromCollection).isEmpty()
		assertThat(fromStream).isEmpty()
		assertThat(fromIterator).isEmpty()
	}

	@Test  // gh-37438
	fun requiredResultWithNullableElements() {
		val col: List<String?> = listOf("foo")

		val singleResult: String = DataAccessUtils.requiredSingleResult(col)
		val uniqueResult: String = DataAccessUtils.requiredUniqueResult(col)

		assertThat(singleResult).isEqualTo("foo")
		assertThat(uniqueResult).isEqualTo("foo")

		val colWithNull: List<String?> = listOf(null)

		assertThatExceptionOfType(TypeMismatchDataAccessException::class.java)
			.isThrownBy { DataAccessUtils.requiredSingleResult(colWithNull) }
		assertThatExceptionOfType(TypeMismatchDataAccessException::class.java)
			.isThrownBy { DataAccessUtils.requiredUniqueResult(colWithNull) }
	}

}
