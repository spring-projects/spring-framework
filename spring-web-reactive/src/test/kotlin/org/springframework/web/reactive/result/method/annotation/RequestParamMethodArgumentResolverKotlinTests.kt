package org.springframework.web.reactive.result.method.annotation

import org.junit.Before
import org.junit.Test
import org.springframework.core.MethodParameter
import org.springframework.core.annotation.SynthesizingMethodParameter
import org.springframework.format.support.DefaultFormattingConversionService
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer
import org.springframework.web.reactive.BindingContext
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.server.adapter.DefaultServerWebExchange
import reactor.test.StepVerifier

/**
 * Kotlin test fixture for [RequestParamMethodArgumentResolver].
 *
 * @author Sebastien Deleuze
 */
class RequestParamMethodArgumentResolverKotlinTests {

    lateinit var resolver: RequestParamMethodArgumentResolver
    lateinit var request: MockServerHttpRequest
    lateinit var bindingContext: BindingContext

    lateinit var nullableParamRequired: MethodParameter
    lateinit var nullableParamNotRequired: MethodParameter
    lateinit var nonNullableParamRequired: MethodParameter
    lateinit var nonNullableParamNotRequired: MethodParameter


    @Before
    fun setup() {
        this.resolver = RequestParamMethodArgumentResolver(null, true)
        this.request = MockServerHttpRequest.get("/").build()
        val initializer = ConfigurableWebBindingInitializer()
        initializer.conversionService = DefaultFormattingConversionService()
        bindingContext = BindingContext(initializer)

        val method = ReflectionUtils.findMethod(javaClass, "handle", String::class.java,
                String::class.java, String::class.java, String::class.java)

        nullableParamRequired = SynthesizingMethodParameter(method, 0)
        nullableParamNotRequired = SynthesizingMethodParameter(method, 1)
        nonNullableParamRequired = SynthesizingMethodParameter(method, 2)
        nonNullableParamNotRequired = SynthesizingMethodParameter(method, 3)
    }

    @Test
    fun resolveNullableRequiredWithParameter() {
        this.request = MockServerHttpRequest.get("/path?name=123").build()
        var result = resolver.resolveArgument(nullableParamRequired, bindingContext, createExchange())
        StepVerifier.create(result).expectNext("123").expectComplete().verify()
    }

    @Test
    fun resolveNullableRequiredWithoutParameter() {
        var result = resolver.resolveArgument(nullableParamRequired, bindingContext, createExchange())
        StepVerifier.create(result).expectComplete().verify()
    }

    @Test
    fun resolveNullableNotRequiredWithParameter() {
        this.request = MockServerHttpRequest.get("/path?name=123").build()
        var result = resolver.resolveArgument(nullableParamNotRequired, bindingContext, createExchange())
        StepVerifier.create(result).expectNext("123").expectComplete().verify()
    }

    @Test
    fun resolveNullableNotRequiredWithoutParameter() {
        var result = resolver.resolveArgument(nullableParamNotRequired, bindingContext, createExchange())
        StepVerifier.create(result).expectComplete().verify()
    }

    @Test
    fun resolveNonNullableRequiredWithParameter() {
        this.request = MockServerHttpRequest.get("/path?name=123").build()
        var result = resolver.resolveArgument(nonNullableParamRequired, bindingContext, createExchange())
        StepVerifier.create(result).expectNext("123").expectComplete().verify()
    }

    @Test
    fun resolveNonNullableRequiredWithoutParameter() {
        var result = resolver.resolveArgument(nonNullableParamRequired, bindingContext, createExchange())
        StepVerifier.create(result).expectError(ServerWebInputException::class.java).verify()
    }

    @Test
    fun resolveNonNullableNotRequiredWithParameter() {
        this.request = MockServerHttpRequest.get("/path?name=123").build()
        var result = resolver.resolveArgument(nonNullableParamNotRequired, bindingContext, createExchange())
        StepVerifier.create(result).expectNext("123").expectComplete().verify()
    }

    @Test
    fun resolveNonNullableNotRequiredWithoutParameter() {
        var result = resolver.resolveArgument(nonNullableParamNotRequired, bindingContext, createExchange())
        StepVerifier.create(result).expectComplete().verify()
    }

    private fun createExchange() = DefaultServerWebExchange(this.request, MockServerHttpResponse())


    @Suppress("unused_parameter")
    fun handle(
            @RequestParam("name") nullableParamRequired: String?,
            @RequestParam("name", required = false) nullableParamNotRequired: String?,
            @RequestParam("name") nonNullableParamRequired: String,
            @RequestParam("name", required = false) nonNullableParamNotRequired: String) {
    }

}

