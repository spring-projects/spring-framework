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

package org.springframework.web.reactive.result.method.annotation

import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer
import java.util.UUID

class CoroutinesValueClassIntegrationTest : AbstractRequestMappingIntegrationTests() {

	override fun initApplicationContext(): ApplicationContext {
		val context = AnnotationConfigApplicationContext()
		context.register(WebConfig::class.java)
		context.refresh()
		return context
	}


	@ParameterizedHttpServerTest
	fun `Suspending handler method with nullable value class request param`(httpServer: HttpServer) {
		startServer(httpServer)

		val entity = performGet("/suspend-value-class?value=550e8400-e29b-41d4-a716-446655440000", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("550e8400-e29b-41d4-a716-446655440000")
	}

	@ParameterizedHttpServerTest
	fun `Suspending handler method with nullable value class request param omitted`(httpServer: HttpServer) {
		startServer(httpServer)

		val entity = performGet("/suspend-value-class", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("outer-null")
	}

	@ParameterizedHttpServerTest
	fun `Suspending handler method with non-optional nullable inner value class request param`(httpServer: HttpServer) {
		startServer(httpServer)

		val entity = performGet("/suspend-nullable-inner-value-class", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("inner-null")
	}

	@ParameterizedHttpServerTest
	fun `Suspending handler method with optional nullable inner value class request param`(httpServer: HttpServer) {
		startServer(httpServer)

		val entity = performGet("/suspend-nullable-inner-value-class-optional", HttpHeaders.EMPTY, String::class.java)
		assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(entity.body).isEqualTo("outer-null")
	}


	@Configuration
	@EnableWebFlux
	@ComponentScan(resourcePattern = "**/CoroutinesValueClassIntegrationTest*")
	open class WebConfig

	@RestController
	class CoroutinesController {

		@GetMapping("/suspend-value-class")
		suspend fun suspendingValueClassEndpoint(@RequestParam value: ValueClass?): String {
			delay(1)
			return when (value) {
				null -> "outer-null"
				else -> value.value.toString()
			}
		}

		@GetMapping("/suspend-nullable-inner-value-class")
		suspend fun suspendingNullableInnerValueClassEndpoint(
			@RequestParam(required = false) value: NullableInnerValueClass
		): String {
			delay(1)
			return if (value.value == null) "inner-null" else value.value.toString()
		}

		@GetMapping("/suspend-nullable-inner-value-class-optional")
		suspend fun suspendingOptionalNullableInnerValueClassEndpoint(
			@RequestParam(required = false) value: NullableInnerValueClass?
		): String {
			delay(1)
			return when {
				value == null -> "outer-null"
				value.value == null -> "inner-null"
				else -> value.value.toString()
			}
		}
	}

	@JvmInline
	value class ValueClass(val value: UUID)

	@JvmInline
	value class NullableInnerValueClass(val value: UUID?)

}
