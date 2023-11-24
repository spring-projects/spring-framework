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

package org.springframework.web.reactive.result.method.annotation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.core.annotation.SynthesizingMethodParameter
import org.springframework.format.support.DefaultFormattingConversionService
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest
import org.springframework.web.testfixture.server.MockServerWebExchange
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer
import org.springframework.web.reactive.BindingContext
import org.springframework.web.server.ServerWebInputException
import reactor.test.StepVerifier

/**
 * Kotlin test fixture for [RequestParamMethodArgumentResolver].
 *
 * @author Sebastien Deleuze
 */
class RequestParamMethodArgumentResolverKotlinTests {

	lateinit var resolver: RequestParamMethodArgumentResolver
	lateinit var bindingContext: BindingContext

	lateinit var nullableParamRequired: MethodParameter
	lateinit var nullableParamNotRequired: MethodParameter
	lateinit var nonNullableParamRequired: MethodParameter
	lateinit var nonNullableParamNotRequired: MethodParameter

	lateinit var defaultValueBooleanParamRequired: MethodParameter
	lateinit var defaultValueBooleanParamNotRequired: MethodParameter
	lateinit var defaultValueIntParamRequired: MethodParameter
	lateinit var defaultValueIntParamNotRequired: MethodParameter
	lateinit var defaultValueStringParamRequired: MethodParameter
	lateinit var defaultValueStringParamNotRequired: MethodParameter


	@BeforeEach
	fun setup() {
		this.resolver = RequestParamMethodArgumentResolver(null, ReactiveAdapterRegistry.getSharedInstance(), true)
		val initializer = ConfigurableWebBindingInitializer()
		initializer.conversionService = DefaultFormattingConversionService()
		bindingContext = BindingContext(initializer)

		val method = ReflectionUtils.findMethod(javaClass, "handle",
			String::class.java, String::class.java, String::class.java, String::class.java,
			Boolean::class.java, Boolean::class.java, Int::class.java, Int::class.java,
			String::class.java, String::class.java)!!

		nullableParamRequired = SynthesizingMethodParameter(method, 0)
		nullableParamNotRequired = SynthesizingMethodParameter(method, 1)
		nonNullableParamRequired = SynthesizingMethodParameter(method, 2)
		nonNullableParamNotRequired = SynthesizingMethodParameter(method, 3)

		defaultValueBooleanParamRequired = SynthesizingMethodParameter(method, 4)
		defaultValueBooleanParamNotRequired = SynthesizingMethodParameter(method, 5)
		defaultValueIntParamRequired = SynthesizingMethodParameter(method, 6)
		defaultValueIntParamNotRequired = SynthesizingMethodParameter(method, 7)
		defaultValueStringParamRequired = SynthesizingMethodParameter(method, 8)
		defaultValueStringParamNotRequired = SynthesizingMethodParameter(method, 9)
	}

	@Test
	fun resolveNullableRequiredWithParameter() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name=123"))
		var result = resolver.resolveArgument(nullableParamRequired, bindingContext, exchange)
		StepVerifier.create(result).expectNext("123").expectComplete().verify()
	}

	@Test
	fun resolveNullableRequiredWithoutParameter() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		var result = resolver.resolveArgument(nullableParamRequired, bindingContext, exchange)
		StepVerifier.create(result).expectComplete().verify()
	}

	@Test
	fun resolveNullableNotRequiredWithParameter() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name=123"))
		var result = resolver.resolveArgument(nullableParamNotRequired, bindingContext, exchange)
		StepVerifier.create(result).expectNext("123").expectComplete().verify()
	}

	@Test
	fun resolveNullableNotRequiredWithoutParameter() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		var result = resolver.resolveArgument(nullableParamNotRequired, bindingContext, exchange)
		StepVerifier.create(result).expectComplete().verify()
	}

	@Test
	fun resolveNonNullableRequiredWithParameter() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name=123"))
		var result = resolver.resolveArgument(nonNullableParamRequired, bindingContext, exchange)
		StepVerifier.create(result).expectNext("123").expectComplete().verify()
	}

	@Test
	fun resolveNonNullableRequiredWithoutParameter() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		var result = resolver.resolveArgument(nonNullableParamRequired, bindingContext, exchange)
		StepVerifier.create(result).expectError(ServerWebInputException::class.java).verify()
	}

	@Test
	fun resolveNonNullableNotRequiredWithParameter() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name=123"))
		var result = resolver.resolveArgument(nonNullableParamNotRequired, bindingContext, exchange)
		StepVerifier.create(result).expectNext("123").expectComplete().verify()
	}

	@Test
	fun resolveNonNullableNotRequiredWithoutParameter() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		var result = resolver.resolveArgument(nonNullableParamNotRequired, bindingContext, exchange)
		StepVerifier.create(result).expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueRequiredWithBooleanParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?value=false"))
		val result = resolver.resolveArgument(defaultValueBooleanParamRequired, bindingContext, exchange)
		StepVerifier.create(result).expectNext(false).expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueRequiredWithoutBooleanParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		val result = resolver.resolveArgument(defaultValueBooleanParamRequired, bindingContext, exchange)
		StepVerifier.create(result).expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueNotRequiredWithBooleanParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?value=false"))
		val result = resolver.resolveArgument(defaultValueBooleanParamNotRequired, bindingContext, exchange)
		StepVerifier.create(result).expectNext(false).expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueNotRequiredWithoutBooleanParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		val result = resolver.resolveArgument(defaultValueBooleanParamNotRequired, bindingContext, exchange)
		StepVerifier.create(result).expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueRequiredWithIntParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?value=123"))
		val result = resolver.resolveArgument(defaultValueIntParamRequired, bindingContext, exchange)
		StepVerifier.create(result).expectNext(123).expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueRequiredWithoutIntParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		val result = resolver.resolveArgument(defaultValueIntParamRequired, bindingContext, exchange)
		StepVerifier.create(result).expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueNotRequiredWithIntParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?value=123"))
		val result = resolver.resolveArgument(defaultValueIntParamNotRequired, bindingContext, exchange)
		StepVerifier.create(result).expectNext(123).expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueNotRequiredWithoutIntParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		val result = resolver.resolveArgument(defaultValueIntParamNotRequired, bindingContext, exchange)
		StepVerifier.create(result).expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueRequiredWithStringParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?value=123"))
		val result = resolver.resolveArgument(defaultValueStringParamRequired, bindingContext, exchange)
		StepVerifier.create(result).expectNext("123").expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueRequiredWithoutStringParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		val result = resolver.resolveArgument(defaultValueStringParamRequired, bindingContext, exchange)
		StepVerifier.create(result).expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueNotRequiredWithStringParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?value=123"))
		val result = resolver.resolveArgument(defaultValueStringParamNotRequired, bindingContext, exchange)
		StepVerifier.create(result).expectNext("123").expectComplete().verify()
	}

	@Test
	fun resolveDefaultValueNotRequiredWithoutStringParameter() {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		val result = resolver.resolveArgument(defaultValueStringParamNotRequired, bindingContext, exchange)
		StepVerifier.create(result).expectComplete().verify()
	}


	@Suppress("unused_parameter")
	fun handle(
			@RequestParam("name") nullableParamRequired: String?,
			@RequestParam("name", required = false) nullableParamNotRequired: String?,
			@RequestParam("name") nonNullableParamRequired: String,
			@RequestParam("name", required = false) nonNullableParamNotRequired: String,

			@RequestParam("value") withDefaultValueBooleanParamRequired: Boolean = true,
			@RequestParam("value", required = false) withDefaultValueBooleanParamNotRequired: Boolean = true,
			@RequestParam("value") withDefaultValueIntParamRequired: Int = 20,
			@RequestParam("value", required = false) withDefaultValueIntParamNotRequired: Int = 20,
			@RequestParam("value") withDefaultValueStringParamRequired: String = "default",
			@RequestParam("value", required = false) withDefaultValueStringParamNotRequired: String = "default") {
	}

}

