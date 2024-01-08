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

package org.springframework.r2dbc.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

/**
 * Tests for [UpdatedRowsFetchSpec] extensions.
 *
 * @author Fred Montariol
 */
class UpdatedRowsFetchSpecExtensionsTests {

	@Test
	fun awaitRowsUpdatedWithValue() {
		val spec = mockk<UpdatedRowsFetchSpec>()
		every { spec.rowsUpdated() } returns Mono.just(42)

		runBlocking {
			assertThat(spec.awaitRowsUpdated()).isEqualTo(42)
		}

		verify {
			spec.rowsUpdated()
		}
	}

}
