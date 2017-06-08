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
	fun `fromPublisher with reified type parameters`() {
		val publisher = mock<Publisher<Foo>>()
		assertNotNull(fromPublisher(publisher))
	}

	@Test
	fun `fromServerSentEvents with reified type parameters`() {
		val publisher = mock<Publisher<Foo>>()
		assertNotNull(fromServerSentEvents(publisher))
	}

	class Foo
}
