package org.springframework.web.reactive.function

import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.reactivestreams.Publisher


/**
 * Tests for [BodyExtractors] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class BodyInsertersExtensionsTests {

	@Test
	fun `bodyFromPublisher with reified type parameters`() {
		val publisher = mock<Publisher<Foo>>()
		assertNotNull(bodyFromPublisher(publisher))
	}

	@Test
	fun `bodyFromServerSentEvents with reified type parameters`() {
		val publisher = mock<Publisher<Foo>>()
		assertNotNull(bodyFromServerSentEvents(publisher))
	}

	class Foo
}
