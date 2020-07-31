/*
 * Copyright 2002-2020 the original author or authors.
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
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

/**
 * Unit tests for [DatabaseClient] extensions.
 *
 * @author Sebastien Deleuze
 * @author Jonas Bark
 * @author Mark Paluch
 */
class DatabaseClientExtensionsTests {

	@Test
	fun bindByIndexShouldBindValue() {
		val spec = mockk<DatabaseClient.GenericExecuteSpec>()
		every { spec.bind(eq(0), any()) } returns spec

		runBlocking {
			spec.bind<String>(0, "foo")
		}

		verify {
			spec.bind(0, Parameter.fromOrEmpty("foo", String::class.java))
		}
	}

	@Test
	fun bindByIndexShouldBindNull() {
		val spec = mockk<DatabaseClient.GenericExecuteSpec>()
		every { spec.bind(eq(0), any()) } returns spec

		runBlocking {
			spec.bind<String>(0, null)
		}

		verify {
			spec.bind(0, Parameter.empty(String::class.java))
		}
	}

	@Test
	fun bindByNameShouldBindValue() {
		val spec = mockk<DatabaseClient.GenericExecuteSpec>()
		every { spec.bind(eq("field"), any()) } returns spec

		runBlocking {
			spec.bind<String>("field", "foo")
		}

		verify {
			spec.bind("field", Parameter.fromOrEmpty("foo", String::class.java))
		}
	}

	@Test
	fun bindByNameShouldBindNull() {
		val spec = mockk<DatabaseClient.GenericExecuteSpec>()
		every { spec.bind(eq("field"), any()) } returns spec

		runBlocking {
			spec.bind<String>("field", null)
		}

		verify {
			spec.bind("field", Parameter.empty(String::class.java))
		}
	}

	@Test
	fun genericExecuteSpecAwait() {
		val spec = mockk<DatabaseClient.GenericExecuteSpec>()
		every { spec.then() } returns Mono.empty()

		runBlocking {
			spec.await()
		}

		verify {
			spec.then()
		}
	}

}
