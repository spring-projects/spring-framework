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
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.lang.Nullable
import org.springframework.web.service.annotation.GetExchange

/**
 * Kotlin tests for [HttpServiceMethod].
 *
 * @author Sebastien Deleuze
 */
class KotlinHttpServiceMethodTests {

	private val client = TestHttpClientAdapter()
	private val proxyFactory = HttpServiceProxyFactory.builder(client).build()

	@Test
	fun coroutinesService(): Unit = runBlocking {
		val service = proxyFactory.createClient(CoroutinesService::class.java)

		val stringBody = service.stringBody()
		assertThat(stringBody).isEqualTo("requestToBody")
		verifyClientInvocation("requestToBody", object : ParameterizedTypeReference<String>() {})

		service.listBody()
		verifyClientInvocation("requestToBody", object : ParameterizedTypeReference<MutableList<String>>() {})

		val flowBody = service.flowBody()
		assertThat(flowBody.toList()).containsExactly("request", "To", "Body", "Flux")
		verifyClientInvocation("requestToBodyFlux", object : ParameterizedTypeReference<String>() {})

		val stringEntity = service.stringEntity()
		assertThat(stringEntity).isEqualTo(ResponseEntity.ok<String>("requestToEntity"))
		verifyClientInvocation("requestToEntity", object : ParameterizedTypeReference<String>() {})

		service.listEntity()
		verifyClientInvocation("requestToEntity", object : ParameterizedTypeReference<MutableList<String>>() {})

		val flowEntity = service.flowEntity()
		assertThat(flowEntity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(flowEntity.body!!.toList()).containsExactly("request", "To", "Entity", "Flux")
		verifyClientInvocation("requestToEntityFlux", object : ParameterizedTypeReference<String>() {})
	}

	private fun verifyClientInvocation(methodName: String, @Nullable expectedBodyType: ParameterizedTypeReference<*>) {
		assertThat(client.invokedMethodName).isEqualTo(methodName)
		assertThat(client.bodyType).isEqualTo(expectedBodyType)
	}

	private interface CoroutinesService {

		@GetExchange
		suspend fun stringBody(): String

		@GetExchange
		suspend fun listBody(): MutableList<String>

		@GetExchange
		fun flowBody(): Flow<String>

		@GetExchange
		suspend fun stringEntity(): ResponseEntity<String>

		@GetExchange
		suspend fun listEntity(): ResponseEntity<MutableList<String>>

		@GetExchange
		fun flowEntity(): ResponseEntity<Flow<String>>
	}

}
