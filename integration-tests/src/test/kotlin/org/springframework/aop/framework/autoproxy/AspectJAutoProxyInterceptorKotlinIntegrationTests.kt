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

package org.springframework.aop.framework.autoproxy

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.autoproxy.AspectJAutoProxyInterceptorKotlinIntegrationTests.InterceptorConfig
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.testfixture.ReactiveCallCountingTransactionManager
import reactor.core.publisher.Mono
import java.lang.reflect.Method
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.TYPE


/**
 *  Integration tests for interceptors with Kotlin (with and without Coroutines) configured
 *  via AspectJ auto-proxy support.
 */
@SpringJUnitConfig(InterceptorConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AspectJAutoProxyInterceptorKotlinIntegrationTests(
    @Autowired val echo: Echo,
    @Autowired val firstAdvisor: TestPointcutAdvisor,
    @Autowired val secondAdvisor: TestPointcutAdvisor,
    @Autowired val countingAspect: CountingAspect,
    @Autowired val reactiveTransactionManager: ReactiveCallCountingTransactionManager) {

    @Test
    fun `Multiple interceptors with regular function`() {
        assertThat(firstAdvisor.interceptor.invocations).isEmpty()
        assertThat(secondAdvisor.interceptor.invocations).isEmpty()
        val value = "Hello!"
        assertThat(echo.echo(value)).isEqualTo(value)
		assertThat(firstAdvisor.interceptor.invocations).singleElement().matches { String::class.java.isAssignableFrom(it) }
		assertThat(secondAdvisor.interceptor.invocations).singleElement().matches { String::class.java.isAssignableFrom(it) }
    }

    @Test
    fun `Multiple interceptors with suspending function`() {
        assertThat(firstAdvisor.interceptor.invocations).isEmpty()
        assertThat(secondAdvisor.interceptor.invocations).isEmpty()
        val value = "Hello!"
        runBlocking {
            assertThat(echo.suspendingEcho(value)).isEqualTo(value)
        }
		assertThat(firstAdvisor.interceptor.invocations).singleElement().matches { Mono::class.java.isAssignableFrom(it) }
		assertThat(secondAdvisor.interceptor.invocations).singleElement().matches { Mono::class.java.isAssignableFrom(it) }
    }

	@Test // gh-33095
	fun `Aspect and reactive transactional with suspending function`() {
		assertThat(countingAspect.counter).isZero()
		assertThat(reactiveTransactionManager.commits).isZero()
		val value = "Hello!"
		runBlocking {
			assertThat(echo.suspendingTransactionalEcho(value)).isEqualTo(value)
		}
		assertThat(countingAspect.counter).`as`("aspect applied").isOne()
		assertThat(reactiveTransactionManager.begun).isOne()
		assertThat(reactiveTransactionManager.commits).`as`("transactional applied").isOne()
	}

	@Test // gh-33210
	fun `Aspect and cacheable with suspending function`() {
		assertThat(countingAspect.counter).isZero()
		val value = "Hello!"
		runBlocking {
			assertThat(echo.suspendingCacheableEcho(value)).isEqualTo("$value 0")
			assertThat(echo.suspendingCacheableEcho(value)).isEqualTo("$value 0")
			assertThat(echo.suspendingCacheableEcho(value)).isEqualTo("$value 0")
			assertThat(countingAspect.counter).`as`("aspect applied once").isOne()

			assertThat(echo.suspendingCacheableEcho("$value bis")).isEqualTo("$value bis 1")
			assertThat(echo.suspendingCacheableEcho("$value bis")).isEqualTo("$value bis 1")
		}
		assertThat(countingAspect.counter).`as`("aspect applied once per key").isEqualTo(2)
	}

    @Configuration
    @EnableAspectJAutoProxy
    @EnableTransactionManagement
	@EnableCaching
    open class InterceptorConfig {

        @Bean
        open fun firstAdvisor() = TestPointcutAdvisor().apply { order = 0 }

        @Bean
        open fun secondAdvisor() = TestPointcutAdvisor().apply { order = 1 }

		@Bean
		open fun countingAspect() = CountingAspect()

		@Bean
		open fun transactionManager(): ReactiveCallCountingTransactionManager {
			return ReactiveCallCountingTransactionManager()
		}

		@Bean
		open fun cacheManager(): CacheManager {
			return ConcurrentMapCacheManager()
		}

        @Bean
        open fun echo(): Echo {
            return Echo()
        }
    }

    class TestMethodInterceptor: MethodInterceptor {

        var invocations: MutableList<Class<*>> = mutableListOf()

        @Suppress("RedundantNullableReturnType")
        override fun invoke(invocation: MethodInvocation): Any? {
            val result = invocation.proceed()
            invocations.add(result!!.javaClass)
            return result
        }

    }

    class TestPointcutAdvisor : StaticMethodMatcherPointcutAdvisor(TestMethodInterceptor()) {

        val interceptor: TestMethodInterceptor
            get() = advice as TestMethodInterceptor

        override fun matches(method: Method, targetClass: Class<*>): Boolean {
            return targetClass == Echo::class.java && method.name.lowercase().endsWith("echo")
        }
    }

	@Target(CLASS, FUNCTION, ANNOTATION_CLASS, TYPE)
	@Retention(AnnotationRetention.RUNTIME)
	annotation class Counting()

	@Aspect
	@Component
	class CountingAspect {

		var counter: Long = 0

		@Around("@annotation(org.springframework.aop.framework.autoproxy.AspectJAutoProxyInterceptorKotlinIntegrationTests.Counting)")
		fun logging(joinPoint: ProceedingJoinPoint): Any {
			return (joinPoint.proceed(joinPoint.args) as Mono<*>).doOnTerminate {
				counter++
			}.checkpoint("CountingAspect")
		}
	}

    open class Echo {

        open fun echo(value: String): String {
            return value
        }

        open suspend fun suspendingEcho(value: String): String {
            delay(1)
            return value
        }

		@Transactional
		@Counting
		open suspend fun suspendingTransactionalEcho(value: String): String {
			delay(1)
			return value
		}

		open var cacheCounter: Int = 0

		@Counting
		@Cacheable("something")
		open suspend fun suspendingCacheableEcho(value: String): String {
			delay(1)
			return "$value ${cacheCounter++}"
		}

    }

}
