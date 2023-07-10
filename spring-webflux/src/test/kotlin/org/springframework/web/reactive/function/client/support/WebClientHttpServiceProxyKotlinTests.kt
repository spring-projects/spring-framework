/*
 * Copyright 2002-2023 the original author or authors.
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
package org.springframework.web.reactive.function.client.support

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.util.function.Consumer

/**
 * Kotlin integration tests for [HTTP Service proxy][HttpServiceProxyFactory]
 * using [WebClient] and [MockWebServer].
 *
 * @author DongHyeon Kim
 * @author Sebastien Deleuze
 */
@Suppress("DEPRECATION")
class KotlinWebClientHttpServiceProxyTests {

	private lateinit var server: MockWebServer

	@BeforeEach
	fun setUp() {
		server = MockWebServer()
	}

	@AfterEach
	fun shutdown() {
		server.shutdown()
	}

	@Test
	fun greetingSuspending() {
		prepareResponse { response: MockResponse ->
			response.setHeader(
				"Content-Type",
				"text/plain"
			).setBody("Hello Spring!")
		}
		runBlocking {
			val greeting = initHttpService().getGreetingSuspending()
			assertThat(greeting).isEqualTo("Hello Spring!")
		}
	}

	@Test
	fun greetingMono() {
		prepareResponse { response: MockResponse ->
			response.setHeader(
				"Content-Type",
				"text/plain"
			).setBody("Hello Spring!")
		}
		StepVerifier.create(initHttpService().getGreetingMono())
			.expectNext("Hello Spring!")
			.expectComplete()
			.verify(Duration.ofSeconds(5))
	}

	@Test
	fun greetingBlocking() {
		prepareResponse { response: MockResponse ->
			response.setHeader(
				"Content-Type",
				"text/plain"
			).setBody("Hello Spring!")
		}
		val greeting = initHttpService().getGreetingBlocking()
		assertThat(greeting).isEqualTo("Hello Spring!")
	}

	@Test
	fun greetingSuspendingWithRequestAttribute() {
		val attributes: MutableMap<String, Any> = HashMap()
		val webClient = WebClient.builder()
			.baseUrl(server.url("/").toString())
			.filter { request: ClientRequest, next: ExchangeFunction ->
				attributes.putAll(request.attributes())
				next.exchange(request)
			}
			.build()
		prepareResponse { response: MockResponse ->
			response.setHeader(
				"Content-Type",
				"text/plain"
			).setBody("Hello Spring!")
		}
		val service = initHttpService(webClient)
		runBlocking {
			val greeting = service.getGreetingSuspendingWithAttribute("myAttributeValue")
			assertThat(greeting).isEqualTo("Hello Spring!")
			assertThat(attributes).containsEntry("myAttribute", "myAttributeValue")
		}
	}

	private fun initHttpService(): TestHttpService {
		val webClient = WebClient.builder().baseUrl(
			server.url("/").toString()
		).build()
		return initHttpService(webClient)
	}

	private fun initHttpService(webClient: WebClient): TestHttpService {
		val adapter = WebClientAdapter.forClient(webClient)
		return HttpServiceProxyFactory.builderFor(adapter).build().createClient()
	}

	private fun prepareResponse(consumer: Consumer<MockResponse>) {
		val response = MockResponse()
		consumer.accept(response)
		server.enqueue(response)
	}

	private interface TestHttpService {
		@GetExchange("/greeting")
		suspend fun getGreetingSuspending(): String

		@GetExchange("/greeting")
		fun getGreetingMono(): Mono<String>

		@GetExchange("/greeting")
		fun getGreetingBlocking(): String

		@GetExchange("/greeting")
		suspend fun getGreetingSuspendingWithAttribute(@RequestAttribute myAttribute: String): String
	}
}
