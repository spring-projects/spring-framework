package org.springframework.aop.framework.autoproxy

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.autoproxy.AspectJAutoProxyInterceptorKotlinIntegrationTests.InterceptorConfig
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Mono
import java.lang.reflect.Method


/**
 *  Integration tests for interceptors with Kotlin (with and without Coroutines) configured
 *  via AspectJ auto-proxy support.
 */
@SpringJUnitConfig(InterceptorConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AspectJAutoProxyInterceptorKotlinIntegrationTests(
    @Autowired val echo: Echo,
    @Autowired val firstAdvisor: TestPointcutAdvisor,
    @Autowired val secondAdvisor: TestPointcutAdvisor) {

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

    @Configuration
    @EnableAspectJAutoProxy
    open class InterceptorConfig {

        @Bean
        open fun firstAdvisor() = TestPointcutAdvisor().apply { order = 0 }

        @Bean
        open fun secondAdvisor() = TestPointcutAdvisor().apply { order = 1 }


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

    open class Echo {

        open fun echo(value: String): String {
            return value;
        }

        open suspend fun suspendingEcho(value: String): String {
            delay(1)
            return value;
        }

    }

}
