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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.http.MediaType
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.reactive.BindingContext
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest
import org.springframework.web.testfixture.method.ResolvableMethod
import org.springframework.web.testfixture.server.MockServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.function.Function

/**
 * Kotlin test fixture for [ModelAttributeMethodArgumentResolver].
 *
 * @author Sebastien Deleuze
 */
class ModelAttributeMethodArgumentResolverKotlinTests {

	private val testMethod = ResolvableMethod.on(javaClass).named("handle").build()

	private lateinit var bindContext: BindingContext

	@BeforeEach
	fun setup() {
		val validator = LocalValidatorFactoryBean()
		validator.afterPropertiesSet()
		val initializer = ConfigurableWebBindingInitializer()
		initializer.validator = validator
		this.bindContext = BindingContext(initializer)
	}

	@Test
	fun validationErrorForDataClass() {
		val parameter = this.testMethod.annotNotPresent(ModelAttribute::class.java).arg(DataClass::class.java)
		testValidationError(parameter, Function.identity())
	}

	private fun testValidationError(parameter: MethodParameter, valueMonoExtractor: Function<Mono<*>, Mono<*>>) {
		testValidationError(parameter, valueMonoExtractor, "age=invalid", "age", "invalid")
	}

	private fun testValidationError(param: MethodParameter, valueMonoExtractor: Function<Mono<*>, Mono<*>>,
									formData: String, field: String, rejectedValue: String) {
		var mono: Mono<*> = createResolver().resolveArgument(param, this.bindContext, postForm(formData))
		mono = valueMonoExtractor.apply(mono)
		StepVerifier.create(mono)
			.consumeErrorWith { ex: Throwable ->
				Assertions.assertThat(ex).isInstanceOf(WebExchangeBindException::class.java)
				val bindException = ex as WebExchangeBindException
				Assertions.assertThat(bindException.errorCount).isEqualTo(1)
				Assertions.assertThat(bindException.hasFieldErrors(field)).isTrue()
				Assertions.assertThat(bindException.getFieldError(field)!!.rejectedValue)
					.isEqualTo(rejectedValue)
			}
			.verify()
	}

	private fun createResolver(): ModelAttributeMethodArgumentResolver {
		return ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance(), false)
	}

	private fun postForm(formData: String): ServerWebExchange {
		return MockServerWebExchange.from(
			MockServerHttpRequest.post("/")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData)
		)
	}

	@Suppress("UNUSED_PARAMETER")
	private fun handle(dataClassNotAnnotated: DataClass) {
	}

	private class DataClass(val name: String, val age: Int, val count: Int)

}
