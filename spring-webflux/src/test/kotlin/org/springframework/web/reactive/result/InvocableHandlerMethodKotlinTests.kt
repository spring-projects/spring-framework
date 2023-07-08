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

package org.springframework.web.reactive.result

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.HandlerResult
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.reactive.result.method.InvocableHandlerMethod
import org.springframework.web.reactive.result.method.annotation.ContinuationHandlerMethodArgumentResolver
import org.springframework.web.reactive.result.method.annotation.RequestParamMethodArgumentResolver
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get
import org.springframework.web.testfixture.server.MockServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.lang.reflect.Method
import java.time.Duration
import kotlin.reflect.jvm.javaMethod

/**
 * Kotlin unit tests for [InvocableHandlerMethod].
 *
 * @author Sebastien Deleuze
 */
class InvocableHandlerMethodKotlinTests {

	private var exchange = MockServerWebExchange.from(get("http://localhost:8080/path"))

	private val resolvers = mutableListOf<HandlerMethodArgumentResolver>(ContinuationHandlerMethodArgumentResolver(),
		RequestParamMethodArgumentResolver(null, ReactiveAdapterRegistry.getSharedInstance(), false))

	@Test
	fun resolveNoArg() {
		this.resolvers.add(stubResolver(Mono.empty()))
		val method = CoroutinesController::singleArg.javaMethod!!
		val result = invoke(CoroutinesController(), method, null)
		assertHandlerResultValue(result, "success:null")
	}

	@Test
	fun resolveArg() {
		this.resolvers.add(stubResolver("foo"))
		val method = CoroutinesController::singleArg.javaMethod!!
		val result = invoke(CoroutinesController(), method,"foo")
		assertHandlerResultValue(result, "success:foo")
	}

	@Test
	fun resolveNoArgs() {
		val method = CoroutinesController::noArgs.javaMethod!!
		val result = invoke(CoroutinesController(), method)
		assertHandlerResultValue(result, "success")
	}

	@Test
	fun invocationTargetException() {
		val method = CoroutinesController::exceptionMethod.javaMethod!!
		val result = invoke(CoroutinesController(), method)

		StepVerifier.create(result)
				.consumeNextWith { StepVerifier.create(it.returnValue as Mono<*>).expectError(IllegalStateException::class.java).verify() }
				.verifyComplete()
	}

	@Test
	fun responseStatusAnnotation() {
		val method = CoroutinesController::created.javaMethod!!
		val result = invoke(CoroutinesController(), method)

		assertHandlerResultValue(result, "created")
		assertThat(this.exchange.response.statusCode).isSameAs(HttpStatus.CREATED)
	}

	@Test
	fun voidMethodWithResponseArg() {
		val response = this.exchange.response
		this.resolvers.add(stubResolver(response))
		val method = CoroutinesController::response.javaMethod!!
		val result = invokeForResult(CoroutinesController(), method, response)

		assertThat(result).`as`("Expected no result (i.e. fully handled)").isNull()
		assertThat(this.exchange.response.headers.getFirst("foo")).isEqualTo("bar")
	}

	@Test
	fun privateController() {
		this.resolvers.add(stubResolver("foo"))
		val method = PrivateCoroutinesController::singleArg.javaMethod!!
		val result = invoke(PrivateCoroutinesController(), method,"foo")
		assertHandlerResultValue(result, "success:foo")
	}

	@Test
	fun defaultValue() {
		this.resolvers.add(stubResolver(Mono.empty()))
		val method = DefaultValueController::handle.javaMethod!!
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "default")
	}

	@Test
	fun defaultValueOverridden() {
		this.resolvers.add(stubResolver(Mono.empty()))
		val method = DefaultValueController::handle.javaMethod!!
		exchange = MockServerWebExchange.from(get("http://localhost:8080/path").queryParam("value", "override"))
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "override")
	}

	@Test
	fun defaultValues() {
		this.resolvers.add(stubResolver(Mono.empty()))
		val method = DefaultValueController::handleMultiple.javaMethod!!
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "10-20")
	}

	@Test
	fun defaultValuesOverridden() {
		this.resolvers.add(stubResolver(Mono.empty()))
		val method = DefaultValueController::handleMultiple.javaMethod!!
		exchange = MockServerWebExchange.from(get("http://localhost:8080/path").queryParam("limit2", "40"))
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "10-40")
	}

	@Test
	fun suspendingDefaultValue() {
		this.resolvers.add(stubResolver(Mono.empty()))
		val method = DefaultValueController::handleSuspending.javaMethod!!
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "default")
	}

	@Test
	fun suspendingDefaultValueOverridden() {
		this.resolvers.add(stubResolver(Mono.empty()))
		val method = DefaultValueController::handleSuspending.javaMethod!!
		exchange = MockServerWebExchange.from(get("http://localhost:8080/path").queryParam("value", "override"))
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "override")
	}


	private fun invokeForResult(handler: Any, method: Method, vararg providedArgs: Any): HandlerResult? {
		return invoke(handler, method, *providedArgs).block(Duration.ofSeconds(5))
	}

	private fun invoke(handler: Any, method: Method, vararg providedArgs: Any?): Mono<HandlerResult> {
		val invocable = InvocableHandlerMethod(handler, method)
		invocable.setArgumentResolvers(this.resolvers)
		return invocable.invoke(this.exchange, BindingContext(), *providedArgs)
	}

	private fun stubResolver(stubValue: Any?): HandlerMethodArgumentResolver {
		return stubResolver(Mono.justOrEmpty(stubValue))
	}

	private fun stubResolver(stubValue: Mono<Any>): HandlerMethodArgumentResolver {
		val resolver = mockk<HandlerMethodArgumentResolver>()
		every { resolver.supportsParameter(any()) } returns true
		every { resolver.resolveArgument(any(), any(), any()) } returns stubValue
		return resolver
	}

	private fun assertHandlerResultValue(mono: Mono<HandlerResult>, expected: String) {
		StepVerifier.create(mono)
				.consumeNextWith {
					if (it.returnValue is Mono<*>) {
						StepVerifier.create(it.returnValue as Mono<*>).expectNext(expected).verifyComplete()
					} else {
						assertThat(it.returnValue).isEqualTo(expected)
					}
				}.verifyComplete()
	}

	class CoroutinesController {

		suspend fun singleArg(q: String?): String {
			delay(1)
			return "success:$q"
		}

		suspend fun noArgs(): String {
			delay(1)
			return "success"
		}

		suspend fun exceptionMethod() {
			throw IllegalStateException("boo")
		}

		@ResponseStatus(HttpStatus.CREATED)
		suspend fun created(): String {
			delay(1)
			return "created"
		}

		suspend fun response(response: ServerHttpResponse) {
			delay(1)
			response.headers.add("foo", "bar")
		}
	}

	private class PrivateCoroutinesController {

		suspend fun singleArg(q: String?): String {
			delay(1)
			return "success:$q"
		}
	}

	@RestController
	class DefaultValueController {

		fun handle(@RequestParam value: String = "default") = value

		fun handleMultiple(@RequestParam(defaultValue = "10") limit1: Int, @RequestParam limit2: Int = 20) = "${limit1}-${limit2}"

		@Suppress("RedundantSuspendModifier")
		suspend fun handleSuspending(@RequestParam value: String = "default") = value

	}
}