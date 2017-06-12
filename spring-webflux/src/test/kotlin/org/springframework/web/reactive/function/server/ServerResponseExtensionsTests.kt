package org.springframework.web.reactive.function.server

import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.reactivestreams.Publisher

/**
 * Mock object based tests for [ServerResponse] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class ServerResponseExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var bodyBuilder: ServerResponse.BodyBuilder


	@Test
	fun `BodyBuilder#body with Publisher and reified type parameters`() {
		val body = mock<Publisher<Foo>>()
		bodyBuilder.body(body)
		verify(bodyBuilder, times(1)).body(body, Foo::class.java)
	}

	class Foo
}
