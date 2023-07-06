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

package org.springframework.web.service.invoker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.service.annotation.GetExchange

/**
 * Kotlin tests for [HttpServiceMethod].
 *
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 */
@Suppress("DEPRECATION")
class KotlinHttpServiceMethodTests {

	private val webClientAdapter = TestHttpClientAdapter()
    private val httpExchangeAdapter = TestHttpExchangeAdapter()
	private val proxyFactory = HttpServiceProxyFactory.builder(webClientAdapter).build()
    private val blockingProxyFactory = HttpServiceProxyFactory.builder()
            .exchangeAdapter(httpExchangeAdapter).build()

	@Test
	fun coroutinesService(): Unit = runBlocking {
		val service = proxyFactory.createClient(FunctionsService::class.java)

		val stringBody = service.stringBody()
		assertThat(stringBody).isEqualTo("body")
		verifyClientInvocation("body", object : ParameterizedTypeReference<String>() {})

		service.listBody()
		verifyClientInvocation("body", object : ParameterizedTypeReference<MutableList<String>>() {})

		val flowBody = service.flowBody()
		assertThat(flowBody.toList()).containsExactly("request", "To", "Body", "Flux")
		verifyClientInvocation("bodyFlux", object : ParameterizedTypeReference<String>() {})

		val stringEntity = service.stringEntity()
		assertThat(stringEntity).isEqualTo(ResponseEntity.ok<String>("entity"))
		verifyClientInvocation("entity", object : ParameterizedTypeReference<String>() {})

		service.listEntity()
		verifyClientInvocation("entity", object : ParameterizedTypeReference<MutableList<String>>() {})

		val flowEntity = service.flowEntity()
		assertThat(flowEntity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(flowEntity.body!!.toList()).containsExactly("request", "To", "Entity", "Flux")
		verifyClientInvocation("entityFlux", object : ParameterizedTypeReference<String>() {})
	}

    @Test
    fun blockingServiceWithExchangeResponseFunction() {
        val service = blockingProxyFactory.createClient(BlockingFunctionsService::class.java)

        val stringBody = service.stringBodyBlocking()
        assertThat(stringBody).isEqualTo("body")
        verifyTemplateInvocation("body", object : ParameterizedTypeReference<String>() {})

        val listBody = service.listBodyBlocking()
        assertThat(listBody.size).isEqualTo(1)
        verifyTemplateInvocation("body", object : ParameterizedTypeReference<MutableList<String>>() {})

        val stringEntity = service.stringEntityBlocking()
        assertThat(stringEntity).isEqualTo(ResponseEntity.ok<String>("entity"))
        verifyTemplateInvocation("entity", object : ParameterizedTypeReference<String>() {})

        service.listEntityBlocking()
        verifyTemplateInvocation("entity", object : ParameterizedTypeReference<MutableList<String>>() {})
    }

    @Test
    fun coroutineServiceWithExchangeResponseFunction() {
        assertThatIllegalStateException().isThrownBy {
            blockingProxyFactory.createClient(FunctionsService::class.java)
        }

        assertThatIllegalStateException().isThrownBy {
            blockingProxyFactory.createClient(SuspendingFunctionsService::class.java)
        }
    }

    private fun verifyTemplateInvocation(methodReference: String, expectedBodyType: ParameterizedTypeReference<*>) {
        assertThat(httpExchangeAdapter.invokedMethodReference).isEqualTo(methodReference)
        assertThat(httpExchangeAdapter.bodyType).isEqualTo(expectedBodyType)
    }

    private fun verifyClientInvocation(methodReference: String, expectedBodyType: ParameterizedTypeReference<*>) {
		assertThat(webClientAdapter.invokedMethodReference).isEqualTo(methodReference)
		assertThat(webClientAdapter.bodyType).isEqualTo(expectedBodyType)
	}

	private interface FunctionsService : SuspendingFunctionsService {

		@GetExchange
		fun flowBody(): Flow<String>

		@GetExchange
		fun flowEntity(): ResponseEntity<Flow<String>>
	}

    private interface SuspendingFunctionsService : BlockingFunctionsService {

        @GetExchange
        suspend fun stringBody(): String

        @GetExchange
        suspend fun listBody(): MutableList<String>

        @GetExchange
        suspend fun stringEntity(): ResponseEntity<String>

        @GetExchange
        suspend fun listEntity(): ResponseEntity<MutableList<String>>
    }

    private interface BlockingFunctionsService {

        @GetExchange
        fun stringBodyBlocking(): String

        @GetExchange
        fun listBodyBlocking(): MutableList<String>

        @GetExchange
        fun stringEntityBlocking(): ResponseEntity<String>

        @GetExchange
        fun listEntityBlocking(): ResponseEntity<MutableList<String>>

    }

}
