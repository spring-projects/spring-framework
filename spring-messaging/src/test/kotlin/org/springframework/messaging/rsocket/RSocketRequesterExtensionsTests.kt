package org.springframework.messaging.rsocket

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Mock object based tests for [RSocketRequester] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@FlowPreview
class RSocketRequesterExtensionsTests {

	@Test
	fun connectAndAwait() {
		val requester = mockk<RSocketRequester>()
		val builder = mockk<RSocketRequester.Builder>()
		every { builder.connect(any()) } returns Mono.just(requester)
		runBlocking {
			assertEquals(requester, builder.connectAndAwait(mockk()))
		}
	}

	@Test
	fun connectTcpAndAwait() {
		val host = "127.0.0.1"
		val requester = mockk<RSocketRequester>()
		val builder = mockk<RSocketRequester.Builder>()
		every { builder.connectTcp(host, anyInt()) } returns Mono.just(requester)
		runBlocking {
			assertEquals(requester, builder.connectTcpAndAwait(host, 0))
		}
	}

	@Test
	fun connectWebSocketAndAwait() {
		val requester = mockk<RSocketRequester>()
		val builder = mockk<RSocketRequester.Builder>()
		every { builder.connectWebSocket(any()) } returns Mono.just(requester)
		runBlocking {
			assertEquals(requester, builder.connectWebSocketAndAwait(mockk()))
		}
	}

	@Test
	fun dataFlowWithType() {
		val requestSpec = mockk<RSocketRequester.RequestSpec>()
		val responseSpec = mockk<RSocketRequester.ResponseSpec>()
		every { requestSpec.data(any<Publisher<String>>(), any<ParameterizedTypeReference<String>>()) } returns responseSpec
		assertEquals(responseSpec, requestSpec.data<String>(mockk()))
	}

	@Test
	fun dataFlowWithoutType() {
		val requestSpec = mockk<RSocketRequester.RequestSpec>()
		val responseSpec = mockk<RSocketRequester.ResponseSpec>()
		every { requestSpec.data(any()) } returns responseSpec
		assertEquals(responseSpec, requestSpec.data(mockk()))
	}

	@Test
	fun sendAndAwait() {
		val responseSpec = mockk<RSocketRequester.ResponseSpec>()
		every { responseSpec.send() } returns Mono.empty()
		runBlocking {
			responseSpec.sendAndAwait()
		}
	}

	@Test
	fun retrieveAndAwait() {
		val response = "foo"
		val responseSpec = mockk<RSocketRequester.ResponseSpec>()
		every { responseSpec.retrieveMono(any<ParameterizedTypeReference<String>>()) } returns Mono.just("foo")
		runBlocking {
			assertEquals(response, responseSpec.retrieveAndAwait())
		}
	}

	@Test
	fun retrieveFlow() {
		val responseSpec = mockk<RSocketRequester.ResponseSpec>()
		every { responseSpec.retrieveFlux(any<ParameterizedTypeReference<String>>()) } returns Flux.just("foo", "bar")
		runBlocking {
			assertEquals(listOf("foo", "bar"), responseSpec.retrieveFlow<String>().toList())
		}
	}

	@Test
	fun retrieveMono() {
		val responseSpec = mockk<RSocketRequester.ResponseSpec>()
		every { responseSpec.retrieveMono(any<ParameterizedTypeReference<String>>()) } returns Mono.just("foo")
		runBlocking {
			assertEquals("foo", responseSpec.retrieveMono<String>().block())
		}
	}

	@Test
	fun retrieveFlux() {
		val responseSpec = mockk<RSocketRequester.ResponseSpec>()
		every { responseSpec.retrieveFlux(any<ParameterizedTypeReference<String>>()) } returns Flux.just("foo", "bar")
		runBlocking {
			assertEquals(listOf("foo", "bar"), responseSpec.retrieveFlux<String>().collectList().block())
		}
	}
}
