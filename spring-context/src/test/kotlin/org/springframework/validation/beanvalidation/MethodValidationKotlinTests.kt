/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	suspend fun coroutinesParameterValidation() {
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
