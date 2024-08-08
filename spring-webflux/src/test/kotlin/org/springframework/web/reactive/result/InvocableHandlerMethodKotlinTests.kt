/*
 * Copyright 2002-2024 the original author or authors.
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
import org.springframework.core.MethodParameter
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.HandlerResult
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.reactive.result.method.InvocableHandlerMethod
import org.springframework.web.reactive.result.method.annotation.ContinuationHandlerMethodArgumentResolver
import org.springframework.web.reactive.result.method.annotation.RequestParamMethodArgumentResolver
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get
import org.springframework.web.testfixture.method.ResolvableMethod
import org.springframework.web.testfixture.server.MockServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.lang.reflect.Method
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

/**
 * Kotlin unit tests for [InvocableHandlerMethod].
 *
 * @author Sebastien Deleuze
 */
class InvocableHandlerMethodKotlinTests {

	private var exchange = MockServerWebExchange.from(get("http://localhost:8080/path"))

	private val resolvers = mutableListOf(ContinuationHandlerMethodArgumentResolver(),
		RequestParamMethodArgumentResolver(null, ReactiveAdapterRegistry.getSharedInstance(), false))

	@Test
	fun resolveNoArg() {
		this.resolvers.add(stubResolver(null, String::class.java))
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
		val method = PrivateController::singleArg.javaMethod!!
		val result = invoke(PrivateController(), method,"foo")
		assertHandlerResultValue(result, "success:foo")
	}

	@Test
	fun privateFunction() {
		val method = PrivateController::class.java.getDeclaredMethod("private")
		val result = invoke(PrivateController(), method)
		assertHandlerResultValue(result, "private")
	}

	@Test
	fun defaultValue() {
		this.resolvers.add(stubResolver(null, String::class.java))
		val method = DefaultValueController::handle.javaMethod!!
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "default")
	}

	@Test
	fun defaultValueOverridden() {
		this.resolvers.add(stubResolver(null, String::class.java))
		val method = DefaultValueController::handle.javaMethod!!
		exchange = MockServerWebExchange.from(get("http://localhost:8080/path").queryParam("value", "override"))
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "override")
	}

	@Test
	fun defaultValues() {
		this.resolvers.add(stubResolver(null, Int::class.java))
		val method = DefaultValueController::handleMultiple.javaMethod!!
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "10-20")
	}

	@Test
	fun defaultValuesOverridden() {
		this.resolvers.add(stubResolver(null, Int::class.java))
		val method = DefaultValueController::handleMultiple.javaMethod!!
		exchange = MockServerWebExchange.from(get("http://localhost:8080/path").queryParam("limit2", "40"))
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "10-40")
	}

	@Test
	fun suspendingDefaultValue() {
		this.resolvers.add(stubResolver(null, String::class.java))
		val method = DefaultValueController::handleSuspending.javaMethod!!
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "default")
	}

	@Test
	fun suspendingDefaultValueOverridden() {
		this.resolvers.add(stubResolver(null, String::class.java))
		val method = DefaultValueController::handleSuspending.javaMethod!!
		exchange = MockServerWebExchange.from(get("http://localhost:8080/path").queryParam("value", "override"))
		val result = invoke(DefaultValueController(), method)
		assertHandlerResultValue(result, "override")
	}

	@Test
	fun unitReturnValue() {
		val method = NullResultController::unit.javaMethod!!
		val result = invoke(NullResultController(), method)
		assertHandlerResultValue(result, null)
	}

	@Test
	fun nullReturnValue() {
		val method = NullResultController::nullableReturnValue.javaMethod!!
		val result = invoke(NullResultController(), method)
		assertHandlerResultValue(result, null)
	}

	@Test
	fun nullParameter() {
		this.resolvers.add(stubResolver(null, String::class.java))
		val method = NullResultController::nullableParameter.javaMethod!!
		val result = invoke(NullResultController(), method, null)
		assertHandlerResultValue(result, null)
	}

	@Test
	fun valueClass() {
		this.resolvers.add(stubResolver(1L, Long::class.java))
		val method = ValueClassController::valueClass.javaMethod!!
		val result = invoke(ValueClassController(), method,1L)
		assertHandlerResultValue(result, "1")
	}

	@Test
	fun valueClassReturnValue() {
		val method = ValueClassController::valueClassReturnValue.javaMethod!!
		val result = invoke(ValueClassController(), method,)
		assertHandlerResultValue(result, "foo")
	}

	@Test
	fun valueClassWithDefaultValue() {
		this.resolvers.add(stubResolver(null, Double::class.java))
		val method = ValueClassController::valueClassWithDefault.javaMethod!!
		val result = invoke(ValueClassController(), method)
		assertHandlerResultValue(result, "3.1")
	}

	@Test
	fun valueClassWithInit() {
		this.resolvers.add(stubResolver("", String::class.java))
		val method = ValueClassController::valueClassWithInit.javaMethod!!
		val result = invoke(ValueClassController(), method)
		assertExceptionThrown(result, IllegalArgumentException::class)
	}

	@Test
	fun valueClassWithNullable() {
		this.resolvers.add(stubResolver(null, LongValueClass::class.java))
		val method = ValueClassController::valueClassWithNullable.javaMethod!!
		val result = invoke(ValueClassController(), method, null)
		assertHandlerResultValue(result, "null")
	}

	@Test
	fun valueClassWithPrivateConstructor() {
		this.resolvers.add(stubResolver(1L, Long::class.java))
		val method = ValueClassController::valueClassWithPrivateConstructor.javaMethod!!
		val result = invoke(ValueClassController(), method, 1L)
		assertHandlerResultValue(result, "1")
	}

	@Test
	fun propertyAccessor() {
		this.resolvers.add(stubResolver(null, String::class.java))
		val method = PropertyAccessorController::prop.getter.javaMethod!!
		val result = invoke(PropertyAccessorController(), method)
		assertHandlerResultValue(result, "foo")
	}

	@Test
	fun extension() {
		this.resolvers.add(stubResolver(CustomException("foo")))
		val method = ResolvableMethod.on(ExtensionHandler::class.java).argTypes(CustomException::class.java).resolveMethod()
		val result = invoke(ExtensionHandler(), method)
		assertHandlerResultValue(result, "foo")
	}

	@Test
	fun extensionWithParameter() {
		this.resolvers.add(stubResolver(CustomException("foo")))
		this.resolvers.add(stubResolver(20, Int::class.java))
		val method = ResolvableMethod.on(ExtensionHandler::class.java)
			.argTypes(CustomException::class.java, Int::class.javaPrimitiveType)
			.resolveMethod()
		val result = invoke(ExtensionHandler(), method)
		assertHandlerResultValue(result, "foo-20")
	}

	@Test
	fun genericParameter() {
		val horse = Animal("horse")
		this.resolvers.add(stubResolver(horse))
		val method = AnimalController::handle.javaMethod!!
		val result = invoke(AnimalController(), method, null)
		assertHandlerResultValue(result, horse.name)
	}


	private fun invokeForResult(handler: Any, method: Method, vararg providedArgs: Any): HandlerResult? {
		return invoke(handler, method, *providedArgs).block(Duration.ofSeconds(5))
	}

	private fun invoke(handler: Any, method: Method, vararg providedArgs: Any?): Mono<HandlerResult> {
		val invocable = InvocableHandlerMethod(handler, method)
		invocable.setArgumentResolvers(this.resolvers)
		return invocable.invoke(this.exchange, BindingContext(), *providedArgs)
	}

	private fun stubResolver(stubValue: Any): HandlerMethodArgumentResolver =
		stubResolver(stubValue, stubValue::class.java)

	private fun stubResolver(stubValue: Any?, stubClass: Class<*>): HandlerMethodArgumentResolver {
		val resolver = mockk<HandlerMethodArgumentResolver>()
		every { resolver.supportsParameter(any()) } answers { (it.invocation.args[0] as MethodParameter).getParameterType() == stubClass }
		every { resolver.resolveArgument(any(), any(), any()) } returns Mono.justOrEmpty(stubValue)
		return resolver
	}

	private fun assertHandlerResultValue(mono: Mono<HandlerResult>, expected: String?) {
		StepVerifier.create(mono)
				.consumeNextWith {
					if (it.returnValue is Mono<*>) {
						StepVerifier.create(it.returnValue as Mono<*>).expectNext(expected).verifyComplete()
					} else {
						assertThat(it.returnValue).isEqualTo(expected)
					}
				}.verifyComplete()
	}

	private fun assertExceptionThrown(mono: Mono<HandlerResult>, exceptionClass: KClass<out Throwable>) {
		StepVerifier.create(mono).verifyError(exceptionClass.java)
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

	private class PrivateController {

		suspend fun singleArg(q: String?): String {
			delay(1)
			return "success:$q"
		}

		private fun private() = "private"
	}

	class DefaultValueController {

		fun handle(@RequestParam value: String = "default") = value

		fun handleMultiple(@RequestParam(defaultValue = "10") limit1: Int, @RequestParam limit2: Int = 20) = "${limit1}-${limit2}"

		@Suppress("RedundantSuspendModifier")
		suspend fun handleSuspending(@RequestParam value: String = "default") = value
	}

	class NullResultController {

		fun unit() {
		}

		fun nullableReturnValue(): String? {
			return null
		}

		fun nullableParameter(value: String?): String? {
			return value
		}
	}

	class ValueClassController {

		fun valueClass(limit: LongValueClass) =
			"${limit.value}"

		fun valueClassReturnValue() =
			StringValueClass("foo")

		fun valueClassWithDefault(limit: DoubleValueClass = DoubleValueClass(3.1)) =
			"${limit.value}"

		fun valueClassWithInit(valueClass: ValueClassWithInit) =
			valueClass

		fun valueClassWithNullable(limit: LongValueClass?) =
			"${limit?.value}"

		fun valueClassWithPrivateConstructor(limit: ValueClassWithPrivateConstructor) =
			"${limit.value}"
	}

	class PropertyAccessorController {

		val prop: String
			@GetMapping("/")
			get() = "foo"
	}

	class ExtensionHandler {

		fun CustomException.handle(): String {
			return "${this.message}"
		}

		fun CustomException.handleWithParameter(limit: Int): String {
			return "${this.message}-$limit"
		}
	}

	private abstract class GenericController<T : Named> {

		fun handle(named: T) = named.name
	}

	private class AnimalController : GenericController<Animal>()

	interface Named {
		val name: String
	}

	data class Animal(override val name: String) : Named

	@JvmInline
	value class StringValueClass(val value: String)

	@JvmInline
	value class LongValueClass(val value: Long)

	@JvmInline
	value class DoubleValueClass(val value: Double)

	@JvmInline
	value class ValueClassWithInit(val value: String) {
		init {
			if (value.isEmpty()) {
				throw IllegalArgumentException()
			}
		}
	}

	@JvmInline
	value class ValueClassWithPrivateConstructor private constructor(val value: Long) {
		companion object {
			fun from(value: Long) = ValueClassWithPrivateConstructor(value)
		}
	}

	class CustomException(message: String) : Throwable(message)
}