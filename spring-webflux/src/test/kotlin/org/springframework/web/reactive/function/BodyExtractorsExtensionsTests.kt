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
	fun `bodyToMono with reified type parameter`() {
		assertNotNull(bodyToMono<Foo>())
	}

	@Test
	fun `bodyToFlux with reified type parameter`() {
		assertNotNull(bodyToFlux<Foo>())
	}

	class Foo
}
