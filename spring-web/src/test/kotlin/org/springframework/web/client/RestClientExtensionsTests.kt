/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.ParameterizedTypeReference
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

/**
 * Mock object based tests for [RestClient] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class RestClientExtensionsTests {

	private val requestBodySpec = mockk<RestClient.RequestBodySpec>(relaxed = true)

	private val responseSpec = mockk<RestClient.ResponseSpec>(relaxed = true)

	@Test
	fun `RequestBodySpec#body with reified type parameters`() {
		val body = mockk<List<Foo>>()
		requestBodySpec.bodyWithType(body)
		verify { requestBodySpec.hint(KType::class.jvmName, typeOf<List<Foo>>())
			.body(body, object : ParameterizedTypeReference<List<Foo>>() {})
		}
	}

	@Test
	fun `ResponseSpec#body with reified type parameters`() {
		responseSpec.body<List<Foo>>()
		verify { responseSpec.hint(KType::class.jvmName, typeOf<List<Foo>>())
			.body(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#requiredBody with reified type parameters`() {
		responseSpec.requiredBody<List<Foo>>()
		verify { responseSpec.hint(KType::class.jvmName, typeOf<List<Foo>>())
			.body(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#requiredBody with null response throws NoSuchElementException`() {
		every { responseSpec.hint(KType::class.jvmName, any()).body(any<ParameterizedTypeReference<Foo>>()) } returns null
		assertThrows<NoSuchElementException> { responseSpec.requiredBody<Foo>() }
	}

	@Test
	fun `ResponseSpec#toEntity with reified type parameters`() {
		responseSpec.toEntity<List<Foo>>()
		verify { responseSpec.hint(KType::class.jvmName, typeOf<List<Foo>>())
			.toEntity(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	private class Foo

}
