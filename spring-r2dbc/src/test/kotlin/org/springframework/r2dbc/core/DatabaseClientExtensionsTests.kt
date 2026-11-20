/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.r2dbc.spi.Parameters
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

/**
 * Tests for [DatabaseClient] extensions.
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
		spec.bind<String>(0, "foo")
		verify {
			spec.bind(0, Parameters.`in`("foo"))
		}
	}

	@Test
	fun bindByIndexShouldBindNull() {
		val spec = mockk<DatabaseClient.GenericExecuteSpec>()
		every { spec.bind(eq(0), any()) } returns spec
		spec.bind<String>(0, null)
		verify {
			spec.bind(0, Parameters.`in`(String::class.java))
		}
	}

	@Test
	fun bindByNameShouldBindValue() {
		val spec = mockk<DatabaseClient.GenericExecuteSpec>()
		every { spec.bind(eq("field"), any()) } returns spec
		spec.bind<String>("field", "foo")
		verify {
			spec.bind("field", Parameters.`in`("foo"))
		}
	}

	@Test
	fun bindByNameShouldBindNull() {
		val spec = mockk<DatabaseClient.GenericExecuteSpec>()
		every { spec.bind(eq("field"), any()) } returns spec
		spec.bind<String>("field", null)
		verify {
			spec.bind("field", Parameters.`in`(String::class.java))
		}
	}

	@Test
	suspend fun genericExecuteSpecAwait() {
		val spec = mockk<DatabaseClient.GenericExecuteSpec>()
		every { spec.then() } returns Mono.empty()
		spec.await()
		verify {
			spec.then()
		}
	}

}
