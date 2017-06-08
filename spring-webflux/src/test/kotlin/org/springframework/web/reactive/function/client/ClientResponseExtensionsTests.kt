package org.springframework.web.reactive.function.client

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

/**
 * Mock object based tests for [ClientResponse] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class ClientResponseExtensionsTests {

    @Mock(answer = Answers.RETURNS_MOCKS)
    lateinit var response: ClientResponse

    @Test
    fun `bodyToMono with KClass`() {
        response.bodyToMono(Foo::class)
        verify(response, Mockito.times(1)).bodyToMono(Foo::class.java)
    }

    @Test
    fun `bodyToMono with reified type parameters`() {
        response.bodyToMono<Foo>()
        verify(response, Mockito.times(1)).bodyToMono(Foo::class.java)
    }

    @Test
    fun `bodyToFlux with KClass`() {
        response.bodyToFlux(Foo::class)
        verify(response, Mockito.times(1)).bodyToFlux(Foo::class.java)
    }

    @Test
    fun `bodyToFlux with reified type parameters`() {
        response.bodyToFlux<Foo>()
        verify(response, Mockito.times(1)).bodyToFlux(Foo::class.java)
    }

    class Foo
}
