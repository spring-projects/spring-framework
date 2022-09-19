/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer
import org.springframework.web.testfixture.http.server.reactive.bootstrap.UndertowHttpServer
import reactor.core.publisher.Flux
import java.time.Duration

class CoroutinesIntegrationTests : AbstractRequestMappingIntegrationTests() {

	override fun initApplicationContext(): ApplicationContext {
		val context = AnnotationConfigApplicationContext()
		context.register(WebConfig::class.java)
		context.refresh()
		return context
	}


	@ParameterizedHttpServerTest
	fun `Suspending handler method`(httpServer: HttpServer) {
		startServer(httpServer)

		val entity = performGet<String>("/suspend", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("foo")
	}

	@ParameterizedHttpServerTest
	fun `Handler method returning Deferred`(httpServer: HttpServer) {
		startServer(httpServer)

		val entity = performGet<String>("/deferred", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("foo")
	}

	@ParameterizedHttpServerTest  // gh-27292
	fun `Suspending ResponseEntity handler method`(httpServer: HttpServer) {
		startServer(httpServer)

		val entity = performGet<String>("/suspend-response-entity", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("{\"value\":\"foo\"}")
	}

	@ParameterizedHttpServerTest
	fun `Handler method returning Flow`(httpServer: HttpServer) {
		startServer(httpServer)

		val entity = performGet<String>("/flow", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("foobar")
	}

	@ParameterizedHttpServerTest
	fun `Suspending handler method returning Flow`(httpServer: HttpServer) {
		startServer(httpServer)

		val entity = performGet<String>("/suspending-flow", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("foobar")
	}

	@ParameterizedHttpServerTest
	fun `Suspending handler method throwing exception`(httpServer: HttpServer) {
		startServer(httpServer)

		assertThatExceptionOfType(HttpServerErrorException.InternalServerError::class.java).isThrownBy {
			performGet<String>("/error", HttpHeaders.EMPTY, String::class.java)
		}
	}

	@ParameterizedHttpServerTest
	fun `Handler method returning Flow throwing exception`(httpServer: HttpServer) {
		startServer(httpServer)

		assertThatExceptionOfType(HttpServerErrorException.InternalServerError::class.java).isThrownBy {
			performGet<String>("/flow-error", HttpHeaders.EMPTY, String::class.java)
		}
	}

	@ParameterizedHttpServerTest
	fun `Suspending handler method returning ResponseEntity of Flux `(httpServer: HttpServer) {
		assumeFalse(httpServer is UndertowHttpServer, "Undertow currently fails")

		startServer(httpServer)

		val entity = performGet<String>("/entity-flux", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("01234")
	}

	@ParameterizedHttpServerTest
	fun `Suspending handler method returning ResponseEntity of Flow`(httpServer: HttpServer) {
		startServer(httpServer)

		val entity = performGet<String>("/entity-flow", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("foobar")
	}


	@Configuration
	@EnableWebFlux
	@ComponentScan(resourcePattern = "**/CoroutinesIntegrationTests*")
	open class WebConfig

	@OptIn(DelicateCoroutinesApi::class)
	@RestController
	class CoroutinesController {

		@GetMapping("/suspend")
		suspend fun suspendingEndpoint(): String {
			delay(1)
			return "foo"
		}

		@GetMapping("/deferred")
		fun deferredEndpoint(): Deferred<String> = GlobalScope.async {
			delay(1)
			"foo"
		}

		@GetMapping("/suspend-response-entity")
		suspend fun suspendingResponseEntityEndpoint(): ResponseEntity<FooContainer<String>> {
			delay(1)
			return ResponseEntity.ok(FooContainer("foo"))
		}

		@GetMapping("/flow")
		fun flowEndpoint()= flow {
			emit("foo")
			delay(1)
			emit("bar")
			delay(1)
		}

		@GetMapping("/suspending-flow")
		suspend fun suspendingFlowEndpoint(): Flow<String> {
			delay(10)
			return flow {
				emit("foo")
				delay(1)
				emit("bar")
				delay(1)
			}
		}

		@GetMapping("/error")
		suspend fun error() {
			delay(1)
			throw IllegalStateException()
		}

		@GetMapping("/flow-error")
		suspend fun flowError() = flow<String> {
			delay(1)
			throw IllegalStateException()
		}

		@GetMapping("/entity-flux")
		suspend fun entityFlux() : ResponseEntity<Flux<String>> {
			val strings = Flux.interval(Duration.ofMillis(100)).take(5)
					.map { l -> l.toString() }
			delay(10)
			return ResponseEntity.ok().body(strings)
		}

		@GetMapping("/entity-flow")
		suspend fun entityFlow() : ResponseEntity<Flow<String>> {
			val strings =  flow {
							emit("foo")
							delay(1)
							emit("bar")
							delay(1)
						}
			return ResponseEntity.ok().body(strings)
		}

	}


	class FooContainer<T>(val value: T)

}
