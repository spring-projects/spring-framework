/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.messaging.rsocket

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture

/**
 * Mock object based tests for [RSocketRequester] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class RSocketRequesterExtensionsTests {

	private val stringTypeRefMatcher: (ParameterizedTypeReference<*>) -> Boolean  = { it.type == String::class.java }

	@Test
	@Suppress("DEPRECATION")
	suspend fun connectAndAwait() {
		val requester = mockk<RSocketRequester>()
		val builder = mockk<RSocketRequester.Builder>()
		every { builder.connect(any()) } returns Mono.just(requester)
		assertThat(builder.connectAndAwait(mockk())).isEqualTo(requester)
	}

	@Test
	@Suppress("DEPRECATION")
	suspend fun connectTcpAndAwait() {
		val host = "127.0.0.1"
		val requester = mockk<RSocketRequester>()
		val builder = mockk<RSocketRequester.Builder>()
		every { builder.connectTcp(host, any()) } returns Mono.just(requester)
		assertThat(builder.connectTcpAndAwait(host, 0)).isEqualTo(requester)
	}

	@Test
	@Suppress("DEPRECATION")
	suspend fun connectWebSocketAndAwait() {
		val requester = mockk<RSocketRequester>()
		val builder = mockk<RSocketRequester.Builder>()
		every { builder.connectWebSocket(any()) } returns Mono.just(requester)
		assertThat(builder.connectWebSocketAndAwait(mockk())).isEqualTo(requester)
	}

	@Test
	fun `dataWithType with Publisher`() {
		val requestSpec = mockk<RSocketRequester.RequestSpec>()
		val data = mockk<Publisher<String>>()
		every { requestSpec.data(any<Publisher<String>>(), match<ParameterizedTypeReference<*>>(stringTypeRefMatcher)) } returns requestSpec
		assertThat(requestSpec.dataWithType(data)).isEqualTo(requestSpec)
	}

	@Test
	fun `dataWithType with Flow`() {
		val requestSpec = mockk<RSocketRequester.RequestSpec>()
		val data = mockk<Flow<String>>()
		every { requestSpec.data(any<Publisher<String>>(), match<ParameterizedTypeReference<*>>(stringTypeRefMatcher)) } returns requestSpec
		assertThat(requestSpec.dataWithType(data)).isEqualTo(requestSpec)
	}

	@Test
	fun `dataWithType with CompletableFuture`() {
		val requestSpec = mockk<RSocketRequester.RequestSpec>()
		val data = mockk<CompletableFuture<String>>()
		every { requestSpec.data(any<Publisher<String>>(), match<ParameterizedTypeReference<*>>(stringTypeRefMatcher)) } returns requestSpec
		assertThat(requestSpec.dataWithType<String>(data)).isEqualTo(requestSpec)
	}

	@Test
	fun dataFlowWithoutType() {
		val requestSpec = mockk<RSocketRequester.RequestSpec>()
		every { requestSpec.data(any()) } returns requestSpec
		assertThat(requestSpec.data(mockk())).isEqualTo(requestSpec)
	}

	@Test
	suspend fun sendAndAwait() {
		val retrieveSpec = mockk<RSocketRequester.RetrieveSpec>()
		every { retrieveSpec.send() } returns Mono.empty()
		retrieveSpec.sendAndAwait()
	}

	@Test
	suspend fun retrieveAndAwait() {
		val response = "foo"
		val retrieveSpec = mockk<RSocketRequester.RetrieveSpec>()
		every { retrieveSpec.retrieveMono(match<ParameterizedTypeReference<*>>(stringTypeRefMatcher)) } returns Mono.just("foo")
		assertThat(retrieveSpec.retrieveAndAwait<String>()).isEqualTo(response)
	}

	@Test
	suspend fun retrieveAndAwaitOrNull() {
		val retrieveSpec = mockk<RSocketRequester.RetrieveSpec>()
		every { retrieveSpec.retrieveMono(match<ParameterizedTypeReference<*>>(stringTypeRefMatcher)) } returns Mono.empty()
		assertThat(retrieveSpec.retrieveAndAwaitOrNull<String>()).isNull()
	}

	@Test
	suspend fun retrieveFlow() {
		val retrieveSpec = mockk<RSocketRequester.RetrieveSpec>()
		every { retrieveSpec.retrieveFlux(match<ParameterizedTypeReference<*>>(stringTypeRefMatcher)) } returns Flux.just("foo", "bar")
		assertThat(retrieveSpec.retrieveFlow<String>().toList()).contains("foo", "bar")
	}

	@Test
	fun retrieveMono() {
		val retrieveSpec = mockk<RSocketRequester.RetrieveSpec>()
		every { retrieveSpec.retrieveMono(match<ParameterizedTypeReference<*>>(stringTypeRefMatcher)) } returns Mono.just("foo")
		assertThat(retrieveSpec.retrieveMono<String>().block()).isEqualTo("foo")
	}

	@Test
	fun retrieveFlux() {
		val retrieveSpec = mockk<RSocketRequester.RetrieveSpec>()
		every { retrieveSpec.retrieveFlux(match<ParameterizedTypeReference<*>>(stringTypeRefMatcher)) } returns Flux.just("foo", "bar")
		assertThat(retrieveSpec.retrieveFlux<String>().collectList().block()).contains("foo", "bar")
	}
}
