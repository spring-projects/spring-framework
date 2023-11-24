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

package org.springframework.validation.beanvalidation

import jakarta.validation.ValidationException
import jakarta.validation.Validator
import jakarta.validation.constraints.NotEmpty
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.ProxyFactory
import org.springframework.validation.annotation.Validated

/**
 * Kotlin tests for [MethodValidationInterceptor] + [LocalValidatorFactoryBean].
 *
 * @author Sebastien Deleuze
 */
@Suppress("UsePropertyAccessSyntax")
class MethodValidationKotlinTests {

	@Test
	fun parameterValidation() {
		val bean = MyValidBean()
		val proxyFactory = ProxyFactory(bean)
		val validator = LocalValidatorFactoryBean()
		validator.afterPropertiesSet()
		proxyFactory.addAdvice(MethodValidationInterceptor(validator as Validator))
		val proxy = proxyFactory.getProxy() as MyValidBean
		assertThat(proxy.validName("name")).isEqualTo("name")
		assertThatExceptionOfType(ValidationException::class.java).isThrownBy {
			proxy.validName("")
		}
	}

	@Test
	fun coroutinesParameterValidation() = runBlocking<Unit> {
		val bean = MyValidCoroutinesBean()
		val proxyFactory = ProxyFactory(bean)
		val validator = LocalValidatorFactoryBean()
		validator.afterPropertiesSet()
		proxyFactory.addAdvice(MethodValidationInterceptor(validator as Validator))
		val proxy = proxyFactory.getProxy() as MyValidCoroutinesBean
		assertThat(proxy.validName("name")).isEqualTo("name")
		assertThatExceptionOfType(ValidationException::class.java).isThrownBy {
			runBlocking {
				proxy.validName("")
			}
		}
	}

	@Validated
	open class MyValidBean {

		@Suppress("UNUSED_PARAMETER")
		open fun validName(@NotEmpty name: String) = name
	}

	@Validated
	open class MyValidCoroutinesBean {

		@Suppress("UNUSED_PARAMETER")
		open suspend fun validName(@NotEmpty name: String) = name
	}

}
