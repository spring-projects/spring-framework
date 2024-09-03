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
import org.springframework.http.MediaType
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer
import org.springframework.web.reactive.BindingContext
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest
import org.springframework.web.testfixture.method.ResolvableMethod
import org.springframework.web.testfixture.server.MockServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

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
	fun bindDataClassError() {
		val parameter: MethodParameter = this.testMethod.annotNotPresent(ModelAttribute::class.java).arg(DataClass::class.java)
		val mono: Mono<Any> =
			createResolver().resolveArgument(parameter, this.bindContext, postForm("name=Robert&age=invalid&count=1"))
		StepVerifier.create(mono)
			.expectNextCount(0)
			.expectError(ServerWebInputException::class.java)
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
