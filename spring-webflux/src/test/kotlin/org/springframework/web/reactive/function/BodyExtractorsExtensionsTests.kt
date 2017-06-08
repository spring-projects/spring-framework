package org.springframework.web.reactive.function

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner


/**
 * Tests for [BodyExtractors] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class BodyExtractorsExtensionsTests {

	@Test
	fun `toMono with KClass`() {
		assertNotNull(toMono(Foo::class))
	}

	@Test
	fun `toMono with reified type parameter`() {
		assertNotNull(toMono<Foo>())
	}

	@Test
	fun `toFlux with KClass`() {
		assertNotNull(toFlux(Foo::class))
	}

	@Test
	fun `toFlux with reified type parameter`() {
		assertNotNull(toFlux<Foo>())
	}

	class Foo
}
