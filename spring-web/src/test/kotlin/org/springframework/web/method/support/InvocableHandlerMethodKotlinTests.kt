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

package org.springframework.web.method.support

import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.testfixture.servlet.MockHttpServletRequest
import org.springframework.web.testfixture.servlet.MockHttpServletResponse
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.lang.reflect.Method
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

/**
 * Kotlin unit tests for [InvocableHandlerMethod].
 *
 * @author Sebastien Deleuze
 */
@Suppress("UNCHECKED_CAST")
class InvocableHandlerMethodKotlinTests {

	private val request: NativeWebRequest = ServletWebRequest(MockHttpServletRequest(), MockHttpServletResponse())

	private val composite = HandlerMethodArgumentResolverComposite()

	@Test
	fun intDefaultValue() {
		composite.addResolver(StubArgumentResolver(Int::class.java, null))
		val value = getInvocable(Handler::intDefaultValue.javaMethod!!).invokeForRequest(request, null)

		Assertions.assertThat(getStubResolver(0).resolvedParameters).hasSize(1)
		Assertions.assertThat(value).isEqualTo("20")
	}

	@Test
	fun booleanDefaultValue() {
		composite.addResolver(StubArgumentResolver(Boolean::class.java, null))
		val value = getInvocable(Handler::booleanDefaultValue.javaMethod!!).invokeForRequest(request, null)

		Assertions.assertThat(getStubResolver(0).resolvedParameters).hasSize(1)
		Assertions.assertThat(value).isEqualTo("true")
	}

	@Test
	fun nullableIntDefaultValue() {
		composite.addResolver(StubArgumentResolver(Int::class.javaObjectType, null))
		val value = getInvocable(Handler::nullableIntDefaultValue.javaMethod!!).invokeForRequest(request, null)

		Assertions.assertThat(getStubResolver(0).resolvedParameters).hasSize(1)
		Assertions.assertThat(value).isEqualTo("20")
	}

	@Test
	fun nullableBooleanDefaultValue() {
		composite.addResolver(StubArgumentResolver(Boolean::class.javaObjectType, null))
		val value = getInvocable(Handler::nullableBooleanDefaultValue.javaMethod!!).invokeForRequest(request, null)

		Assertions.assertThat(getStubResolver(0).resolvedParameters).hasSize(1)
		Assertions.assertThat(value).isEqualTo("true")
	}

	@Test
	fun unitReturnValue() {
		val value = getInvocable(Handler::unit.javaMethod!!).invokeForRequest(request, null)
		Assertions.assertThat(value).isNull()
	}

	@Test
	fun nullReturnValue() {
		composite.addResolver(StubArgumentResolver(String::class.java, null))
		val value = getInvocable(Handler::nullable.javaMethod!!).invokeForRequest(request, null)
		Assertions.assertThat(value).isNull()
	}

	@Test
	fun private() {
		composite.addResolver(StubArgumentResolver(Float::class.java, 1.2f))
		val value = getInvocable(ReflectionUtils.findMethod(Handler::class.java, "private", Float::class.java)!!).invokeForRequest(request, null)

		Assertions.assertThat(getStubResolver(0).resolvedParameters).hasSize(1)
		Assertions.assertThat(value).isEqualTo("1.2")
	}

	@Test
	fun valueClass() {
		composite.addResolver(StubArgumentResolver(Long::class.java, 1L))
		val value = getInvocable(ValueClassHandler::longValueClass.javaMethod!!).invokeForRequest(request, null)
		Assertions.assertThat(value).isEqualTo(1L)
	}

	@Test
	fun valueClassReturnValue() {
		val value = getInvocable(ValueClassHandler::valueClassReturnValue.javaMethod!!).invokeForRequest(request, null)
		Assertions.assertThat(value).isEqualTo("foo")
	}

	@Test
	fun resultOfUnitReturnValue() {
		val value = getInvocable(ValueClassHandler::resultOfUnitReturnValue.javaMethod!!).invokeForRequest(request, null)
		Assertions.assertThat(value).isNull()
	}

	@Test
	fun valueClassDefaultValue() {
		composite.addResolver(StubArgumentResolver(Double::class.java))
		val value = getInvocable(ValueClassHandler::doubleValueClass.javaMethod!!).invokeForRequest(request, null)
		Assertions.assertThat(value).isEqualTo(3.1)
	}

	@Test
	fun valueClassWithInit() {
		composite.addResolver(StubArgumentResolver(String::class.java, ""))
		val invocable = getInvocable(ValueClassHandler::valueClassWithInit.javaMethod!!)
		Assertions.assertThatIllegalArgumentException().isThrownBy { invocable.invokeForRequest(request, null) }
	}

	@Test
	fun valueClassWithNullable() {
		composite.addResolver(StubArgumentResolver(LongValueClass::class.java, null))
		val value = getInvocable(ValueClassHandler::valueClassWithNullable.javaMethod!!).invokeForRequest(request, null)
		Assertions.assertThat(value).isNull()
	}

	@Test
	fun valueClassWithPrivateConstructor() {
		composite.addResolver(StubArgumentResolver(Char::class.java, 'a'))
		val value = getInvocable(ValueClassHandler::valueClassWithPrivateConstructor.javaMethod!!).invokeForRequest(request, null)
		Assertions.assertThat(value).isEqualTo('a')
	}

	@Test
	fun suspendingValueClass() {
		composite.addResolver(ContinuationHandlerMethodArgumentResolver())
		composite.addResolver(StubArgumentResolver(Long::class.java, 1L))
		val value = getInvocable(SuspendingValueClassHandler::longValueClass.javaMethod!!).invokeForRequest(request, null)
		StepVerifier.create(value as Mono<Long>).expectNext(1L).verifyComplete()
	}

	@Test
	fun suspendingValueClassReturnValue() {
		composite.addResolver(ContinuationHandlerMethodArgumentResolver())
		val value = getInvocable(SuspendingValueClassHandler::valueClassReturnValue.javaMethod!!).invokeForRequest(request, null)
		StepVerifier.create(value as Mono<String>).expectNext("foo").verifyComplete()
	}

	@Test
	fun suspendingResultOfUnitReturnValue() {
		composite.addResolver(ContinuationHandlerMethodArgumentResolver())
		val value = getInvocable(SuspendingValueClassHandler::resultOfUnitReturnValue.javaMethod!!).invokeForRequest(request, null)
		StepVerifier.create(value as Mono<Unit>).verifyComplete()
	}

	@Test
	fun suspendingValueClassDefaultValue() {
		composite.addResolver(ContinuationHandlerMethodArgumentResolver())
		composite.addResolver(StubArgumentResolver(Double::class.java))
		val value = getInvocable(SuspendingValueClassHandler::doubleValueClass.javaMethod!!).invokeForRequest(request, null)
		StepVerifier.create(value as Mono<Double>).expectNext(3.1).verifyComplete()
	}

	@Test
	fun suspendingValueClassWithInit() {
		composite.addResolver(ContinuationHandlerMethodArgumentResolver())
		composite.addResolver(StubArgumentResolver(String::class.java, ""))
		val value = getInvocable(SuspendingValueClassHandler::valueClassWithInit.javaMethod!!).invokeForRequest(request, null)
		StepVerifier.create(value as Mono<String>).verifyError(IllegalArgumentException::class.java)
	}

	@Test
	fun suspendingValueClassWithNullable() {
		composite.addResolver(ContinuationHandlerMethodArgumentResolver())
		composite.addResolver(StubArgumentResolver(LongValueClass::class.java, null))
		val value = getInvocable(SuspendingValueClassHandler::valueClassWithNullable.javaMethod!!).invokeForRequest(request, null)
		StepVerifier.create(value as Mono<Long>).verifyComplete()
	}

	@Test
	fun suspendingValueClassWithPrivateConstructor() {
		composite.addResolver(ContinuationHandlerMethodArgumentResolver())
		composite.addResolver(StubArgumentResolver(Char::class.java, 'a'))
		val value = getInvocable(SuspendingValueClassHandler::valueClassWithPrivateConstructor.javaMethod!!).invokeForRequest(request, null)
		StepVerifier.create(value as Mono<Char>).expectNext('a').verifyComplete()
	}

	@Test
	fun propertyAccessor() {
		val value = getInvocable(PropertyAccessorHandler::prop.javaGetter!!).invokeForRequest(request, null)
		Assertions.assertThat(value).isEqualTo("foo")
	}

	@Test
	fun extension() {
		composite.addResolver(StubArgumentResolver(CustomException::class.java, CustomException("foo")))
		val value = getInvocable(ReflectionUtils.findMethod(ExtensionHandler::class.java, "handle", CustomException::class.java)!!).invokeForRequest(request, null)
		Assertions.assertThat(value).isEqualTo("foo")
	}

	@Test
	fun extensionWithParameter() {
		composite.addResolver(StubArgumentResolver(CustomException::class.java, CustomException("foo")))
		composite.addResolver(StubArgumentResolver(Int::class.java, 20))
		val value = getInvocable(ReflectionUtils.findMethod(ExtensionHandler::class.java, "handleWithParameter", CustomException::class.java, Int::class.java)!!)
			.invokeForRequest(request, null)
		Assertions.assertThat(value).isEqualTo("foo-20")
	}

	@Test
	fun genericParameter() {
		val horse = Animal("horse")
		composite.addResolver(StubArgumentResolver(Animal::class.java, horse))
		val value = getInvocable(AnimalHandler::handle.javaMethod!!, AnimalHandler::class.java).invokeForRequest(request, null)
		Assertions.assertThat(value).isEqualTo(horse.name)
	}

	private fun getInvocable(method: Method, clazz: Class<*> = method.declaringClass): InvocableHandlerMethod {
		val handlerMethod = InvocableHandlerMethod(clazz.constructors.first().newInstance(), method)
		handlerMethod.setHandlerMethodArgumentResolvers(composite)
		return handlerMethod
	}

	private fun getStubResolver(index: Int): StubArgumentResolver {
		return composite.resolvers[index] as StubArgumentResolver
	}

	private class Handler {

		fun intDefaultValue(limit: Int = 20) =
			limit.toString()

		fun nullableIntDefaultValue(limit: Int? = 20) =
			limit.toString()

		fun booleanDefaultValue(status: Boolean = true) =
			status.toString()

		fun nullableBooleanDefaultValue(status: Boolean? = true) =
			status.toString()

		fun unit(): Unit {
		}

		@Suppress("UNUSED_PARAMETER")
		fun nullable(arg: String?): String? {
			return null
		}

		private fun private(value: Float) = value.toString()

	}

	private class ValueClassHandler {

		fun valueClassReturnValue() = StringValueClass("foo")

		fun resultOfUnitReturnValue() = Result.success(Unit)

		fun longValueClass(limit: LongValueClass) = limit.value

		fun doubleValueClass(limit: DoubleValueClass = DoubleValueClass(3.1)) = limit.value

		fun valueClassWithInit(valueClass: ValueClassWithInit) = valueClass

		fun valueClassWithNullable(limit: LongValueClass?) = limit?.value

		fun valueClassWithPrivateConstructor(limit: ValueClassWithPrivateConstructor) = limit.value
	}

	private class SuspendingValueClassHandler {

		suspend fun valueClassReturnValue(): StringValueClass {
			delay(1)
			return StringValueClass("foo")
		}

		suspend fun resultOfUnitReturnValue(): Result<Unit> {
			delay(1)
			return Result.success(Unit)
		}

		suspend fun longValueClass(limit: LongValueClass): Long {
			delay(1)
			return limit.value
		}


		suspend fun doubleValueClass(limit: DoubleValueClass = DoubleValueClass(3.1)): Double {
			delay(1)
			return limit.value
		}

		suspend fun valueClassWithInit(valueClass: ValueClassWithInit): ValueClassWithInit {
			delay(1)
			return valueClass
		}

		suspend fun valueClassWithNullable(limit: LongValueClass?): Long? {
			delay(1)
			return limit?.value
		}

		suspend fun valueClassWithPrivateConstructor(limit: ValueClassWithPrivateConstructor): Char {
			delay(1)
			return limit.value
		}
	}

	private class PropertyAccessorHandler {

		val prop: String
			get() = "foo"
	}

	private class ExtensionHandler {

		fun CustomException.handle(): String {
			return "${this.message}"
		}

		fun CustomException.handleWithParameter(limit: Int): String {
			return "${this.message}-$limit"
		}
	}

	private abstract class GenericHandler<T : Named> {

		fun handle(named: T) = named.name
	}

	private class AnimalHandler : GenericHandler<Animal>()

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
	value class ValueClassWithPrivateConstructor private constructor(val value: Char) {
		companion object {
			fun from(value: Char) = ValueClassWithPrivateConstructor(value)
		}
	}

	class CustomException(message: String) : Throwable(message)

	// Avoid adding a spring-webmvc dependency
	class ContinuationHandlerMethodArgumentResolver : HandlerMethodArgumentResolver {

		override fun supportsParameter(parameter: MethodParameter) =
			"kotlin.coroutines.Continuation" == parameter.getParameterType().getName()

		override fun resolveArgument(
			parameter: MethodParameter,
			mavContainer: ModelAndViewContainer?,
			webRequest: NativeWebRequest,
			binderFactory: WebDataBinderFactory?
		) = null

	}

}
