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

package org.springframework.web.reactive.function.client

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.CoExchangeFilterFunction.Companion.COROUTINE_CONTEXT_ATTRIBUTE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Mock object based tests for [WebClient] Kotlin extensions
 *
 * @author Sebastien Deleuze
 * @author Dmitry Sulman
 */
class WebClientExtensionsTests {

	private val requestBodySpec = mockk<WebClient.RequestBodySpec>(relaxed = true)

	private val responseSpec = mockk<WebClient.ResponseSpec>(relaxed = true)


	@Test
	fun `RequestBodySpec#body with Publisher and reified type parameters`() {
		val body = mockk<Publisher<List<Foo>>>()
		requestBodySpec.body(body)
		verify { requestBodySpec.body(body, object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `RequestBodySpec#body with Flow and reified type parameters`() {
		val body = mockk<Flow<List<Foo>>>()
		requestBodySpec.body(body)
		verify { requestBodySpec.body(ofType<Any>(), object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `RequestBodySpec#body with CompletableFuture and reified type parameters`() {
		val body = mockk<CompletableFuture<List<Foo>>>()
		requestBodySpec.body<List<Foo>>(body)
		verify { requestBodySpec.body(ofType<Any>(), object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `RequestBodySpec#bodyValueWithType with reified type parameters`() {
		val body = mockk<List<Foo>>()
		requestBodySpec.bodyValueWithType<List<Foo>>(body)
		verify { requestBodySpec.bodyValue(body, object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#bodyToMono with reified type parameters`() {
		responseSpec.bodyToMono<List<Foo>>()
		verify { responseSpec.bodyToMono(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#bodyToFlux with reified type parameters`() {
		responseSpec.bodyToFlux<List<Foo>>()
		verify { responseSpec.bodyToFlux(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `bodyToFlow with reified type parameters`() {
		responseSpec.bodyToFlow<List<Foo>>()
		verify { responseSpec.bodyToFlux(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `awaitExchange with function parameter`() {
		val foo = mockk<Foo>()
		every { requestBodySpec.exchangeToMono(any<Function<ClientResponse, Mono<Foo>>>()) } returns Mono.just(foo)
		runBlocking {
			assertThat(requestBodySpec.awaitExchange { foo }).isEqualTo(foo)
		}
	}

	@Test
	fun `awaitExchange with coroutines context`() {
		val foo = mockk<Foo>()
		val slot = slot<Function<ClientResponse, Mono<Foo>>>()
		every { requestBodySpec.exchangeToMono(capture(slot)) } answers {
			slot.captured.apply(mockk<ClientResponse>())
		}
		runBlocking(FooContextElement(foo)) {
			assertThat(requestBodySpec.awaitExchange { currentCoroutineContext()[FooContextElement]!!.foo }).isEqualTo(foo)
		}
	}

	@Test
	fun `awaitExchangeOrNull returning null`() {
		val foo = mockk<Foo>()
		every { requestBodySpec.exchangeToMono(any<Function<ClientResponse, Mono<Foo>>>()) } returns Mono.empty()
		runBlocking {
			assertThat(requestBodySpec.awaitExchangeOrNull { foo }).isEqualTo(null)
		}
	}

	@Test
	fun `awaitExchangeOrNull returning object`() {
		val foo = mockk<Foo>()
		every { requestBodySpec.exchangeToMono(any<Function<ClientResponse, Mono<Foo>>>()) } returns Mono.just(foo)
		runBlocking {
			assertThat(requestBodySpec.awaitExchangeOrNull { foo }).isEqualTo(foo)
		}
	}

	@Test
	fun `awaitExchangeOrNull with coroutines context`() {
		val foo = mockk<Foo>()
		val slot = slot<Function<ClientResponse, Mono<Foo>>>()
		every { requestBodySpec.exchangeToMono(capture(slot)) } answers {
			slot.captured.apply(mockk<ClientResponse>())
		}
		runBlocking(FooContextElement(foo)) {
			assertThat(requestBodySpec.awaitExchangeOrNull { currentCoroutineContext()[FooContextElement]!!.foo }).isEqualTo(foo)
		}
	}

	@Test
	fun exchangeToFlow() {
		val foo = mockk<Foo>()
		every { requestBodySpec.exchangeToFlux(any<Function<ClientResponse, Flux<Foo>>>()) } returns Flux.just(foo, foo)
		runBlocking {
			assertThat(requestBodySpec.exchangeToFlow {
				flow {
					emit(foo)
					emit(foo)
				}
			}.toList()).isEqualTo(listOf(foo, foo))
		}
	}

	@Test
	fun awaitBody() {
		val spec = mockk<WebClient.ResponseSpec>()
		every { spec.bodyToMono<String>() } returns Mono.just("foo")
		runBlocking {
			assertThat(spec.awaitBody<String>()).isEqualTo("foo")
		}
	}

	@Test
	fun `awaitBody of type Unit`() {
		val spec = mockk<WebClient.ResponseSpec>()
		val entity = mockk<ResponseEntity<Void>>()
		every { spec.toBodilessEntity() } returns Mono.just(entity)
		runBlocking {
			assertThat(spec.awaitBody<Unit>()).isEqualTo(Unit)
		}
	}

	@Test
	fun awaitBodyOrNull() {
		val spec = mockk<WebClient.ResponseSpec>()
		every { spec.bodyToMono<String>() } returns Mono.just("foo")
		runBlocking {
			assertThat(spec.awaitBodyOrNull<String>()).isEqualTo("foo")
		}
	}

	@Test
	fun `awaitBodyOrNull of type Unit`() {
		val spec = mockk<WebClient.ResponseSpec>()
		val entity = mockk<ResponseEntity<Void>>()
		every { spec.toBodilessEntity() } returns Mono.just(entity)
		runBlocking {
			assertThat(spec.awaitBodyOrNull<Unit>()).isEqualTo(Unit)
		}
	}

	@Test
	fun awaitBodilessEntity() {
		val spec = mockk<WebClient.ResponseSpec>()
		val entity = mockk<ResponseEntity<Void>>()
		every { spec.toBodilessEntity() } returns Mono.just(entity)
		runBlocking {
			assertThat(spec.awaitBodilessEntity()).isEqualTo(entity)
		}
	}

	@Test
	fun `ResponseSpec#toEntity with reified type parameters`() {
		responseSpec.toEntity<List<Foo>>()
		verify { responseSpec.toEntity(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#toEntityList with reified type parameters`() {
		responseSpec.toEntityList<List<Foo>>()
		verify { responseSpec.toEntityList(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#toEntityFlux with reified type parameters`() {
		responseSpec.toEntityFlux<List<Foo>>()
		verify { responseSpec.toEntityFlux(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#awaitEntity with coroutine context propagation`() {
		val exchangeFunction = mockk<ExchangeFunction>()
		val mockResponse = mockk<ClientResponse>()
		val mockClientHeaders = mockk<ClientResponse.Headers>()
		val foo = mockk<Foo>()
		val slot = slot<ClientRequest>()
		every { exchangeFunction.exchange(capture(slot)) } returns Mono.just(mockResponse)
		every { mockResponse.statusCode() } returns HttpStatus.OK
		every { mockResponse.headers() } returns mockClientHeaders
		every { mockClientHeaders.asHttpHeaders() } returns HttpHeaders()
		every { mockResponse.bodyToMono(Foo::class.java) } returns Mono.just(foo)
		runBlocking(FooContextElement(foo)) {
			val responseEntity = WebClient.builder()
				.exchangeFunction(exchangeFunction)
				.filter(object : CoExchangeFilterFunction() {
					override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
						assertThat(currentCoroutineContext()[FooContextElement.Key]!!.foo).isEqualTo(foo)
						return next.exchange(request)
					}
				})
				.build().get().uri("/path").retrieve().awaitEntity<Foo>()
			val capturedContext = slot.captured.attribute(COROUTINE_CONTEXT_ATTRIBUTE).get() as CoroutineContext
			assertThat(capturedContext[FooContextElement.Key]!!.foo).isEqualTo(foo)
			assertThat(responseEntity.body).isEqualTo(foo)
		}
	}

	@Test
	fun `ResponseSpec#awaitEntity with coroutine context propagation to multiple CoExchangeFilterFunctions`() {
		val exchangeFunction = mockk<ExchangeFunction>()
		val mockResponse = mockk<ClientResponse>()
		val mockClientHeaders = mockk<ClientResponse.Headers>()
		val foo = mockk<Foo>()
		val slot = slot<ClientRequest>()
		every { exchangeFunction.exchange(capture(slot)) } returns Mono.just(mockResponse)
		every { mockResponse.statusCode() } returns HttpStatus.OK
		every { mockResponse.headers() } returns mockClientHeaders
		every { mockClientHeaders.asHttpHeaders() } returns HttpHeaders()
		every { mockResponse.bodyToMono(Foo::class.java) } returns Mono.just(foo)
		runBlocking {
			val responseEntity = WebClient.builder()
				.exchangeFunction(exchangeFunction)
				.filter(object : CoExchangeFilterFunction() {
					override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
						return withContext(FooContextElement(foo)) { next.exchange(request) }
					}
				})
				.filter(object : CoExchangeFilterFunction() {
					override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
						assertThat(currentCoroutineContext()[FooContextElement.Key]!!.foo).isEqualTo(foo)
						return next.exchange(request)
					}
				})
				.build().get().uri("/path").retrieve().awaitEntity<Foo>()
			val capturedContext = slot.captured.attribute(COROUTINE_CONTEXT_ATTRIBUTE).get() as CoroutineContext
			assertThat(capturedContext[FooContextElement.Key]!!.foo).isEqualTo(foo)
			assertThat(responseEntity.body).isEqualTo(foo)
		}
	}

	@Test
	fun `ResponseSpec#toEntity with coroutine context propagation to multiple CoExchangeFilterFunctions`() {
		val exchangeFunction = mockk<ExchangeFunction>()
		val mockResponse = mockk<ClientResponse>()
		val mockClientHeaders = mockk<ClientResponse.Headers>()
		val foo = mockk<Foo>()
		val slot = slot<ClientRequest>()
		every { exchangeFunction.exchange(capture(slot)) } returns Mono.just(mockResponse)
		every { mockResponse.statusCode() } returns HttpStatus.OK
		every { mockResponse.headers() } returns mockClientHeaders
		every { mockClientHeaders.asHttpHeaders() } returns HttpHeaders()
		every { mockResponse.bodyToMono(Foo::class.java) } returns Mono.just(foo)
		val responseEntity = WebClient.builder()
			.exchangeFunction(exchangeFunction)
			.filter(object : CoExchangeFilterFunction() {
				override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
					return withContext(FooContextElement(foo)) { next.exchange(request) }
				}
			})
			.filter(object : CoExchangeFilterFunction() {
				override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
					assertThat(currentCoroutineContext()[FooContextElement.Key]!!.foo).isEqualTo(foo)
					return next.exchange(request)
				}
			})
			.build().get().uri("/path").retrieve().toEntity(Foo::class.java)
			.block(Duration.ofSeconds(10))
		val capturedContext = slot.captured.attribute(COROUTINE_CONTEXT_ATTRIBUTE).get() as CoroutineContext
		assertThat(capturedContext[FooContextElement.Key]!!.foo).isEqualTo(foo)
		assertThat(responseEntity!!.body).isEqualTo(foo)
	}

	@Test
	fun `ResponseSpec#awaitExchange with coroutine context propagation`() {
		val exchangeFunction = mockk<ExchangeFunction>()
		val mockResponse = mockk<ClientResponse>()
		val foo = mockk<Foo>()
		val slot = slot<ClientRequest>()
		every { exchangeFunction.exchange(capture(slot)) } returns Mono.just(mockResponse)
		every { mockResponse.releaseBody() } returns Mono.empty()
		runBlocking(FooContextElement(foo)) {
			val responseBody = WebClient.builder()
				.exchangeFunction(exchangeFunction)
				.filter(object : CoExchangeFilterFunction() {
					override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
						assertThat(currentCoroutineContext()[FooContextElement.Key]!!.foo).isEqualTo(foo)
						return next.exchange(request)
					}
				})
				.build().get().uri("/path").awaitExchange { foo }
			val capturedContext = slot.captured.attribute(COROUTINE_CONTEXT_ATTRIBUTE).get() as CoroutineContext
			assertThat(capturedContext[FooContextElement.Key]!!.foo).isEqualTo(foo)
			assertThat(responseBody).isEqualTo(foo)
		}
	}

	@Test
	fun `ResponseSpec#awaitExchangeOrNull with coroutine context propagation`() {
		val exchangeFunction = mockk<ExchangeFunction>()
		val mockResponse = mockk<ClientResponse>()
		val foo = mockk<Foo>()
		val slot = slot<ClientRequest>()
		every { exchangeFunction.exchange(capture(slot)) } returns Mono.just(mockResponse)
		every { mockResponse.releaseBody() } returns Mono.empty()
		runBlocking(FooContextElement(foo)) {
			val responseBody = WebClient.builder()
				.exchangeFunction(exchangeFunction)
				.filter(object : CoExchangeFilterFunction() {
					override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
						assertThat(currentCoroutineContext()[FooContextElement.Key]!!.foo).isEqualTo(foo)
						return next.exchange(request)
					}
				})
				.build().get().uri("/path").awaitExchangeOrNull { foo }
			val capturedContext = slot.captured.attribute(COROUTINE_CONTEXT_ATTRIBUTE).get() as CoroutineContext
			assertThat(capturedContext[FooContextElement.Key]!!.foo).isEqualTo(foo)
			assertThat(responseBody).isEqualTo(foo)
		}
	}

	@Test
	fun `ResponseSpec#awaitBody with coroutine context propagation`() {
		val exchangeFunction = mockk<ExchangeFunction>()
		val mockResponse = mockk<ClientResponse>()
		val foo = mockk<Foo>()
		val slot = slot<ClientRequest>()
		every { exchangeFunction.exchange(capture(slot)) } returns Mono.just(mockResponse)
		every { mockResponse.statusCode() } returns HttpStatus.OK
		every { mockResponse.bodyToMono(object : ParameterizedTypeReference<Foo>() {}) } returns Mono.just(foo)
		runBlocking(FooContextElement(foo)) {
			val responseBody = WebClient.builder()
				.exchangeFunction(exchangeFunction)
				.filter(object : CoExchangeFilterFunction() {
					override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
						assertThat(currentCoroutineContext()[FooContextElement.Key]!!.foo).isEqualTo(foo)
						return next.exchange(request)
					}
				})
				.build().get().uri("/path").retrieve().awaitBody<Foo>()
			val capturedContext = slot.captured.attribute(COROUTINE_CONTEXT_ATTRIBUTE).get() as CoroutineContext
			assertThat(capturedContext[FooContextElement.Key]!!.foo).isEqualTo(foo)
			assertThat(responseBody).isEqualTo(foo)
		}
	}

	@Test
	fun `ResponseSpec#awaitBodyOrNull with coroutine context propagation`() {
		val exchangeFunction = mockk<ExchangeFunction>()
		val mockResponse = mockk<ClientResponse>()
		val foo = mockk<Foo>()
		val slot = slot<ClientRequest>()
		every { exchangeFunction.exchange(capture(slot)) } returns Mono.just(mockResponse)
		every { mockResponse.statusCode() } returns HttpStatus.OK
		every { mockResponse.bodyToMono(object : ParameterizedTypeReference<Foo>() {}) } returns Mono.just(foo)
		runBlocking(FooContextElement(foo)) {
			val responseBody = WebClient.builder()
				.exchangeFunction(exchangeFunction)
				.filter(object : CoExchangeFilterFunction() {
					override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
						assertThat(currentCoroutineContext()[FooContextElement.Key]!!.foo).isEqualTo(foo)
						return next.exchange(request)
					}
				})
				.build().get().uri("/path").retrieve().awaitBodyOrNull<Foo>()
			val capturedContext = slot.captured.attribute(COROUTINE_CONTEXT_ATTRIBUTE).get() as CoroutineContext
			assertThat(capturedContext[FooContextElement.Key]!!.foo).isEqualTo(foo)
			assertThat(responseBody).isEqualTo(foo)
		}
	}

	@Test
	fun `ResponseSpec#awaitBodilessEntity with coroutine context propagation`() {
		val exchangeFunction = mockk<ExchangeFunction>()
		val mockResponse = mockk<ClientResponse>()
		val mockClientHeaders = mockk<ClientResponse.Headers>()
		val foo = mockk<Foo>()
		val slot = slot<ClientRequest>()
		every { exchangeFunction.exchange(capture(slot)) } returns Mono.just(mockResponse)
		every { mockResponse.statusCode() } returns HttpStatus.OK
		every { mockResponse.releaseBody() } returns Mono.empty()
		every { mockResponse.headers() } returns mockClientHeaders
		every { mockClientHeaders.asHttpHeaders() } returns HttpHeaders()
		runBlocking(FooContextElement(foo)) {
			val responseEntity = WebClient.builder()
				.exchangeFunction(exchangeFunction)
				.filter(object : CoExchangeFilterFunction() {
					override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
						assertThat(currentCoroutineContext()[FooContextElement.Key]!!.foo).isEqualTo(foo)
						return next.exchange(request)
					}
				})
				.build().get().uri("/path").retrieve().awaitBodilessEntity()
			val capturedContext = slot.captured.attribute(COROUTINE_CONTEXT_ATTRIBUTE).get() as CoroutineContext
			assertThat(capturedContext[FooContextElement.Key]!!.foo).isEqualTo(foo)
			assertThat(responseEntity.hasBody()).isEqualTo(false)
		}
	}

	class Foo

	private data class FooContextElement(val foo: Foo) : AbstractCoroutineContextElement(FooContextElement) {
		companion object Key : CoroutineContext.Key<FooContextElement>
	}
}
